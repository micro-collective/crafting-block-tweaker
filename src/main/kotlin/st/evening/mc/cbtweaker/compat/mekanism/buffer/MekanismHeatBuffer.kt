package st.evening.mc.cbtweaker.compat.mekanism.buffer

import mekanism.api.IHeatTransfer
import mekanism.common.capabilities.Capabilities
import mekanism.common.util.HeatUtils
import mezz.jei.api.IJeiHelpers
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
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
import st.evening.mc.cbtweaker.compat.mekanism.gui.HeatBarControl
import st.evening.mc.cbtweaker.compat.mekanism.jei.JeiMekanismHeatIngredient
import st.evening.mc.cbtweaker.compat.mekanism.jei.JeiMekanismTemperatureIngredient
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.gui.inventory.SyncedUiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.util.CbtMathHelper
import st.evening.mc.cbtweaker.util.machine.StatMetric
import st.evening.mc.cbtweaker.util.component.SidedBufferConfig
import st.evening.mc.cbtweaker.util.gui.DrawableData
import st.evening.mc.cbtweaker.util.gui.SamplableData
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.prelude.api.data.ser.DoubleSerializer
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectDouble
import st.evening.mc.prelude.api.data.tjson.expectDoubleValue
import st.evening.mc.prelude.api.data.tjson.expectFloat
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.drawable.drawFullSizeAsProgress
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.CapabilityVisitor
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.RequireMod
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.math.DoubleArithmetic
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation

@RequireMod(MekanismCompat.MOD_ID)
class MekanismHeatBuffer(
    val config: Config,
    private val world: World,
    private val bufPos: BlockPos,
    temp: Double,
    private val observer: BufferObserver?
) : IHeatTransfer, NbtCompoundSerializable {
    companion object {
        private const val SER_TEMP: String = "temp"
    }

    private val tempState: ValueStateAtom<Double> = ValueStateAtom(temp, DoubleSerializer)
    private var bufferedHeat: Double = 0.0

    override fun getTemp(): Double = tempState.value

    fun setTemp(newTemp: Double) {
        if (tempState.update(newTemp)) {
            observer?.onIngredientsChanged()
        }
    }

    fun getHeat(): Double = temp * config.heatCapacity

    override fun getInverseConductionCoefficient(): Double = config.invConductanceCoeff

    override fun getInsulationCoefficient(face: EnumFacing): Double = config.insulationCoeff

    override fun transferHeatTo(heat: Double) {
        bufferedHeat += heat
    }

    override fun simulateHeat(): DoubleArray = HeatUtils.simulate(this)

    override fun applyTemperatureChange(): Double {
        val newTemp = (temp + bufferedHeat / config.heatCapacity).coerceAtLeast(0.0)
        temp = newTemp
        bufferedHeat = 0.0
        return newTemp
    }

    override fun canConnectHeat(face: EnumFacing): Boolean = true

    override fun getAdjacent(face: EnumFacing): IHeatTransfer? {
        if (!config.spreadHeat) return null
        val te = world.getTileEntity(bufPos.offset(face)) ?: return null
        return te.getCapability(Capabilities.HEAT_TRANSFER_CAPABILITY, face.opposite)
    }

    fun copy(observer: BufferObserver?): MekanismHeatBuffer = MekanismHeatBuffer(config, world, bufPos, temp, observer)

    fun tick() {
        if (world.isRemote) return
        simulateHeat()
        applyTemperatureChange()
    }

    override fun writeToNbt(dto: NBTTagCompound) {
        dto.runAction {
            SER_TEMP double tempState.value
        }
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        tempState.setValueFromSync(dto.getDouble(SER_TEMP))
    }

    private fun createUiElement(): UiElement = UiElementImpl()

    private inner class UiElementImpl : SyncedUiElement {
        override val syncData: Piecewise
            get() = tempState

        @ClientSide.Strong
        override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
            config.uiPosition.placeElement(
                uiIndex, layout, wrapper,
                HeatBarControl(
                    config.barBg.drawable,
                    config.barFg.drawable,
                    { temp },
                    config.barMaxTemp,
                    config.barOrientation,
                    config.barOffsetX,
                    config.barOffsetY
                )
            )
        }
    }

    class Config(
        val heatCapacity: Double,
        val invConductanceCoeff: Double,
        val insulationCoeff: Double,
        val spreadHeat: Boolean,
        val uiPosition: UiPosition,
        val barBg: DrawableData,
        val barFg: SamplableData,
        val barOffsetX: Int,
        val barOffsetY: Int,
        val barOrientation: DrawOrientation,
        val barMaxTemp: Double
    )

    class Accumulator : Iterable<Double> {
        private val buffers: MutableList<MekanismHeatBuffer> = mutableListOf()

        fun accumulate(buffer: MekanismHeatBuffer) {
            buffers += buffer
        }

        fun getTemp(metric: StatMetric): Double = metric.compute(DoubleArithmetic, this, buffers.size) ?: 0.0

        fun getTotalHeat(): Double = buffers.sumOf { it.getHeat() }

        fun addHeat(amount: Double) {
            when (buffers.size) {
                0 -> {}
                1 -> buffers[0].transferHeatTo(amount)
                else -> {
                    // lowest-temperature buffers get more heat
                    val weights = DoubleArray(buffers.size) { 1000.0 / buffers[it].temp.coerceAtLeast(1.0) }
                    val totalWeight = weights.sum()
                    buffers.forEachIndexed { i, buffer ->
                        buffer.transferHeatTo(amount * weights[i] / totalWeight)
                    }
                }
            }
        }

        fun removeHeat(amount: Double) {
            when (buffers.size) {
                0 -> {}
                1 -> buffers[0].transferHeatTo(-amount)
                else -> {
                    // highest-energy buffers lose more heat
                    val weights = DoubleArray(buffers.size) { buffers[it].getHeat() }
                    val totalWeight = weights.sum()
                    buffers.forEachIndexed { i, buffer ->
                        buffer.transferHeatTo(amount * weights[i] / totalWeight)
                    }
                }
            }
        }

        fun copy(): Accumulator {
            val acc = Accumulator()
            buffers.forEach {
                acc.accumulate(it.copy(null))
            }
            return acc
        }

        override fun iterator(): Iterator<Double> = TemperatureIterator(buffers.iterator())

        private class TemperatureIterator(private val backing: Iterator<MekanismHeatBuffer>) : Iterator<Double> {
            override fun hasNext(): Boolean = backing.hasNext()

            override fun next(): Double = backing.next().temp
        }
    }

    class JeiBuffer(private val config: Config) {
        private var contents: JeiIngredient<Double>? = null

        fun isEmpty(): Boolean = contents == null

        fun setContents(ingredient: JeiIngredient<Double>) {
            contents = ingredient
        }

        @ClientSide.Physical
        fun createJeiUiElement(contRegion: IntRectangle): JeiUiElement<*> {
            val barBg = config.barBg.drawable
            val barFg = config.barFg.drawable
            val pos = config.uiPosition.computePosition(contRegion, barBg.width, barBg.height)
            val barRegion = Rect2i(pos.x + config.barOffsetX, pos.y + config.barOffsetY, barFg.width, barFg.height)
            return object : JeiUiElement<Double> {
                override val jeiIngredient: JeiIngredient<Double>?
                    get() = contents

                override val ingredientRegion: IntRectangle
                    get() = barRegion

                override fun drawElement(ingredient: Double?, partialTicks: Float) {
                    config.barBg.drawable.drawFullSize(partialTicks, pos.x, pos.y)
                    if (ingredient != null) {
                        config.barFg.drawable.drawFullSizeAsProgress(
                            partialTicks,
                            barRegion.posX,
                            barRegion.posY,
                            config.barOrientation,
                            (ingredient / config.barMaxTemp).toFloat()
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

        fun addIngredient(ingredient: JeiIngredient<Double>): Boolean {
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

    object Type : SidedBufferType<MekanismHeatBuffer, Accumulator, JeiBuffer, JeiAccumulator> {
        override val id: ResourceLocation = CbTweaker.resource("mekanism_heat")

        override val bufferClass: Class<MekanismHeatBuffer>
            get() = MekanismHeatBuffer::class.java
        override val accumulatorClass: Class<Accumulator>
            get() = Accumulator::class.java

        context(_: JsonPath)
        override fun loadBufferFactory(dto: TJson.Object): BufferFactory<MekanismHeatBuffer, JeiBuffer> {
            val config = Config(
                dto.expectDouble("heat_capacity") ?: 1.0,
                dto.expectDouble("inverse_conduction_coeff") ?: 1.0,
                dto.expectDouble("insulation_coeff") ?: 1.0,
                dto.expectBool("spread_heat") ?: true,
                dto.useAny("ui_position") { UiPosition.load(it) } ?: UiPosition.CENTER,
                dto.useAny("bar_bg") { DrawableData.loadSliceOrBlank(it, 6, 36) } ?: CbtGuiData.MEKANISM_HEAT_BAR_BG,
                dto.useAny("bar_fg") { DrawableData.loadSliceOrBlank(it, 4, 34) } ?: CbtGuiData.MEKANISM_HEAT_BAR_FG,
                dto.expectInt("bar_offset_x") ?: 1,
                dto.expectInt("bar_offset_y") ?: 1,
                dto.useString("bar_orientation") { DrawOrientation.serializer.deserializeFromJson(it) }
                    ?: DrawOrientation.BOTTOM_TO_TOP,
                dto.expectDouble("bar_max_temp") ?: 3000.0
            )
            return object : BufferFactory<MekanismHeatBuffer, JeiBuffer> {
                override fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): MekanismHeatBuffer =
                    MekanismHeatBuffer(config, world, pos, 0.0, observer)

                override fun createJeiBuffer(): JeiBuffer = JeiBuffer(config)
            }
        }

        override fun configureDefaultSides(sideConfig: SidedBufferConfig<MekanismHeatBuffer>) {
            sideConfig.setAllEnabled(true)
        }

        override fun attachCapabilities(target: CapabilityVisitor, buffer: MekanismHeatBuffer) {
            target.visit(Capabilities.HEAT_TRANSFER_CAPABILITY, buffer)
        }

        override fun tick(buffer: MekanismHeatBuffer) {
            buffer.tick()
        }

        override fun createAccumulator(): Accumulator = Accumulator()

        override fun accumulate(acc: Accumulator, buffer: MekanismHeatBuffer) {
            acc.accumulate(buffer)
        }

        override fun copyAccumulator(acc: Accumulator): Accumulator = acc.copy()

        override fun createUiElement(buffer: MekanismHeatBuffer): UiElement = buffer.createUiElement()

        @ServerSide
        override fun serializeBufferToNbt(buffer: MekanismHeatBuffer, dto: NBTTagCompound) {
            buffer.writeToNbt(dto)
        }

        @ServerSide
        override fun deserializeBufferFromNbt(buffer: MekanismHeatBuffer, dto: NBTTagCompound) {
            buffer.readFromNbt(dto)
        }

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

    class TemperatureMatcher(
        private val temp: Double,
        private val metric: StatMetric,
        private val initialOnly: Boolean
    ) : IngredientMatcher<Accumulator, JeiAccumulator> {
        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            checkTemp(acc)

        override fun consumePeriodic(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            initialOnly || checkTemp(acc)

        private fun checkTemp(acc: Lazy<Accumulator>): Boolean = acc.value.getTemp(metric) >= temp

        private val jeiIngredient: JeiMekanismTemperatureIngredient =
            JeiMekanismTemperatureIngredient(temp, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("temperature")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): TemperatureMatcher = TemperatureMatcher(
                dto.expectDoubleValue("amount"),
                dto.useString("metric") { StatMetric.serializer.deserializeFromJson(it) } ?: StatMetric.MAX,
                dto.expectBool("continuous")?.let { !it } ?: false
            )
        }
    }

    class HeatMatcher(private val amount: Double, private val doConsume: Boolean) :
        IngredientMatcher<Accumulator, JeiAccumulator> {

        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean {
            val scaledAmount = amount * consumeFactor
            if (scaledAmount <= 0) return true
            val accum = acc.value
            if (accum.getTotalHeat() < scaledAmount) return false
            if (doConsume) {
                accum.removeHeat(amount)
            }
            return true
        }

        private val jeiIngredient: JeiMekanismHeatIngredient =
            JeiMekanismHeatIngredient(amount, false, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("heat")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): HeatMatcher =
                HeatMatcher(dto.expectDoubleValue("amount"), dto.expectBool("consume") ?: true)
        }
    }

    class HeatRateMatcher(private val rate: Double) : IngredientMatcher<Accumulator, JeiAccumulator> {
        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            !checkMode || consume(acc, consumeFactor) // make sure there's enough heat to start

        override fun consumePeriodic(acc: Lazy<Accumulator>, consumeFactor: Float, checkMode: Boolean): Boolean =
            consume(acc, consumeFactor)

        private fun consume(acc: Lazy<Accumulator>, consumeFactor: Float): Boolean {
            val scaledAmount = rate * consumeFactor
            if (scaledAmount <= 0) return true
            val accum = acc.value
            if (accum.getTotalHeat() < scaledAmount) return false
            accum.removeHeat(scaledAmount)
            return true
        }

        private val jeiIngredient: JeiMekanismHeatIngredient =
            JeiMekanismHeatIngredient(rate, true, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("heat_rate")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): HeatRateMatcher =
                HeatRateMatcher(dto.expectDoubleValue("rate"))
        }
    }

    class HeatProvider(private val amount: Double, private val chance: Float) :
        IngredientProvider<Accumulator, JeiAccumulator> {

        override fun insertFinal(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean {
            if (CbtMathHelper.rollProduce(chance, checkMode)) {
                acc.value.addHeat(amount)
            }
            return true
        }

        private val jeiIngredient: JeiMekanismHeatIngredient =
            JeiMekanismHeatIngredient(amount, false, JeiIngredient.Role.OUTPUT, chance)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("heat")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): HeatProvider =
                HeatProvider(dto.expectDoubleValue("amount"), dto.expectFloat("chance") ?: 1F)
        }
    }

    class HeatRateProvider(private val rate: Double) : IngredientProvider<Accumulator, JeiAccumulator> {
        override fun insertPeriodic(acc: Lazy<Accumulator>, checkMode: Boolean): Boolean {
            acc.value.addHeat(rate)
            return true
        }

        private val jeiIngredient: JeiMekanismHeatIngredient =
            JeiMekanismHeatIngredient(rate, true, JeiIngredient.Role.OUTPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientProviderType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("heat_rate")

            context(_: JsonPath)
            override fun loadProvider(dto: TJson.Object): HeatRateProvider =
                HeatRateProvider(dto.expectDoubleValue("rate"))
        }
    }
}
