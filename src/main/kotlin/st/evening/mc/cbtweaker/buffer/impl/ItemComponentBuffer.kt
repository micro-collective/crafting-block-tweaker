package st.evening.mc.cbtweaker.buffer.impl

import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.SlotItemHandler
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.BufferObserver
import st.evening.mc.cbtweaker.buffer.VirtualBufferType
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.gui.inventory.UiContainer
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.util.gui.DrawableData
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.cbtweaker.util.machine.MutableComponentSet
import st.evening.mc.cbtweaker.util.recipe.IngredientLoader
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectStringValue
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.gui.engine.prefab.InventorySlot
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.copyWithSize
import st.evening.mc.prelude.api.util.game.isEqual
import st.evening.mc.prelude.api.util.game.stacksWith
import st.evening.mc.prelude.api.util.math.IntRectangle
import java.util.function.Predicate
import kotlin.math.min

class ItemComponentBuffer(
    private val config: Config,
    val world: World,
    val bufPos: BlockPos,
    private var storedStack: ItemStack,
    private val observer: BufferObserver?
) : IItemHandlerModifiable, NbtCompoundSerializable {
    companion object {
        private fun checkSlotIndex(slotIndex: Int) {
            if (slotIndex != 0) {
                throw IndexOutOfBoundsException("Component buffer has only one slot, so $slotIndex is out of bounds!")
            }
        }
    }

    override fun getSlots(): Int = 1

    override fun getStackInSlot(slot: Int): ItemStack {
        checkSlotIndex(slot)
        return storedStack
    }

    private fun setAndNotify(stack: ItemStack) {
        storedStack = stack
        observer?.onComponentsChanged()
    }

    override fun setStackInSlot(slot: Int, stack: ItemStack) {
        checkSlotIndex(slot)
        if (storedStack.isEqual(stack)) return
        setAndNotify(stack)
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        checkSlotIndex(slot)
        if (stack.isEmpty) return ItemStack.EMPTY
        config.itemFilter?.let {
            if (!it.test(stack)) return stack
        }
        val stored = storedStack
        if (!stored.isEmpty && !stored.stacksWith(stack)) return stack
        val stackCount = stack.count
        val storedCount = stored.count
        val toTransfer = min(stackCount, min(config.maxStackSize, stack.maxStackSize) - storedCount)
        if (toTransfer <= 0) return stack
        if (!simulate) {
            setAndNotify(stack.copyWithSize(storedCount + toTransfer))
        }
        return stack.copyWithSize(stackCount - toTransfer)
    }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        checkSlotIndex(slot)
        if (amount <= 0) return ItemStack.EMPTY
        val stored = storedStack
        if (stored.isEmpty) return ItemStack.EMPTY
        val storedCount = stored.count
        val toTransfer = amount.coerceAtMost(storedCount)
        if (!simulate) {
            setAndNotify(stored.copyWithSize(storedCount - toTransfer))
        }
        return stored.copyWithSize(toTransfer)
    }

    override fun getSlotLimit(slot: Int): Int = config.maxStackSize

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean = config.itemFilter?.test(stack) != false

    fun createUiElement(): UiElement = UiElementImpl()

    override fun writeToNbt(dto: NBTTagCompound) {
        if (!storedStack.isEmpty) {
            storedStack.writeToNBT(dto)
        }
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        storedStack = if (dto.isEmpty) ItemStack.EMPTY else ItemStack(dto)
    }

    private inner class UiElementImpl : UiElement {
        override fun addToContainer(uiIndex: Int, container: UiContainer, region: IntRectangle) {
            container.addSlot(SlotItemHandler(this@ItemComponentBuffer, 0, 0, 0))
        }

        @ClientSide.Strong
        override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
            config.uiPosition.placeElement(
                uiIndex, layout, wrapper,
                InventorySlot(baseSlotIndex, config.slotBg.drawable)
            )
        }
    }

    class Config(
        val maxStackSize: Int,
        val itemFilter: Predicate<ItemStack>?,
        val componentId: String,
        val uiPosition: UiPosition,
        val slotBg: DrawableData
    )

    object Type : VirtualBufferType<ItemComponentBuffer>() {
        override val id: ResourceLocation = CbTweaker.resource("item_component")

        override val bufferClass: Class<ItemComponentBuffer>
            get() = ItemComponentBuffer::class.java

        context(_: JsonPath)
        override fun loadVirtualBufferFactory(dto: TJson.Object): VirtualBufferFactory<ItemComponentBuffer> {
            val config = Config(
                dto.expectInt("stack_size") ?: 64,
                dto.useAny("item_filter") { IngredientLoader.loadItemFilter(it) },
                dto.expectStringValue("component_id"),
                dto.useAny("ui_position") { UiPosition.load(it) } ?: UiPosition.CENTER,
                dto.useAny("slot_bg") { DrawableData.loadSliceOrBlank(it, 16, 16) } ?: CbtGuiData.ITEM_SLOT
            )
            return { world, pos, observer ->
                ItemComponentBuffer(config, world, pos, ItemStack.EMPTY, observer)
            }
        }

        override fun collectComponents(components: MutableComponentSet, buffer: ItemComponentBuffer) {
            components.put(buffer.config.componentId, buffer.storedStack.count)
        }

        @ServerSide
        override fun handleDestruction(state: ItemComponentBuffer, blockState: IBlockState) {
            val stack = state.storedStack
            if (!stack.isEmpty) {
                InvHelper.dropItem(stack, state.world, state.bufPos)
            }
        }

        override fun serializeBufferToNbt(buffer: ItemComponentBuffer, dto: NBTTagCompound) {
            buffer.writeToNbt(dto)
        }

        override fun deserializeBufferFromNbt(buffer: ItemComponentBuffer, dto: NBTTagCompound) {
            buffer.readFromNbt(dto)
        }

        override fun createUiElement(buffer: ItemComponentBuffer): UiElement = buffer.createUiElement()
    }
}
