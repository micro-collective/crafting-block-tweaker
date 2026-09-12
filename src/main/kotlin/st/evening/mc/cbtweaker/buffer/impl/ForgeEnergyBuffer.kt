package st.evening.mc.cbtweaker.buffer.impl

import mezz.jei.api.IGuiHelper
import mezz.jei.api.IJeiHelpers
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.IEnergyStorage
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
import st.evening.mc.cbtweaker.compat.jei.ingredient.impl.JeiForgeEnergyIngredient
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.gui.element.BarControl
import st.evening.mc.cbtweaker.gui.inventory.SyncedUiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.util.CbtMathHelper
import st.evening.mc.cbtweaker.util.component.SidedBufferConfig
import st.evening.mc.cbtweaker.util.gui.DrawableData
import st.evening.mc.cbtweaker.util.gui.SamplableData
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.prelude.api.component.energy.ConcatEnergyStorage
import st.evening.mc.prelude.api.component.energy.RatedEnergyStorage
import st.evening.mc.prelude.api.data.ser.IntSerializer
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
import st.evening.mc.prelude.api.data.tjson.expectString
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useIntValue
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.drawable.drawFullSizeAsProgress
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.CapabilityVisitor
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.EnergyHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.math.ceilDivPos
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace

class ForgeEnergyBuffer(
    private val config: Config,
    val world: World,
    val bufPos: BlockPos,
    energyStored: Int,
    private val observer: BufferObserver?
) : IEnergyStorage, NbtCompoundSerializable {
    companion object {
        private const val DEFAULT_ENERGY_UNIT: String = "FE"
        private const val DEFAULT_POWER_UNIT: String = "$DEFAULT_ENERGY_UNIT/t"

        private const val SER_ENERGY: String = "energy"
    }

    val ratedStorage: IEnergyStorage = RatedEnergyStorage(this, config.insertRate, config.extractRate)

    private val energyState: ValueStateAtom<Int> = ValueStateAtom(energyStored, IntSerializer)

    fun setEnergyStored(newEnergy: Int) {
        if (energyState.update(newEnergy)) {
            observer?.onIngredientsChanged()
        }
    }

    override fun getEnergyStored(): Int = energyState.value

    override fun getMaxEnergyStored(): Int = config.capacity

    fun isEmpty(): Boolean = energyStored <= 0

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        if (maxReceive <= 0) return 0
        val energy = energyStored
        val toTransfer = maxReceive.coerceAtMost(config.capacity - energy)
        if (toTransfer <= 0) return 0
        if (!simulate) {
            energyStored = energy + toTransfer
        }
        return toTransfer
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        if (maxExtract <= 0) return 0
        val energy = energyStored
        if (energy <= 0) return 0
        val toTransfer = energy.coerceAtMost(maxExtract)
        if (!simulate) {
            energyStored = energy - toTransfer
        }
        return toTransfer
    }

    override fun canExtract(): Boolean = true

    override fun canReceive(): Boolean = true

    fun copy(observer: BufferObserver?): ForgeEnergyBuffer =
        ForgeEnergyBuffer(config, world, bufPos, energyStored, observer)

    override fun writeToNbt(dto: NBTTagCompound) {
        dto.runAction {
            SER_ENERGY int energyState.value
        }
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        energyState.setValueFromSync(dto.getInteger(SER_ENERGY))
    }

    private fun createUiElement(): UiElement = UiElementImpl()

    private inner class UiElementImpl : SyncedUiElement {
        override val syncData: Piecewise
            get() = energyState

        @ClientSide.Strong
        override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
            config.uiPosition.placeElement(
                uiIndex, layout, wrapper,
                BarControl.IntTank(
                    config.barBg.drawable,
                    config.barFg.drawable,
                    { energyStored },
                    config.capacity,
                    config.energyUnitName,
                    config.barOrientation,
                    config.barOffsetX,
                    config.barOffsetY
                )
            )
        }
    }

    class Config(
        val capacity: Int,
        val insertRate: Int,
        val extractRate: Int,
        val allowAutoExport: Boolean,
        val uiPosition: UiPosition,
        val barBg: DrawableData,
        val barFg: SamplableData,
        val barOffsetX: Int,
        val barOffsetY: Int,
        val barOrientation: DrawOrientation,
        val energyUnitName: String
    )

    class Accumulator {
        private val buffers: MutableList<ForgeEnergyBuffer> = mutableListOf()
        private val concatStorage: IEnergyStorage = ConcatEnergyStorage(buffers)

        fun accumulate(buffer: ForgeEnergyBuffer) {
            buffers += buffer
        }

        fun getStored(): Int = concatStorage.energyStored

        fun getCapacity(): Int = concatStorage.maxEnergyStored

        fun insert(maxReceive: Int, simulate: Boolean): Int = concatStorage.receiveEnergy(maxReceive, simulate)

        fun extract(maxExtract: Int, simulate: Boolean): Int = concatStorage.extractEnergy(maxExtract, simulate)

        fun copy(): Accumulator {
            val acc = Accumulator()
            buffers.forEach {
                acc.accumulate(it.copy(null))
            }
            return acc
        }
    }

    class JeiBuffer(private val config: Config) {
        private var contents: JeiIngredient<Int>? = null

        fun isEmpty(): Boolean = contents == null

        fun setContents(amount: Int, isRate: Boolean, role: JeiIngredient.Role) {
            contents = JeiForgeEnergyIngredient(
                amount,
                if (isRate) "${config.energyUnitName}/t" else config.energyUnitName,
                role
            )
        }

        @ClientSide.Physical
        fun createJeiUiElement(contRegion: IntRectangle, guiHelper: IGuiHelper): JeiUiElement<*> {
            val barBg = config.barBg.drawable
            val barFg = config.barFg.drawable
            val pos = config.uiPosition.computePosition(contRegion, barBg.width, barBg.height)
            val barRegion = Rect2i(pos.x + config.barOffsetX, pos.y + config.barOffsetY, barFg.width, barFg.height)
            val ticker = guiHelper.createTickTimer(32, 32, false)
            return object : JeiUiElement<Int> {
                override val jeiIngredient: JeiIngredient<Int>?
                    get() = contents

                override val ingredientRegion: IntRectangle
                    get() = barRegion

                override fun drawElement(ingredient: Int?, partialTicks: Float) {
                    config.barBg.drawable.drawFullSize(partialTicks, pos.x, pos.y)
                    if (ingredient != null) {
                        config.barFg.drawable.drawFullSizeAsProgress(
                            partialTicks,
                            barRegion.posX,
                            barRegion.posY,
                            config.barOrientation,
                            if (contents?.role == JeiIngredient.Role.OUTPUT) {
                                ticker.value / ticker.maxValue.toFloat()
                            } else {
                                1 - ticker.value / ticker.maxValue.toFloat()
                            }
                        )
                    }
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

        fun addIngredient(amount: Int, isRate: Boolean, role: JeiIngredient.Role): Boolean {
            while (buffers.isNotEmpty()) {
                val buffer = buffers.removeFirst()
                if (buffer.isEmpty()) {
                    buffer.setContents(amount, isRate, role)
                    return true
                }
            }
            return false
        }
    }

    object Type : AutoExportingBufferType<ForgeEnergyBuffer, Accumulator, JeiBuffer, JeiAccumulator>,
        SidedBufferType<ForgeEnergyBuffer, Accumulator, JeiBuffer, JeiAccumulator> {

        override val id: ResourceLocation = CbTweaker.resource("forge_energy")

        override val bufferClass: Class<ForgeEnergyBuffer>
            get() = ForgeEnergyBuffer::class.java
        override val accumulatorClass: Class<Accumulator>
            get() = Accumulator::class.java

        context(_: JsonPath)
        override fun loadBufferFactory(dto: TJson.Object): BufferFactory<ForgeEnergyBuffer, JeiBuffer> {
            val capacity = dto.useIntValue("capacity") {
                if (it < 0) throw SerializationException.withPath("Capacity must be positive!")
                return@useIntValue it
            }
            val extractRate = dto.expectInt("extract_rate") ?: 0
            val config = Config(
                capacity,
                dto.expectInt("insert_rate") ?: (capacity ceilDivPos 100),
                extractRate,
                dto.expectBool("allow_auto_export") ?: (extractRate > 0),
                dto.useAny("ui_position") { UiPosition.load(it) } ?: UiPosition.CENTER,
                dto.useAny("bar_bg") { DrawableData.loadSliceOrBlank(it, 6, 36) } ?: CbtGuiData.ENERGY_BAR_BG,
                dto.useAny("bar_fg") { DrawableData.loadSliceOrBlank(it, 4, 34) } ?: CbtGuiData.ENERGY_BAR_FG,
                dto.expectInt("bar_offset_x") ?: 1,
                dto.expectInt("bar_offset_y") ?: 1,
                dto.useString("bar_orientation") { DrawOrientation.serializer.deserializeFromJson(it) }
                    ?: DrawOrientation.BOTTOM_TO_TOP,
                dto.expectString("energy_unit_name") ?: DEFAULT_ENERGY_UNIT
            )
            return object : BufferFactory<ForgeEnergyBuffer, JeiBuffer> {
                override fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): ForgeEnergyBuffer =
                    ForgeEnergyBuffer(config, world, pos, 0, observer)

                override fun createJeiBuffer(): JeiBuffer = JeiBuffer(config)
            }
        }

        override fun configureDefaultSides(sideConfig: SidedBufferConfig<ForgeEnergyBuffer>) {
            sideConfig.setAllEnabled(true)
        }

        override fun attachCapabilities(target: CapabilityVisitor, buffer: ForgeEnergyBuffer) {
            target.visit(CapabilityEnergy.ENERGY, buffer.ratedStorage)
        }

        override fun getDefaultAutoExportState(buffer: ForgeEnergyBuffer): Boolean? =
            buffer.config.run { if (!allowAutoExport) null else extractRate > 0 }

        @ServerSide
        override fun handleAutoExport(
            buffer: ForgeEnergyBuffer,
            front: BlockSide,
            faces: Set<RelativeFace>,
            ticker: TickModulator
        ) {
            if (faces.isEmpty()) {
                ticker.interval = 32
                return
            }
            if (buffer.isEmpty()) {
                ticker.increaseIntervalUntil(2, 32)
                return
            }
            val world = buffer.world
            val pos = buffer.bufPos
            val receivers = faces.mapNotNull {
                val absFace = it.getFace(front)
                return@mapNotNull world.getTileEntity(pos.offset(absFace))
                    ?.getCapability(CapabilityEnergy.ENERGY, absFace.opposite)
            }
            if (receivers.isEmpty()) {
                ticker.interval = 32
                return
            }
            val transferred = EnergyHelper.distribute(buffer.extractEnergy(buffer.config.extractRate, true), receivers)
            if (transferred <= 0) {
                ticker.increaseIntervalUntil(2, 32)
                return
            }
            buffer.extractEnergy(transferred, false)
            ticker.interval = 1
        }

        override fun createAccumulator(): Accumulator = Accumulator()

        override fun accumulate(acc: Accumulator, buffer: ForgeEnergyBuffer) {
            acc.accumulate(buffer)
        }

        override fun copyAccumulator(acc: Accumulator): Accumulator = acc.copy()

        @ServerSide
        override fun serializeBufferToNbt(buffer: ForgeEnergyBuffer, dto: NBTTagCompound) {
            buffer.writeToNbt(dto)
        }

        @ServerSide
        override fun deserializeBufferFromNbt(buffer: ForgeEnergyBuffer, dto: NBTTagCompound) {
            buffer.readFromNbt(dto)
        }

        override fun createUiElement(buffer: ForgeEnergyBuffer): UiElement = buffer.createUiElement()

        override fun createJeiAccumulator(): JeiAccumulator = JeiAccumulator()

        override fun jeiAccumulate(acc: JeiAccumulator, buffer: JeiBuffer) {
            acc.accumulate(buffer)
        }

        @ClientSide.Physical
        override fun createJeiUiElements(
            buffer: JeiBuffer,
            contRegion: IntRectangle,
            jeiHelpers: IJeiHelpers
        ): Collection<JeiUiElement<*>> = listOf(buffer.createJeiUiElement(contRegion, jeiHelpers.guiHelper))
    }

    class EnergyMatcher(private val amount: Int, private val doConsume: Boolean) :
        IngredientMatcher<Accumulator, JeiAccumulator> {

        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean {
            val scaledAmount = CbtMathHelper.scaleConsumeInt(amount, consumeFactor, checkMode)
            return scaledAmount <= 0 || acc.value.extract(scaledAmount, !doConsume) >= scaledAmount
        }

        private val jeiIngredient: JeiForgeEnergyIngredient =
            JeiForgeEnergyIngredient(amount, DEFAULT_ENERGY_UNIT, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean =
            acc.addIngredient(amount, false, JeiIngredient.Role.INPUT)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("energy")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): EnergyMatcher =
                EnergyMatcher(dto.expectIntValue("amount"), dto.expectBool("consume") ?: true)
        }
    }

    class PowerMatcher(private val rate: Int) : IngredientMatcher<Accumulator, JeiAccumulator> {
        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            !checkMode || consume(acc, consumeFactor, true) // make sure there's enough energy to start

        override fun consumePeriodic(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            consume(acc, consumeFactor, checkMode)

        private fun consume(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean {
            val scaledAmount = CbtMathHelper.scaleConsumeInt(rate, consumeFactor, checkMode)
            return scaledAmount <= 0 || acc.value.extract(scaledAmount, true) >= scaledAmount
        }

        private val jeiIngredient: JeiForgeEnergyIngredient =
            JeiForgeEnergyIngredient(rate, DEFAULT_POWER_UNIT, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(rate, true, JeiIngredient.Role.INPUT)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("power")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): PowerMatcher = PowerMatcher(dto.expectIntValue("rate"))
        }
    }

    class EnergyProvider(private val amount: Int, private val chance: Float) :
        IngredientProvider<Accumulator, JeiAccumulator> {

        override fun insertFinal(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean =
            !CbtMathHelper.rollProduce(chance, checkMode) || acc.value.insert(amount, false) >= amount

        private val jeiIngredient: JeiForgeEnergyIngredient =
            JeiForgeEnergyIngredient(amount, DEFAULT_ENERGY_UNIT, JeiIngredient.Role.OUTPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean =
            acc.addIngredient(amount, false, JeiIngredient.Role.OUTPUT)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("energy")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): EnergyProvider =
                EnergyProvider(dto.expectIntValue("amount"), dto.expectFloat("chance") ?: 1F)
        }
    }

    class PowerProvider(private val rate: Int) : IngredientProvider<Accumulator, JeiAccumulator> {
        override fun insertPeriodic(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean {
            acc.value.insert(rate, false)
            return true
        }

        private val jeiIngredient: JeiForgeEnergyIngredient =
            JeiForgeEnergyIngredient(rate, DEFAULT_POWER_UNIT, JeiIngredient.Role.OUTPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean =
            acc.addIngredient(rate, true, JeiIngredient.Role.OUTPUT)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("power")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): PowerProvider = PowerProvider(dto.expectIntValue("rate"))
        }
    }
}
