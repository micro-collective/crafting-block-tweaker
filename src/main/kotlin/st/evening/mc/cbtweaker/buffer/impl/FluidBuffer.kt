package st.evening.mc.cbtweaker.buffer.impl

import mezz.jei.api.IJeiHelpers
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidTankInfo
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidTankProperties
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler
import net.minecraftforge.items.CapabilityItemHandler
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
import st.evening.mc.cbtweaker.compat.jei.ingredient.impl.JeiFluidIngredient
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.gui.element.FluidTankControl
import st.evening.mc.cbtweaker.gui.inventory.SyncedUiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.network.C2SInteractTankTransfer
import st.evening.mc.cbtweaker.util.CbtMathHelper
import st.evening.mc.cbtweaker.util.gui.FluidBarRenderer
import st.evening.mc.cbtweaker.util.gui.Positioned
import st.evening.mc.cbtweaker.util.gui.TankDrawData
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.cbtweaker.util.machine.TransferType
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.component.fluid.RatedFluidHandler
import st.evening.mc.prelude.api.data.ser.FluidStackSerializer
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectFloat
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectIntValue
import st.evening.mc.prelude.api.data.tjson.useIntValue
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.game.CapabilityVisitor
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.FluidKey
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.copyWithAmount
import st.evening.mc.prelude.api.util.game.forceCopyWithAmount
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace

class FluidBuffer(
    private val config: Config,
    val world: World,
    val bufPos: BlockPos,
    storedFluid: FluidStack?,
    private val observer: BufferObserver?
) : IFluidTank, IFluidHandler, IFluidTankProperties, NbtCompoundSerializable {
    private val tankProps: Array<IFluidTankProperties> = arrayOf(this)
    val restrictedHandler: IFluidHandler = RatedFluidHandler(this, config.insertRate ?: -1, config.extractRate ?: -1)

    private val tankState: ValueStateAtom<FluidStack?> = ValueStateAtom(storedFluid, FluidStackSerializer)

    // would be a huge problem if someone mutated this, but making a copy every time would be inefficient...
    override fun getFluid(): FluidStack? = tankState.value

    fun setFluid(newFluid: FluidStack?) {
        val storedFluid = tankState.value
        if (newFluid == null || newFluid.amount <= 0) {
            if (storedFluid == null) return
            tankState.value = null
        } else {
            if (storedFluid != null && storedFluid.isFluidStackIdentical(newFluid)) return
            tankState.value = newFluid
        }
        observer?.onIngredientsChanged()
    }

    fun isEmpty(): Boolean = fluid?.let { it.amount <= 0 } ?: true

    override fun getFluidAmount(): Int = fluid?.amount ?: 0

    override fun getCapacity(): Int = config.capacity

    override fun getContents(): FluidStack? = fluid

    override fun canFill(): Boolean = true

    override fun canDrain(): Boolean = true

    override fun canFillFluidType(fluidStack: FluidStack?): Boolean =
        fluidStack == null || config.fluidFilter?.let { fluidStack.fluid == it.value } ?: true

    override fun canDrainFluidType(fluidStack: FluidStack?): Boolean = true

    override fun getInfo(): FluidTankInfo = FluidTankInfo(this)

    override fun getTankProperties(): Array<out IFluidTankProperties> = tankProps

    override fun fill(resource: FluidStack?, doFill: Boolean): Int {
        if (resource == null || resource.amount <= 0) return 0
        config.fluidFilter?.let {
            if (resource.fluid != it.value) return 0
        }
        val storedFluid = fluid
        val storedAmount: Int = when {
            storedFluid == null -> 0
            storedFluid.isFluidEqual(resource) -> storedFluid.amount
            else -> return 0
        }
        val toTransfer = resource.amount.coerceAtMost(capacity - storedAmount)
        if (toTransfer <= 0) return 0
        if (doFill) {
            fluid = resource.forceCopyWithAmount(storedAmount + toTransfer)
        }
        return toTransfer
    }

    private fun doDrain(storedFluid: FluidStack, maxDrain: Int, doDrain: Boolean): FluidStack {
        val toTransfer = storedFluid.amount.coerceAtMost(maxDrain)
        if (doDrain) {
            fluid = storedFluid.copyWithAmount(storedFluid.amount - toTransfer)
        }
        return storedFluid.forceCopyWithAmount(toTransfer)
    }

    override fun drain(maxDrain: Int, doDrain: Boolean): FluidStack? {
        if (maxDrain <= 0) return null
        val storedFluid = fluid ?: return null
        return doDrain(storedFluid, maxDrain, doDrain)
    }

    override fun drain(resource: FluidStack?, doDrain: Boolean): FluidStack? {
        if (resource == null || resource.amount <= 0) return null
        val storedFluid = fluid
        if (!resource.isFluidEqual(storedFluid)) return null
        return doDrain(storedFluid as FluidStack, resource.amount, doDrain)
    }

    fun copy(observer: BufferObserver?): FluidBuffer = FluidBuffer(config, world, bufPos, fluid, observer)

    override fun writeToNbt(dto: NBTTagCompound) {
        tankState.value?.writeToNBT(dto)
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        tankState.setValueFromSync(FluidStack.loadFluidStackFromNBT(dto))
    }

    private fun createUiElement(): UiElement = UiElementImpl()

    private inner class UiElementImpl : SyncedUiElement, C2SInteractTankTransfer.Listener {
        override val syncData: Piecewise
            get() = tankState

        @ClientSide.Strong
        override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
            config.uiTank.uiPosition.placeElement(
                uiIndex, layout, wrapper,
                FluidTankControl(uiIndex, this@FluidBuffer, config.uiTank.data, config.allowUiInteraction)
            )
        }

        @ServerSide
        override fun handleTankTransfer(player: EntityPlayerMP, transferType: TransferType) {
            val stack = player.inventory.itemStack
            if (stack.isEmpty) return
            val playerInv = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ?: return
            val result = when (transferType) {
                TransferType.INSERT ->
                    FluidUtil.tryEmptyContainerAndStow(stack, this@FluidBuffer, playerInv, Int.MAX_VALUE, player, true)
                TransferType.EXTRACT ->
                    FluidUtil.tryFillContainerAndStow(stack, this@FluidBuffer, playerInv, Int.MAX_VALUE, player, true)
            }
            if (result.isSuccess) {
                player.updateHeldItem()
            }
        }
    }

    class Config(
        val capacity: Int,
        val insertRate: Int?,
        val extractRate: Int?,
        val fluidFilter: Lazy<Fluid>?, // must be lazy because the registry can be mutated after buffers are loaded
        val allowAutoExport: Boolean,
        val allowBlockInteraction: Boolean,
        val uiTank: Positioned<TankDrawData>,
        val allowUiInteraction: Boolean
    )

    class Accumulator {
        private val buffers: MutableList<FluidBuffer> = mutableListOf()
        private val tanks: MutableMap<FluidKey?, MultiTank> = mutableMapOf()

        fun accumulate(buffer: FluidBuffer) {
            buffers.add(buffer)
            val key = FluidKey.fromStack(buffer.fluid)
            tanks.getOrPut(key) { MultiTank(key) }.addTank(buffer)
        }

        fun getTank(key: FluidKey?): IFluidTank = tanks[key] ?: EmptyFluidHandler.INSTANCE

        @PreludeInternal
        fun getAllTanks(): Map<FluidKey?, IFluidTank> = tanks

        fun insert(stack: FluidStack?, doFill: Boolean): Int {
            if (stack == null || stack.amount <= 0) return 0
            val fullAmount = stack.amount
            tanks[FluidKey.fromStack(stack)]?.let {
                stack.amount -= it.fill(stack, doFill)
                if (stack.amount <= 0) {
                    stack.amount = fullAmount
                    return fullAmount
                }
            }
            tanks[null]?.let {
                stack.amount -= it.fill(stack, doFill)
                if (stack.amount <= 0) {
                    stack.amount = fullAmount
                    return fullAmount
                }
            }
            val transferred = fullAmount - stack.amount
            stack.amount = fullAmount
            return transferred
        }

        @OptIn(PreludeInternal::class)
        inline fun forEach(action: (Map.Entry<FluidKey?, IFluidTank>) -> Unit) {
            getAllTanks().forEach(action)
        }

        fun copy(): Accumulator {
            val acc = Accumulator()
            buffers.forEach {
                acc.accumulate(it.copy(null))
            }
            return acc
        }

        private class MultiTank(private val key: FluidKey?) : IFluidTank {
            private val tanks: MutableList<IFluidTank> = mutableListOf()

            fun addTank(tank: IFluidTank) {
                tanks += tank
            }

            override fun getFluid(): FluidStack? {
                val fluidKey = key ?: return null
                val amount = fluidAmount
                return if (amount > 0) fluidKey.newStack(amount) else null
            }

            override fun getFluidAmount(): Int {
                val fluidKey = key
                return if (fluidKey == null) {
                    tanks.sumOf { it.fluidAmount }
                } else {
                    tanks.sumOf { if (fluidKey.matches(it.fluid)) it.fluidAmount else 0 }
                }
            }

            override fun getCapacity(): Int = tanks.sumOf { it.capacity }

            override fun getInfo(): FluidTankInfo = FluidTankInfo(this)

            override fun fill(resource: FluidStack, doFill: Boolean): Int {
                val fullAmount = resource.amount
                tanks.forEach {
                    resource.amount -= it.fill(resource, doFill)
                    if (resource.amount <= 0) {
                        resource.amount = fullAmount
                        return fullAmount
                    }
                }
                val transferred = fullAmount - resource.amount
                resource.amount = fullAmount
                return transferred
            }

            override fun drain(maxDrain: Int, doDrain: Boolean): FluidStack? {
                var filter = key
                var remAmount = maxDrain
                tanks.forEach { tank ->
                    if (filter == null) {
                        val drained = tank.drain(remAmount, doDrain)
                        if (drained != null && drained.amount > 0) {
                            remAmount -= drained.amount
                            if (remAmount <= 0) {
                                return drained
                            }
                            filter = FluidKey.fromStack(drained)
                        }
                    } else if (filter.matches(tank.fluid)) {
                        val drained = tank.drain(remAmount, doDrain)
                        if (drained != null && drained.amount > 0) {
                            remAmount -= drained.amount
                            if (remAmount <= 0) {
                                return filter.newStack(maxDrain)
                            }
                        }
                    }
                }
                return if (filter == null || remAmount >= maxDrain) null else filter.newStack(maxDrain - remAmount)
            }
        }
    }

    class JeiBuffer(private val config: Config) {
        private var contents: JeiIngredient<FluidStack>? = null

        fun isEmpty(): Boolean = contents == null

        fun setContents(ingredient: JeiIngredient<FluidStack>) {
            contents = ingredient
        }

        @ClientSide.Physical
        fun createJeiUiElement(contRegion: IntRectangle): JeiUiElement<*> {
            val uiTank = config.uiTank.data
            val barBg = uiTank.bgTexture.drawable
            val pos = config.uiTank.uiPosition.computePosition(contRegion, barBg.width, barBg.height)
            val barRegion = Rect2i(pos.x + uiTank.fgOffsetX, pos.y + uiTank.fgOffsetY, uiTank.fgWidth, uiTank.fgHeight)
            val barRenderer = FluidBarRenderer()
            return object : JeiUiElement<FluidStack> {
                override val jeiIngredient: JeiIngredient<FluidStack>?
                    get() = contents

                override val ingredientRegion: IntRectangle
                    get() = barRegion

                override fun drawElement(ingredient: FluidStack?, partialTicks: Float) {
                    config.uiTank.data.bgTexture.drawable.drawFullSize(partialTicks, pos.x, pos.y)
                    if (ingredient == null) return
                    val amount = ingredient.amount
                    if (amount <= 0) return
                    barRenderer.drawBar(
                        ingredient,
                        amount,
                        config.capacity.coerceAtMost(amount * 3), // ensure visibility even for very small amounts
                        barRegion,
                        partialTicks
                    )
                }
            }
        }
    }

    class JeiAccumulator {
        private val buffers: ArrayDeque<JeiBuffer> = ArrayDeque()

        fun accumulate(buffer: JeiBuffer) {
            if (buffer.isEmpty()) {
                buffers += buffer
            }
        }

        fun addIngredient(ingredient: JeiIngredient<FluidStack>): Boolean {
            while (buffers.isNotEmpty()) {
                val buffer = buffers.removeFirst()
                if (buffer.isEmpty()) {
                    buffer.setContents(ingredient)
                    return true
                }
            }
            return false
        }
    }

    object Type : AutoExportingBufferType<FluidBuffer, Accumulator, JeiBuffer, JeiAccumulator>,
        SidedBufferType<FluidBuffer, Accumulator, JeiBuffer, JeiAccumulator> {

        val DEFAULT_UI_TANK: Positioned<TankDrawData> =
            Positioned(UiPosition.CENTER, TankDrawData(CbtGuiData.FLUID_SLOT, 1, 1, 16, 16))

        override val id: ResourceLocation = CbTweaker.resource("fluid")

        override val bufferClass: Class<FluidBuffer>
            get() = FluidBuffer::class.java
        override val accumulatorClass: Class<Accumulator>
            get() = Accumulator::class.java

        context(_: JsonPath)
        override fun loadBufferFactory(dto: TJson.Object): BufferFactory<FluidBuffer, JeiBuffer> {
            val capacity = dto.useIntValue("capacity") {
                if (it < 0) throw SerializationException.withPath("Capacity must be positive!")
                return@useIntValue it
            }
            val config = Config(
                capacity,
                dto.expectInt("insert_rate"),
                dto.expectInt("extract_rate"),
                dto.useString("fluid_filter") { lazy { FluidRegistry.getFluid(it) } },
                dto.expectBool("allow_auto_export") ?: false,
                dto.expectBool("allow_block_interaction") ?: true,
                dto.useObject("ui_tank") { TankDrawData.loadPositioned(it, DEFAULT_UI_TANK) } ?: DEFAULT_UI_TANK,
                dto.expectBool("allow_ui_interaction") ?: true
            )
            return object : BufferFactory<FluidBuffer, JeiBuffer> {
                override fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): FluidBuffer =
                    FluidBuffer(config, world, pos, null, observer)

                override fun createJeiBuffer(): JeiBuffer = JeiBuffer(config)
            }
        }

        override fun attachCapabilities(target: CapabilityVisitor, buffer: FluidBuffer) {
            target.visit(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, buffer.restrictedHandler)
        }

        override fun getDefaultAutoExportState(buffer: FluidBuffer): Boolean? =
            if (buffer.config.allowAutoExport) false else null

        @ServerSide
        override fun handleAutoExport(
            buffer: FluidBuffer,
            front: BlockSide,
            faces: Set<RelativeFace>,
            ticker: TickModulator
        ) {
            if (faces.isEmpty()) {
                ticker.increaseIntervalUntil(20, 60)
                return
            }
            if (buffer.isEmpty()) {
                ticker.increaseIntervalUntil(8, 60)
                return
            }
            val world = buffer.world
            val pos = buffer.bufPos
            val receivers = faces.mapNotNull {
                val absFace = it.getFace(front)
                return@mapNotNull world.getTileEntity(pos.offset(absFace))
                    ?.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, absFace.opposite)
            }
            if (receivers.isEmpty()) {
                ticker.increaseIntervalUntil(20, 60)
                return
            }
            var workDone = false
            run {
                faces.forEach {
                    val absFace = it.getFace(front)
                    val te = world.getTileEntity(pos.offset(absFace)) ?: return@forEach
                    val dest = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, absFace.opposite)
                        ?: return@forEach
                    val result = FluidUtil.tryFluidTransfer(
                        dest, buffer, buffer.config.extractRate ?: Int.MAX_VALUE, true
                    )
                    if (result != null && result.amount >= 0) {
                        workDone = true
                        if (buffer.isEmpty()) return@run
                    }
                }
            }
            if (workDone) {
                ticker.interval = 8
            } else {
                ticker.increaseIntervalUntil(8, 60)
            }
        }

        override fun handleInteraction(
            state: FluidBuffer, blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
            face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
        ): Boolean = state.config.allowBlockInteraction && FluidUtil.interactWithFluidHandler(player, hand, state)

        override fun createAccumulator(): Accumulator = Accumulator()

        override fun accumulate(acc: Accumulator, buffer: FluidBuffer) {
            acc.accumulate(buffer)
        }

        override fun copyAccumulator(acc: Accumulator): Accumulator = acc.copy()

        @ServerSide
        override fun serializeBufferToNbt(buffer: FluidBuffer, dto: NBTTagCompound) {
            buffer.writeToNbt(dto)
        }

        @ServerSide
        override fun deserializeBufferFromNbt(buffer: FluidBuffer, dto: NBTTagCompound) {
            buffer.readFromNbt(dto)
        }

        override fun createUiElement(buffer: FluidBuffer): UiElement = buffer.createUiElement()

        override fun createJeiAccumulator(): JeiAccumulator = JeiAccumulator()

        override fun jeiAccumulate(acc: JeiAccumulator, buffer: JeiBuffer) {
            acc.accumulate(buffer)
        }

        @ClientSide.Physical
        override fun createJeiUiElements(
            buffer: JeiBuffer,
            contRegion: IntRectangle,
            jeiHelpers: IJeiHelpers
        ): Collection<JeiUiElement<*>> = listOf(buffer.createJeiUiElement(contRegion))
    }

    class FluidMatcher(private val fluid: FluidKey, private val amount: Int, private val doConsume: Boolean) :
        IngredientMatcher<Accumulator, JeiAccumulator> {

        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean {
            val scaledAmount = CbtMathHelper.scaleConsumeInt(amount, consumeFactor, checkMode)
            if (scaledAmount <= 0) return true
            val drained = acc.value.getTank(fluid).drain(scaledAmount, doConsume)
            return drained != null && drained.amount >= scaledAmount
        }

        private val jeiIngredient: JeiFluidIngredient =
            JeiFluidIngredient(fluid.newStack(amount)!!, false, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("fluid")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): FluidMatcher = FluidMatcher(
                FluidKey.Serializer.deserializeFromJson(dto),
                dto.expectIntValue("amount"),
                dto.expectBool("consume") ?: true
            )
        }
    }

    class FluidRateMatcher(private val fluid: FluidKey, private val rate: Int) :
        IngredientMatcher<Accumulator, JeiAccumulator> {

        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            !checkMode || consume(acc, consumeFactor, true) // make sure there's enough fluid to start

        override fun consumePeriodic(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            consume(acc, consumeFactor, checkMode)

        private fun consume(
            acc: Lazy<Accumulator>,
            consumeFactor: Float,
            checkMode: Boolean
        ): Boolean {
            val scaledAmount = CbtMathHelper.scaleConsumeInt(rate, consumeFactor, checkMode)
            if (scaledAmount <= 0) return true
            val drained = acc.value.getTank(fluid).drain(scaledAmount, true)
            return drained != null && drained.amount >= scaledAmount
        }

        private val jeiIngredient: JeiFluidIngredient =
            JeiFluidIngredient(fluid.newStack(rate)!!, true, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("fluid_rate")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): FluidRateMatcher =
                FluidRateMatcher(FluidKey.Serializer.deserializeFromJson(dto), dto.expectIntValue("rate"))
        }
    }

    class FluidProvider(private val fluid: FluidKey, private val amount: Int, private val chance: Float) :
        IngredientProvider<Accumulator, JeiAccumulator> {

        override fun insertFinal(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean =
            !CbtMathHelper.rollProduce(chance, checkMode) || acc.value.insert(fluid.newStack(amount), true) >= amount

        private val jeiIngredient: JeiFluidIngredient =
            JeiFluidIngredient(fluid.newStack(amount)!!, false, JeiIngredient.Role.OUTPUT, chance)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("fluid")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): FluidProvider = FluidProvider(
                FluidKey.Serializer.deserializeFromJson(dto),
                dto.expectIntValue("amount"),
                dto.expectFloat("chance") ?: 0F
            )
        }
    }

    class FluidRateProvider(private val fluid: FluidKey, private val rate: Int) :
        IngredientProvider<Accumulator, JeiAccumulator> {

        override fun insertPeriodic(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean {
            acc.value.insert(fluid.newStack(rate), true)
            return true
        }

        private val jeiIngredient: JeiFluidIngredient =
            JeiFluidIngredient(fluid.newStack(rate)!!, true, JeiIngredient.Role.OUTPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("fluid_rate")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): FluidRateProvider =
                FluidRateProvider(FluidKey.Serializer.deserializeFromJson(dto), dto.expectIntValue("rate"))
        }
    }
}
