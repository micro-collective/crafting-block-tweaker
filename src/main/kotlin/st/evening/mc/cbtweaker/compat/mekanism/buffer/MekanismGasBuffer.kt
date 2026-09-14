package st.evening.mc.cbtweaker.compat.mekanism.buffer

import mekanism.api.gas.Gas
import mekanism.api.gas.GasRegistry
import mekanism.api.gas.GasStack
import mekanism.api.gas.IGasItem
import mekanism.common.base.target.GasHandlerTarget
import mekanism.common.capabilities.Capabilities
import mekanism.common.util.EmitUtils
import mezz.jei.api.IJeiHelpers
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
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
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.cbtweaker.compat.mekanism.gas.GasHelper
import st.evening.mc.cbtweaker.compat.mekanism.gas.GasStackSerializer
import st.evening.mc.cbtweaker.compat.mekanism.gas.RatedGasTank
import st.evening.mc.cbtweaker.compat.mekanism.gas.SingleGasTank
import st.evening.mc.cbtweaker.compat.mekanism.gas.isEqual
import st.evening.mc.cbtweaker.compat.mekanism.gui.GasBarRenderer
import st.evening.mc.cbtweaker.compat.mekanism.gui.GasTankControl
import st.evening.mc.cbtweaker.compat.mekanism.jei.JeiMekanismGasIngredient
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.gui.inventory.SyncedUiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.network.C2SInteractTankTransfer
import st.evening.mc.cbtweaker.util.CbtMathHelper
import st.evening.mc.cbtweaker.util.gui.Positioned
import st.evening.mc.cbtweaker.util.gui.TankDrawData
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.cbtweaker.util.machine.TransferType
import st.evening.mc.prelude.api.PreludeInternal
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
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.game.CapabilityVisitor
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.RequireMod
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace

@RequireMod(MekanismCompat.MOD_ID)
class MekanismGasBuffer(
    private val config: Config,
    val world: World,
    val bufPos: BlockPos,
    storedGas: GasStack?,
    private val observer: BufferObserver?
) : SingleGasTank, NbtCompoundSerializable {
    val restrictedTank: SingleGasTank = RatedGasTank(this, config.insertRate ?: -1, config.extractRate ?: -1)

    private val tankState: ValueStateAtom<GasStack?> = ValueStateAtom(storedGas, GasStackSerializer)

    override fun getGas(): GasStack? = tankState.value

    fun setGas(newGas: GasStack?) {
        val storedGas = tankState.value
        if (newGas == null || newGas.amount <= 0) {
            if (storedGas == null) return
            tankState.value = null
        } else {
            if (storedGas != null && storedGas.isEqual(newGas)) return
            tankState.value = newGas
        }
        observer?.onIngredientsChanged()
    }

    fun isEmpty(): Boolean = tankState.value?.let { it.amount <= 0 } ?: true

    override fun getStored(): Int = gas?.amount ?: 0

    override fun getMaxGas(): Int = config.capacity

    override val gasType: Gas?
        get() = gas?.gas

    override fun canReceiveGas(face: EnumFacing, gas: Gas): Boolean = config.gasFilter?.let { gas == it.value } ?: true

    override fun canDrawGas(face: EnumFacing, gas: Gas): Boolean = true

    override fun receiveGas(face: EnumFacing?, gasStack: GasStack?, commit: Boolean): Int {
        if (gasStack == null || gasStack.amount <= 0) return 0
        config.gasFilter?.let {
            if (gasStack.gas != it.value) return 0
        }
        val storedGas = gas
        val storedAmount = when {
            storedGas == null -> 0
            storedGas.isGasEqual(gasStack) -> storedGas.amount
            else -> return 0
        }
        val toTransfer = gasStack.amount.coerceAtMost(maxGas - storedAmount)
        if (toTransfer <= 0) return 0
        if (commit) {
            gas = GasStack(gasStack.gas, storedAmount + toTransfer)
        }
        return toTransfer
    }

    override fun drawGas(face: EnumFacing?, amount: Int, commit: Boolean): GasStack? {
        if (amount <= 0) return null
        val storedGas = gas ?: return null
        val toTransfer = storedGas.amount.coerceAtMost(amount)
        if (commit) {
            gas = GasStack(storedGas.gas, storedGas.amount - toTransfer)
        }
        return GasStack(storedGas.gas, toTransfer)
    }

    fun copy(observer: BufferObserver?): MekanismGasBuffer = MekanismGasBuffer(config, world, bufPos, gas, observer)

    override fun writeToNbt(dto: NBTTagCompound) {
        tankState.value?.write(dto)
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        tankState.setValueFromSync(GasStack.readFromNBT(dto))
    }

    private fun createUiElement(): UiElement = UiElementImpl()

    private inner class UiElementImpl : SyncedUiElement, C2SInteractTankTransfer.Listener {
        override val syncData: Piecewise
            get() = tankState

        @ClientSide.Strong
        override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
            config.uiTank.uiPosition.placeElement(
                uiIndex, layout, wrapper,
                GasTankControl(uiIndex, this@MekanismGasBuffer, config.uiTank.data, config.allowUiInteraction)
            )
        }

        @ServerSide
        override fun handleTankTransfer(player: EntityPlayerMP, transferType: TransferType) {
            val heldStack = player.inventory.itemStack
            if (heldStack.isEmpty) return
            val item = heldStack.item
            if (item !is IGasItem) return
            when (transferType) {
                TransferType.INSERT -> {
                    val gasStack = item.removeGas(heldStack, maxGas - stored)
                    if (gasStack == null || gasStack.amount <= 0) return
                    gasStack.amount -= receiveGas(null, gasStack, true)
                    if (gasStack.amount > 0) {
                        item.addGas(heldStack, gasStack)
                    }
                }
                TransferType.EXTRACT -> {
                    val available = drawGas(null, item.getMaxGas(heldStack), false)
                    if (available == null || available.amount <= 0) return
                    val transferred = item.addGas(heldStack, available)
                    if (transferred <= 0) return
                    drawGas(null, transferred, true)
                }
            }
            world.playSound(
                null, player.posX, player.posY + 0.5, player.posZ,
                CbTweaker.defns.soundGasTransfer, SoundCategory.BLOCKS, 1F, 1F
            )
            player.updateHeldItem()
        }
    }

    class Config(
        val capacity: Int,
        val insertRate: Int?,
        val extractRate: Int?,
        val gasFilter: Lazy<Gas>?, // must be lazy because the registry can be mutated after buffers are loaded
        val allowAutoExport: Boolean,
        val allowBlockInteraction: Boolean,
        val uiTank: Positioned<TankDrawData>,
        val allowUiInteraction: Boolean
    )

    class Accumulator {
        private val buffers: MutableList<MekanismGasBuffer> = mutableListOf()
        private val tanks: MutableMap<Gas?, MultiTank> = mutableMapOf()

        fun accumulate(buffer: MekanismGasBuffer) {
            buffers += buffer
            val gas = buffer.gasType
            tanks.getOrPut(gas) { MultiTank(gas) }.addTank(buffer)
        }

        fun getTank(gas: Gas?): SingleGasTank = tanks[gas] ?: SingleGasTank.Empty

        @PreludeInternal
        fun getAllTanks(): Map<Gas?, SingleGasTank> = tanks

        fun insert(stack: GasStack?, commit: Boolean): Int {
            if (stack == null || stack.amount <= 0) return 0
            val fullAmount = stack.amount
            tanks[stack.gas]?.let {
                stack.amount -= it.receiveGas(EnumFacing.NORTH, stack, commit)
                if (stack.amount <= 0) {
                    stack.amount = fullAmount
                    return fullAmount
                }
            }
            tanks[null]?.let {
                stack.amount -= it.receiveGas(EnumFacing.NORTH, stack, commit)
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
        inline fun forEach(action: (Map.Entry<Gas?, SingleGasTank>) -> Unit) {
            getAllTanks().forEach(action)
        }

        fun copy(): Accumulator {
            val acc = Accumulator()
            buffers.forEach {
                acc.accumulate(it.copy(null))
            }
            return acc
        }

        private class MultiTank(override val gasType: Gas?) : SingleGasTank {
            private val tanks: MutableList<SingleGasTank> = mutableListOf()

            fun addTank(tank: SingleGasTank) {
                tanks += tank
            }

            override fun getGas(): GasStack? {
                val gas = gasType ?: return null
                val amount = stored
                return if (amount > 0) GasStack(gas, amount) else null
            }

            override fun getStored(): Int {
                val gas = gasType
                return if (gas == null) {
                    tanks.sumOf { it.stored }
                } else {
                    tanks.sumOf { if (it.gasType == gas) it.stored else 0 }
                }
            }

            override fun getMaxGas(): Int = tanks.sumOf { it.maxGas }

            override fun receiveGas(face: EnumFacing?, resource: GasStack, commit: Boolean): Int {
                val fullAmount = resource.amount
                tanks.forEach {
                    resource.amount -= it.receiveGas(null, resource, commit)
                    if (resource.amount <= 0) {
                        resource.amount = fullAmount
                        return fullAmount
                    }
                }
                val transferred = fullAmount - resource.amount
                resource.amount = fullAmount
                return transferred
            }

            override fun drawGas(face: EnumFacing?, maxDrain: Int, commit: Boolean): GasStack? {
                var filter = gasType
                var remAmount = maxDrain
                tanks.forEach { tank ->
                    if (filter == null) {
                        val drained = tank.drawGas(null, remAmount, commit)
                        if (drained != null && drained.amount > 0) {
                            remAmount -= drained.amount
                            if (remAmount <= 0) {
                                return drained
                            }
                            filter = drained.gas
                        }
                    } else if (filter == tank.gasType) {
                        val drained = tank.drawGas(null, remAmount, commit)
                        if (drained != null && drained.amount > 0) {
                            remAmount -= drained.amount
                            if (remAmount <= 0) {
                                return GasStack(filter, maxDrain)
                            }
                        }
                    }
                }
                return if (filter == null || remAmount >= maxDrain) null else GasStack(filter, maxDrain - remAmount)
            }

            override fun canReceiveGas(enumFacing: EnumFacing, gas: Gas): Boolean = gasType?.let { gas == it } ?: true

            override fun canDrawGas(enumFacing: EnumFacing, gas: Gas): Boolean = gasType?.let { gas == it } ?: true
        }
    }

    class JeiBuffer(private val config: Config) {
        private var contents: JeiIngredient<GasStack>? = null

        fun isEmpty(): Boolean = contents == null

        fun setContents(ingredient: JeiIngredient<GasStack>) {
            contents = ingredient
        }

        @ClientSide.Physical
        fun createJeiUiElement(contRegion: IntRectangle): JeiUiElement<*> {
            val uiTank = config.uiTank.data
            val barBg = uiTank.bgTexture.drawable
            val pos = config.uiTank.uiPosition.computePosition(contRegion, barBg.width, barBg.height)
            val barRegion = Rect2i(pos.x + uiTank.fgOffsetX, pos.y + uiTank.fgOffsetY, uiTank.fgWidth, uiTank.fgHeight)
            val barRenderer = GasBarRenderer()
            return object : JeiUiElement<GasStack> {
                override val jeiIngredient: JeiIngredient<GasStack>?
                    get() = contents

                override val ingredientRegion: IntRectangle
                    get() = barRegion

                override fun drawElement(ingredient: GasStack?, partialTicks: Float) {
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

        fun addIngredient(ingredient: JeiIngredient<GasStack>): Boolean {
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

    object Type : AutoExportingBufferType<MekanismGasBuffer, Accumulator, JeiBuffer, JeiAccumulator>,
        SidedBufferType<MekanismGasBuffer, Accumulator, JeiBuffer, JeiAccumulator> {

        val DEFAULT_UI_TANK: Positioned<TankDrawData> =
            Positioned(UiPosition.CENTER, TankDrawData(CbtGuiData.FLUID_SLOT, 1, 1, 16, 16))

        override val id: ResourceLocation = CbTweaker.resource("mekanism_gas")

        override val bufferClass: Class<MekanismGasBuffer>
            get() = MekanismGasBuffer::class.java
        override val accumulatorClass: Class<Accumulator>
            get() = Accumulator::class.java

        context(_: JsonPath)
        override fun loadBufferFactory(dto: TJson.Object): BufferFactory<MekanismGasBuffer, JeiBuffer> {
            val capacity = dto.useIntValue("capacity") {
                if (it < 0) throw SerializationException.withPath("Capacity must be positive!")
                return@useIntValue it
            }
            val config = Config(
                capacity,
                dto.expectInt("insert_rate"),
                dto.expectInt("extract_rate"),
                dto.useString("gas_filter") { lazy { GasRegistry.getGas(it) } },
                dto.expectBool("allow_auto_export") ?: false,
                dto.expectBool("allow_block_interaction") ?: true,
                dto.useObject("ui_tank") { TankDrawData.loadPositioned(it, DEFAULT_UI_TANK) } ?: DEFAULT_UI_TANK,
                dto.expectBool("allow_ui_interaction") ?: true
            )
            return object : BufferFactory<MekanismGasBuffer, JeiBuffer> {
                override fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): MekanismGasBuffer =
                    MekanismGasBuffer(config, world, pos, null, observer)

                override fun createJeiBuffer(): JeiBuffer = JeiBuffer(config)
            }
        }

        override fun attachCapabilities(target: CapabilityVisitor, buffer: MekanismGasBuffer) {
            target.visit(Capabilities.GAS_HANDLER_CAPABILITY, buffer.restrictedTank)
        }

        override fun getDefaultAutoExportState(buffer: MekanismGasBuffer): Boolean? =
            if (buffer.config.allowAutoExport) false else null

        @ServerSide
        override fun handleAutoExport(
            buffer: MekanismGasBuffer,
            front: BlockSide,
            faces: Set<RelativeFace>,
            ticker: TickModulator
        ) {
            if (faces.isEmpty()) {
                ticker.increaseIntervalUntil(20, 60)
                return
            }
            val storedGas = buffer.gas
            if (storedGas == null || storedGas.amount <= 0) {
                ticker.increaseIntervalUntil(8, 60)
                return
            }
            val toSend = buffer.config.extractRate?.let {
                if (storedGas.amount <= it) storedGas else GasStack(storedGas.gas, it)
            } ?: storedGas
            val target = GasHandlerTarget(toSend)
            val world = buffer.world
            val pos = buffer.bufPos
            faces.forEach {
                val absFace = it.getFace(front)
                val te = world.getTileEntity(pos.offset(absFace)) ?: return@forEach
                val destFace = absFace.opposite
                val dest = te.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, destFace) ?: return@forEach
                target.addHandler(destFace, dest)
            }
            val handlerCount = target.getHandlers().size
            if (handlerCount <= 0) {
                ticker.increaseIntervalUntil(8, 60)
                return
            }
            val transferred = EmitUtils.sendToAcceptors(setOf(target), handlerCount, toSend.amount, toSend)
            if (transferred <= 0) {
                ticker.increaseIntervalUntil(8, 60)
                return
            }
            buffer.drawGas(null, transferred, true)
            ticker.interval = 8
        }

        override fun handleInteraction(
            state: MekanismGasBuffer, blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
            face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
        ): Boolean {
            if (!state.config.allowBlockInteraction) return false
            val stack = player.getHeldItem(hand)
            if (stack.isEmpty) return false
            val item = stack.item
            if (item !is IGasItem) return false
            var stackGas = item.getGas(stack)

            // try to insert
            if (stackGas != null && stackGas.amount > 0 && state.canReceiveGas(face, stackGas.gas)) {
                stackGas = item.removeGas(stack, state.maxGas - state.stored)
                if (stackGas != null && stackGas.amount > 0) {
                    stackGas.amount -= state.receiveGas(face, stackGas, true)
                    if (stackGas.amount > 0) {
                        item.addGas(stack, stackGas)
                    }
                    return true
                }
            }

            // try to extract
            if (stackGas != null) {
                if (state.canDrawGas(face, stackGas.gas)) {
                    val available = state.drawGas(face, item.getMaxGas(stack) - stackGas.amount, false)
                    val transferred = item.addGas(stack, available)
                    if (transferred > 0) {
                        state.drawGas(face, transferred, true)
                        return true
                    }
                }
            } else {
                val available = state.drawGas(face, item.getMaxGas(stack), false)
                if (available != null && available.amount > 0) {
                    val transferred = item.addGas(stack, available)
                    if (transferred > 0) {
                        state.drawGas(face, transferred, true)
                        return true
                    }
                }
            }
            return false
        }

        override fun createAccumulator(): Accumulator = Accumulator()

        override fun accumulate(acc: Accumulator, buffer: MekanismGasBuffer) {
            acc.accumulate(buffer)
        }

        override fun copyAccumulator(acc: Accumulator): Accumulator = acc.copy()

        @ServerSide
        override fun serializeBufferToNbt(buffer: MekanismGasBuffer, dto: NBTTagCompound) {
            buffer.writeToNbt(dto)
        }

        @ServerSide
        override fun deserializeBufferFromNbt(buffer: MekanismGasBuffer, dto: NBTTagCompound) {
            buffer.readFromNbt(dto)
        }

        override fun createUiElement(buffer: MekanismGasBuffer): UiElement = buffer.createUiElement()

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

    class GasMatcher(private val gas: Gas, private val amount: Int, private val doConsume: Boolean) :
        IngredientMatcher<Accumulator, JeiAccumulator> {

        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean {
            val scaledAmount = CbtMathHelper.scaleConsumeInt(amount, consumeFactor, checkMode)
            if (scaledAmount <= 0) return true
            val drained = acc.value.getTank(gas).drawGas(null, scaledAmount, doConsume)
            return drained != null && drained.amount >= scaledAmount
        }

        private val jeiIngredient: JeiMekanismGasIngredient =
            JeiMekanismGasIngredient(GasStack(gas, amount), false, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("gas")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): GasMatcher = GasMatcher(
                dto.useStringValue("gas") { GasHelper.loadGas(it) },
                dto.expectIntValue("amount"),
                dto.expectBool("consume") ?: true
            )
        }
    }

    class GasRateMatcher(private val gas: Gas, private val rate: Int) :
        IngredientMatcher<Accumulator, JeiAccumulator> {

        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            !checkMode || consume(acc, consumeFactor, true) // make sure there's enough gas to start

        override fun consumePeriodic(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            consume(acc, consumeFactor, checkMode)

        private fun consume(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean {
            val scaledAmount = CbtMathHelper.scaleConsumeInt(rate, consumeFactor, checkMode)
            if (scaledAmount <= 0) return true
            val drained = acc.value.getTank(gas).drawGas(null, scaledAmount, true)
            return drained != null && drained.amount >= scaledAmount
        }

        private val jeiIngredient: JeiMekanismGasIngredient =
            JeiMekanismGasIngredient(GasStack(gas, rate), true, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("gas_rate")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): GasRateMatcher =
                GasRateMatcher(dto.useStringValue("gas") { GasHelper.loadGas(it) }, dto.expectIntValue("rate"))
        }
    }

    class GasProvider(private val gas: Gas, private val amount: Int, private val chance: Float) :
        IngredientProvider<Accumulator, JeiAccumulator> {

        override fun insertFinal(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean =
            !CbtMathHelper.rollProduce(chance, checkMode) || acc.value.insert(GasStack(gas, amount), true) >= amount

        private val jeiIngredient: JeiMekanismGasIngredient =
            JeiMekanismGasIngredient(GasStack(gas, amount), false, JeiIngredient.Role.OUTPUT, chance)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("gas")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): GasProvider = GasProvider(
                dto.useStringValue("gas") { GasHelper.loadGas(it) },
                dto.expectIntValue("amount"),
                dto.expectFloat("chance") ?: 1F
            )
        }
    }

    class GasRateProvider(private val gas: Gas, private val rate: Int) :
        IngredientProvider<Accumulator, JeiAccumulator> {

        override fun insertPeriodic(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean {
            acc.value.insert(GasStack(gas, rate), true)
            return true
        }

        private val jeiIngredient: JeiMekanismGasIngredient =
            JeiMekanismGasIngredient(GasStack(gas, rate), true, JeiIngredient.Role.OUTPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("gas_rate")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): GasRateProvider =
                GasRateProvider(dto.useStringValue("gas") { GasHelper.loadGas(it) }, dto.expectIntValue("rate"))
        }
    }
}
