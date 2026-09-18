package st.evening.mc.cbtweaker.buffer.impl

import mezz.jei.api.IJeiHelpers
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.items.SlotItemHandler
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.AutoExportingBufferType
import st.evening.mc.cbtweaker.buffer.BufferFactory
import st.evening.mc.cbtweaker.buffer.BufferObserver
import st.evening.mc.cbtweaker.buffer.SidedBufferType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcher
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcherType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientProvider
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientProviderType
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.compat.jei.ingredient.impl.JeiItemIngredient
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.gui.inventory.UiContainer
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.util.CbtMathHelper
import st.evening.mc.cbtweaker.util.capability.RestrictedIoItemHandler
import st.evening.mc.cbtweaker.util.gui.DrawableData
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.cbtweaker.util.machine.ItemConsumeType
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.cbtweaker.util.recipe.IngredientLoader
import st.evening.mc.cbtweaker.util.recipe.ItemSpecifier
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectFloat
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectStringValue
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.addChild
import st.evening.mc.prelude.api.gui.engine.prefab.AbsoluteLayout
import st.evening.mc.prelude.api.gui.engine.prefab.InventorySlot
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.gui.engine.runAction
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.data.plusAssign
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.CapabilityVisitor
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ItemKey
import st.evening.mc.prelude.api.util.game.OreEntry
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.copyWithSize
import st.evening.mc.prelude.api.util.game.isEqual
import st.evening.mc.prelude.api.util.game.stacksWith
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.math.Vec2i
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace
import java.util.LinkedList
import java.util.function.Predicate
import kotlin.math.min

class ItemStackBuffer private constructor(
    private val config: Config,
    val world: World,
    val bufPos: BlockPos,
    private val inventory: Array<ItemStack>,
    private val observer: BufferObserver?
) : IItemHandlerModifiable, NbtCompoundSerializable {
    companion object {
        private const val SER_ITEMS: String = "items"
    }

    val restrictedInventory: IItemHandler = RestrictedIoItemHandler(this, config.allowInsert, config.allowExtract)

    constructor(config: Config, world: World, pos: BlockPos, observer: BufferObserver?) :
        this(config, world, pos, Array(config.slotCount) { ItemStack.EMPTY }, observer)

    override fun getSlots(): Int = inventory.size

    override fun getStackInSlot(slot: Int): ItemStack = inventory[slot]

    private fun setAndNotify(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        observer?.onIngredientsChanged()
    }

    override fun setStackInSlot(slot: Int, stack: ItemStack) {
        if (inventory[slot].isEqual(stack)) return
        setAndNotify(slot, stack)
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        if (stack.isEmpty) return ItemStack.EMPTY
        config.itemFilter?.let {
            if (!it.test(stack)) return stack
        }
        val slotStack = inventory[slot]
        if (!slotStack.isEmpty && !slotStack.stacksWith(stack)) return stack
        val stackCount = stack.count
        val slotCount = slotStack.count
        val toTransfer = min(stackCount, min(config.maxStackSize, stack.maxStackSize) - slotCount)
        if (toTransfer <= 0) return stack
        if (!simulate) {
            setAndNotify(slot, stack.copyWithSize(slotCount + toTransfer))
        }
        return stack.copyWithSize(stackCount - toTransfer)
    }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        if (amount <= 0) return ItemStack.EMPTY
        val slotStack = inventory[slot]
        if (slotStack.isEmpty) return ItemStack.EMPTY
        val slotCount = slotStack.count
        val toTransfer = amount.coerceAtMost(slotCount)
        if (!simulate) {
            setAndNotify(slot, slotStack.copyWithSize(slotCount - toTransfer))
        }
        return slotStack.copyWithSize(toTransfer)
    }

    override fun getSlotLimit(slot: Int): Int = config.maxStackSize

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean = config.itemFilter?.test(stack) != false

    fun copy(observer: BufferObserver?): ItemStackBuffer =
        ItemStackBuffer(config, world, bufPos, inventory.clone(), observer)

    fun createUiElement(): UiElement {
        val slotPosList = arrayOfNulls<Vec2i>(config.slotCount)
        val dims = CbtMathHelper.layOutSlotGroup(18, 18, slotPosList)
        @Suppress("UNCHECKED_CAST")
        return UiElementImpl(dims.x, dims.y, slotPosList as Array<Vec2i>)
    }

    override fun writeToNbt(dto: NBTTagCompound) {
        dto.runAction {
            SER_ITEMS tag NBTTagList().also { itemsDto ->
                inventory.forEach {
                    itemsDto += if (it.isEmpty) NBTTagCompound() else it.serializeNBT()
                }
            }
        }
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        val itemsTag: NBTTagList = dto.getTagList(SER_ITEMS, Constants.NBT.TAG_COMPOUND)
        val limit = min(inventory.size, itemsTag.tagCount())
        var i = 0
        while (i < limit) {
            val stackTag = itemsTag.getCompoundTagAt(i)
            inventory[i++] = if (stackTag.isEmpty) ItemStack.EMPTY else ItemStack(stackTag)
        }
        while (i < inventory.size) {
            inventory[i++] = ItemStack.EMPTY
        }
    }

    private inner class UiElementImpl(
        private val width: Int,
        private val height: Int,
        private val slotPosList: Array<Vec2i>
    ) : UiElement {
        override fun addToContainer(uiIndex: Int, container: UiContainer, region: IntRectangle) {
            for (i in 0..<config.slotCount) {
                container.addSlot(SlotItemHandler(restrictedInventory, i, 0, 0))
            }
        }

        @ClientSide.Strong
        override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
            config.uiPosition.placeElement(uiIndex, layout, wrapper, AbsoluteLayout(width, height).runAction {
                slotPosList.forEachIndexed { i, slotPos ->
                    addChild(slotPos.x, slotPos.y, InventorySlot(baseSlotIndex + i, config.slotBg.drawable))
                }
            })
        }
    }

    class Config(
        val slotCount: Int,
        val maxStackSize: Int,
        val itemFilter: Predicate<ItemStack>?,
        val allowInsert: Boolean,
        val allowExtract: Boolean,
        val allowAutoExport: Boolean,
        val uiPosition: UiPosition,
        val slotBg: DrawableData
    )

    class Accumulator {
        private val buffers: MutableList<ItemStackBuffer> = mutableListOf()
        private val wildcard: MutableMap<ItemSpecifier.Wildcard, MultiStore> = mutableMapOf()
        private val byMeta: MutableMap<ItemSpecifier.ByMeta, MultiStore> = mutableMapOf()
        private val emptySlots: MultiStore = MultiStore(null)

        fun accumulate(buffer: ItemStackBuffer) {
            buffers += buffer
            for (i in 0..<buffer.slots) {
                val stack = buffer.getStackInSlot(i)
                if (stack.isEmpty) {
                    emptySlots.addSlot(buffer, i)
                } else {
                    wildcard.put(ItemSpecifier.Wildcard.fromStack(stack), buffer, i)
                    byMeta.put(ItemSpecifier.ByMeta.fromStack(stack), buffer, i)
                }
            }
        }

        private fun <K : ItemSpecifier> MutableMap<K, MultiStore>.put(key: K, buffer: ItemStackBuffer, slotIndex: Int) {
            getOrPut(key) { MultiStore(key) }.addSlot(buffer, slotIndex)
        }

        fun getStore(key: ItemSpecifier): MultiStore? = when (key) {
            is ItemSpecifier.Wildcard -> wildcard[key]
            is ItemSpecifier.ByMeta -> byMeta[key]
        }

        fun getEmptyStore(): MultiStore = emptySlots

        @PreludeInternal
        fun getAllStores(): Collection<MultiStore> = wildcard.values

        fun insert(stack: ItemStack, simulate: Boolean): ItemStack {
            if (stack.isEmpty) return ItemStack.EMPTY
            var rem = stack
            byMeta[ItemSpecifier.ByMeta.fromStack(stack)]?.let {
                rem = it.insert(rem, simulate)
                if (rem.isEmpty) return ItemStack.EMPTY
            }
            wildcard[ItemSpecifier.Wildcard.fromStack(rem)]?.let {
                rem = it.insert(rem, simulate)
                if (rem.isEmpty) return ItemStack.EMPTY
            }
            return emptySlots.insert(rem, simulate)
        }

        fun dropItem(stack: ItemStack) {
            if (buffers.isNotEmpty()) {
                val buf = buffers[0]
                InvHelper.dropItem(stack, buf.world, buf.bufPos)
            }
        }

        fun insertOrDrop(stack: ItemStack, checkMode: Boolean) {
            val rem = this@Accumulator.insert(stack, false)
            if (!checkMode && !rem.isEmpty) {
                dropItem(rem)
            }
        }

        @OptIn(PreludeInternal::class)
        inline fun forEach(action: (MultiStore) -> Unit) {
            getAllStores().forEach(action)
        }

        fun copy(): Accumulator {
            val acc = Accumulator()
            buffers.forEach {
                acc.accumulate(it.copy(null))
            }
            return acc
        }

        class MultiStore(val storedItem: ItemSpecifier?) {
            private val slots: MutableList<BufferSlot> = mutableListOf()

            val count: Int
                get() = slots.sumOf { (buffer, slotIndex) ->
                    val stack = buffer.getStackInSlot(slotIndex)
                    return@sumOf if (storedItem?.test(stack) != false) stack.count else 0
                }

            fun addSlot(buffer: ItemStackBuffer, slotIndex: Int) {
                slots += BufferSlot(buffer, slotIndex)
            }

            fun setItem(item: ItemKey?) {
                if (item == null) {
                    slots.forEach { (buffer, slotIndex) ->
                        buffer.setAndNotify(slotIndex, ItemStack.EMPTY)
                    }
                } else {
                    slots.forEach { (buffer, slotIndex) ->
                        val stack = buffer.getStackInSlot(slotIndex)
                        if (stack.isEmpty) return@forEach
                        buffer.setAndNotify(slotIndex, item.newStack(stack.count))
                    }
                }
            }

            fun insert(stack: ItemStack, simulate: Boolean): ItemStack {
                var rem = stack
                slots.forEach { (buffer, slotIndex) ->
                    rem = buffer.insertItem(slotIndex, stack, simulate)
                    if (rem.isEmpty) return ItemStack.EMPTY
                }
                return rem
            }

            fun extract(amount: Int, simulate: Boolean): ItemStack {
                val key = storedItem ?: return ItemStack.EMPTY
                var remAmount = amount
                slots.forEach { (buffer, slotIndex) ->
                    if (!key.test(buffer.getStackInSlot(slotIndex))) return@forEach
                    remAmount -= buffer.extractItem(slotIndex, remAmount, simulate).count
                    if (remAmount <= 0) {
                        return key.newStack(amount)
                    }
                }
                return if (remAmount >= amount) ItemStack.EMPTY else key.newStack(amount - remAmount)
            }

            fun damage(amount: Int, simulate: Boolean): Int {
                val key = storedItem ?: return 0
                var remAmount = amount
                slots.forEach { (buffer, slotIndex) ->
                    val stack = buffer.getStackInSlot(slotIndex)
                    if (!key.test(stack)) return@forEach
                    val amountHere = remAmount.coerceAtMost(stack.maxDamage - stack.itemDamage + 1)
                    if (amountHere <= 0) return@forEach
                    val damagedStack = stack.copy()
                    if (!simulate) {
                        if (damagedStack.attemptDamageItem(amountHere, CbtMathHelper.cbtRandom, null)) {
                            buffer.setAndNotify(slotIndex, ItemStack.EMPTY)
                        } else {
                            buffer.setAndNotify(slotIndex, damagedStack)
                        }
                    }
                    remAmount -= amountHere
                    if (remAmount <= 0) return amount
                }
                return amount - remAmount
            }

            private data class BufferSlot(val buffer: ItemStackBuffer, val slotIndex: Int)
        }
    }

    class JeiBuffer(private val config: Config) {
        private val contents: Array<JeiIngredient<ItemStack>?> = arrayOfNulls(config.slotCount)
        private var nextEmptyIndex: Int = 0

        fun hasRemainingSlots(): Boolean = nextEmptyIndex < contents.size

        fun setContents(ingredient: JeiIngredient<ItemStack>) {
            contents[nextEmptyIndex++] = ingredient
        }

        @ClientSide.Physical
        fun createJeiUiElements(contRegion: IntRectangle): Collection<JeiUiElement<*>> {
            val elems = mutableListOf<JeiUiElement<*>>()
            val slotPosList = Array(contents.size) { Vec2i.ZERO }
            val slotBg = config.slotBg
            val slotWidth = slotBg.drawable.width
            val slotHeight = slotBg.drawable.height
            val dims = CbtMathHelper.layOutSlotGroup(slotWidth, slotHeight, slotPosList)
            val region = config.uiPosition.computeRegion(contRegion, dims.x, dims.y)
            contents.forEachIndexed { i, slotContents ->
                val slotOffset = slotPosList[i]
                val slotX = region.posX + slotOffset.x
                val slotY = region.posY + slotOffset.y
                val itemRegion = Rect2i(slotX + slotWidth / 2 - 8, slotY + slotHeight / 2 - 8, 16, 16)
                elems.add(object : JeiUiElement<ItemStack> {
                    override val jeiIngredient: JeiIngredient<ItemStack>?
                        get() = slotContents

                    override val ingredientRegion: IntRectangle
                        get() = itemRegion

                    override fun drawElement(ingredient: ItemStack?, partialTicks: Float) {
                        slotBg.drawable.drawFullSize(partialTicks, slotX, slotY)
                        if (ingredient != null) {
                            slotContents!!.drawIcon(itemRegion.posX, itemRegion.posY, ingredient, partialTicks)
                        }
                    }
                })
            }
            return elems
        }
    }

    class JeiAccumulator {
        private val buffers: MutableList<JeiBuffer> = LinkedList<JeiBuffer>()

        fun accumulate(buffer: JeiBuffer) {
            if (buffer.hasRemainingSlots()) {
                buffers.add(buffer)
            }
        }

        fun addIngredient(ingredient: JeiIngredient<ItemStack>): Boolean {
            val iter = buffers.iterator()
            while (iter.hasNext()) {
                val buffer = iter.next()
                if (!buffer.hasRemainingSlots()) {
                    iter.remove()
                    continue
                }
                buffer.setContents(ingredient)
                if (!buffer.hasRemainingSlots()) {
                    iter.remove()
                }
                return true
            }
            return false
        }
    }

    object Type : AutoExportingBufferType<ItemStackBuffer, Accumulator, JeiBuffer, JeiAccumulator>,
        SidedBufferType<ItemStackBuffer, Accumulator, JeiBuffer, JeiAccumulator> {

        override val id: ResourceLocation = CbTweaker.resource("item_stack")

        override val bufferClass: Class<ItemStackBuffer>
            get() = ItemStackBuffer::class.java
        override val accumulatorClass: Class<Accumulator>
            get() = Accumulator::class.java

        context(_: JsonPath)
        override fun loadBufferFactory(dto: TJson.Object): BufferFactory<ItemStackBuffer, JeiBuffer> {
            val config = Config(
                dto.expectInt("slots") ?: 1,
                dto.expectInt("stack_size") ?: 64,
                dto.useAny("item_filter") { IngredientLoader.loadItemFilter(it) },
                dto.expectBool("allow_insert") ?: true,
                dto.expectBool("allow_extract") ?: true,
                dto.expectBool("allow_auto_export") ?: false,
                dto.useAny("ui_position") { UiPosition.load(it) } ?: UiPosition.CENTER,
                dto.useAny("slot_bg") { DrawableData.loadSliceOrBlank(it, 16, 16) } ?: CbtGuiData.ITEM_SLOT
            )
            return object : BufferFactory<ItemStackBuffer, JeiBuffer> {
                override fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): ItemStackBuffer =
                    ItemStackBuffer(config, world, pos, observer)

                override fun createJeiBuffer(): JeiBuffer = JeiBuffer(config)
            }
        }

        override fun attachCapabilities(target: CapabilityVisitor, buffer: ItemStackBuffer) {
            target.visit(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, buffer.restrictedInventory)
        }

        override fun getDefaultAutoExportState(buffer: ItemStackBuffer): Boolean? =
            if (buffer.config.allowAutoExport) false else null

        @ServerSide
        override fun handleAutoExport(
            buffer: ItemStackBuffer,
            front: BlockSide,
            faces: Set<RelativeFace>,
            ticker: TickModulator
        ) {
            if (faces.isEmpty()) {
                ticker.increaseIntervalUntil(20, 60)
                return
            }
            var slotIndex = 0
            while (buffer.getStackInSlot(slotIndex).isEmpty) {
                if (++slotIndex >= buffer.slots) {
                    ticker.increaseIntervalUntil(8, 60)
                    return
                }
            }
            val world = buffer.world
            val pos = buffer.bufPos
            var workDone = false
            run {
                faces.forEach { relFace ->
                    val face = relFace.getFace(front)
                    val dest = world.getTileEntity(pos.offset(face))
                        ?.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.opposite) ?: return@forEach
                    val stack = buffer.getStackInSlot(slotIndex)
                    val rem = ItemHandlerHelper.insertItemStacked(dest, stack, false)
                    if (stack.count > rem.count) {
                        buffer.setStackInSlot(slotIndex, rem)
                        workDone = true
                        if (rem.isEmpty && ++slotIndex >= buffer.slots) return@run
                    }
                }
            }
            if (workDone) {
                ticker.interval = 8
            } else {
                ticker.increaseIntervalUntil(8, 60)
            }
        }

        @ServerSide
        override fun handleDestruction(state: ItemStackBuffer, blockState: IBlockState) {
            val world = state.world
            val pos = state.bufPos
            for (i in 0..<state.slots) {
                val stack = state.getStackInSlot(i)
                if (!stack.isEmpty) {
                    InvHelper.dropItem(stack, world, pos)
                }
            }
        }

        override fun createAccumulator(): Accumulator = Accumulator()

        override fun accumulate(acc: Accumulator, buffer: ItemStackBuffer) {
            acc.accumulate(buffer)
        }

        override fun copyAccumulator(acc: Accumulator): Accumulator = acc.copy()

        @ServerSide
        override fun serializeBufferToNbt(buffer: ItemStackBuffer, dto: NBTTagCompound) {
            buffer.writeToNbt(dto)
        }

        @ServerSide
        override fun deserializeBufferFromNbt(buffer: ItemStackBuffer, dto: NBTTagCompound) {
            buffer.readFromNbt(dto)
        }

        override fun createUiElement(buffer: ItemStackBuffer): UiElement = buffer.createUiElement()

        override fun createJeiAccumulator(): JeiAccumulator = JeiAccumulator()

        override fun jeiAccumulate(acc: JeiAccumulator, buffer: JeiBuffer) {
            acc.accumulate(buffer)
        }

        @ClientSide.Physical
        override fun createJeiUiElements(
            buffer: JeiBuffer,
            contRegion: IntRectangle,
            jeiHelpers: IJeiHelpers
        ): Collection<JeiUiElement<*>> = buffer.createJeiUiElements(contRegion)
    }

    class ItemMatcher(
        private val item: ItemSpecifier,
        private val count: Int,
        private val consumeType: ItemConsumeType
    ) :
        IngredientMatcher<Accumulator, JeiAccumulator> {

        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean {
            val scaledCount = CbtMathHelper.scaleConsumeInt(count, consumeFactor, checkMode)
            if (scaledCount <= 0) return true
            val store = acc.value.getStore(item) ?: return false
            when (consumeType) {
                ItemConsumeType.CONSUME -> {
                    val extracted = store.extract(scaledCount, false)
                    if (extracted.count < scaledCount) return false
                    val containerStack = extracted.item.getContainerItem(extracted)
                    if (!containerStack.isEmpty) {
                        containerStack.count = extracted.count
                        val rem = store.insert(containerStack, false)
                        if (!rem.isEmpty) {
                            // we don't track the buffer(s) that the ingredients were extracted from, so this just drops
                            // rem at a random buffer's position. hopefully it's empty in every reasonable case
                            acc.value.insertOrDrop(rem, checkMode)
                        }
                    }
                    return true
                }
                ItemConsumeType.DELETE -> return store.extract(scaledCount, false).count >= scaledCount
                ItemConsumeType.DAMAGE -> return store.damage(scaledCount, false) >= scaledCount
                ItemConsumeType.KEEP -> return store.extract(scaledCount, true).count >= scaledCount
            }
        }

        private val jeiIngredient: JeiItemIngredient = when (consumeType) {
            ItemConsumeType.CONSUME, ItemConsumeType.DELETE ->
                JeiItemIngredient(item.newStack(count), JeiIngredient.Role.INPUT, null)
            ItemConsumeType.DAMAGE ->
                JeiItemIngredient(item.newStack(1), JeiIngredient.Role.INPUT, JeiIngredient.Annotation.Damage(count))
            ItemConsumeType.KEEP ->
                JeiItemIngredient(item.newStack(count), JeiIngredient.Role.INPUT, JeiIngredient.Annotation.Keep)
        }

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("item")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): ItemMatcher = ItemMatcher(
                ItemSpecifier.load(dto),
                dto.expectInt("count") ?: 1,
                dto.useString("consume") {
                    ItemConsumeType.serializer.deserializeFromJson(it)
                } ?: ItemConsumeType.CONSUME
            )
        }
    }

    class OreDictionaryMatcher( // could add an ore name -> multistore cache?
        private val oreEntry: OreEntry,
        private val count: Int,
        private val consumeType: ItemConsumeType
    ) : IngredientMatcher<Accumulator, JeiAccumulator> {
        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean {
            var scaledCount = CbtMathHelper.scaleConsumeInt(count, consumeFactor, checkMode)
            if (scaledCount <= 0) return true
            when (consumeType) {
                ItemConsumeType.CONSUME -> {
                    acc.value.forEachMatching {
                        val extracted = it.extract(scaledCount, false)
                        if (extracted.isEmpty) return@forEachMatching
                        val containerStack = extracted.item.getContainerItem(extracted)
                        if (!containerStack.isEmpty) {
                            containerStack.count = extracted.count
                            val rem = it.insert(containerStack, false)
                            if (!rem.isEmpty) {
                                acc.value.insertOrDrop(rem, checkMode)
                            }
                        }
                        scaledCount -= extracted.count
                        if (scaledCount <= 0) return true
                    }
                    return false
                }
                ItemConsumeType.DELETE -> {
                    acc.value.forEachMatching {
                        scaledCount -= it.extract(scaledCount, false).count
                        if (scaledCount <= 0) return true
                    }
                    return false
                }
                ItemConsumeType.DAMAGE -> {
                    acc.value.forEachMatching {
                        scaledCount -= it.damage(scaledCount, false)
                        if (scaledCount <= 0) return true
                    }
                    return false
                }
                ItemConsumeType.KEEP -> {
                    acc.value.forEachMatching {
                        scaledCount -= it.extract(scaledCount, true).count
                        if (scaledCount <= 0) return true
                    }
                    return false
                }
            }
        }

        private inline fun Accumulator.forEachMatching(action: (Accumulator.MultiStore) -> Unit) {
            forEach { store ->
                if (store.storedItem?.matchesOreEntry(oreEntry) != true) return@forEach
                action(store)
            }
        }

        private val jeiIngredient: JeiItemIngredient = when (consumeType) {
            ItemConsumeType.CONSUME, ItemConsumeType.DELETE ->
                JeiItemIngredient(oreEntry, count, JeiIngredient.Role.INPUT, null)
            ItemConsumeType.DAMAGE ->
                JeiItemIngredient(oreEntry, 1, JeiIngredient.Role.INPUT, JeiIngredient.Annotation.Damage(count))
            ItemConsumeType.KEEP ->
                JeiItemIngredient(oreEntry, count, JeiIngredient.Role.INPUT, JeiIngredient.Annotation.Keep)
        }

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("ore_dict")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): OreDictionaryMatcher = OreDictionaryMatcher(
                OreEntry(dto.expectStringValue("ore")),
                dto.expectInt("count") ?: 1,
                dto.useString("consume") {
                    ItemConsumeType.serializer.deserializeFromJson(it)
                } ?: ItemConsumeType.CONSUME
            )
        }
    }

    class ItemProvider(private val item: ItemKey, private val count: Int, private val chance: Float) :
        IngredientProvider<Accumulator, JeiAccumulator> {

        override fun insertFinal(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean =
            !CbtMathHelper.rollProduce(chance, checkMode) || acc.value.insert(item.newStack(count), false).isEmpty

        private val jeiIngredient: JeiItemIngredient = JeiItemIngredient(
            item.newStack(count),
            JeiIngredient.Role.OUTPUT,
            JeiIngredient.Annotation.fromChance(chance)
        )

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("item")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): ItemProvider = ItemProvider(
                ItemKey.Serializer.deserializeFromJson(dto),
                dto.expectInt("count") ?: 1,
                dto.expectFloat("chance") ?: 1F
            )
        }
    }
}
