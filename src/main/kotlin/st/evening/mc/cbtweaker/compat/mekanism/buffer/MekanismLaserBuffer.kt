package st.evening.mc.cbtweaker.compat.mekanism.buffer

import mekanism.api.lasers.ILaserReceptor
import mekanism.common.capabilities.Capabilities
import mezz.jei.api.IGuiHelper
import mezz.jei.api.IJeiHelpers
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.BufferFactory
import st.evening.mc.cbtweaker.buffer.BufferObserver
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcher
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcherType
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.cbtweaker.compat.mekanism.jei.JeiMekanismJoulesIngredient
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.gui.element.BarControl
import st.evening.mc.cbtweaker.gui.inventory.SyncedUiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.util.gui.DrawableData
import st.evening.mc.cbtweaker.util.gui.SamplableData
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.prelude.api.data.ser.DoubleSerializer
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectDoubleValue
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useDoubleValue
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
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation

@RequireMod(MekanismCompat.MOD_ID)
class MekanismLaserBuffer(
    val config: Config,
    val world: World,
    val bufPos: BlockPos,
    energyStored: Double,
    private val observer: BufferObserver?
) : ILaserReceptor, NbtCompoundSerializable {
    companion object {
        private const val SER_ENERGY: String = "energy"
    }

    private val energyState: ValueStateAtom<Double> = ValueStateAtom(energyStored, DoubleSerializer)

    var energyStored: Double
        get() = energyState.value
        set(value) {
            if (energyState.update(value)) {
                observer?.onIngredientsChanged()
            }
        }

    fun insert(amount: Double, commit: Boolean): Double {
        if (amount <= 0.0) return 0.0
        val energy = energyStored
        val toTransfer = amount.coerceAtMost(config.capacity - energy)
        if (toTransfer <= 0.0) return 0.0
        if (commit) {
            energyStored = energy + toTransfer
        }
        return toTransfer
    }

    fun extract(amount: Double, commit: Boolean): Double {
        if (amount <= 0.0) return 0.0
        val energy = energyStored
        if (energy <= 0.0) return 0.0
        val toTransfer = energy.coerceAtMost(amount)
        if (commit) {
            energyStored = energy - toTransfer
        }
        return toTransfer
    }

    override fun receiveLaserEnergy(energy: Double, face: EnumFacing) {
        energyStored = (energyStored + energy).coerceAtMost(config.capacity)
    }

    override fun canLasersDig(): Boolean = false

    fun copy(observer: BufferObserver?): MekanismLaserBuffer =
        MekanismLaserBuffer(config, world, bufPos, energyStored, observer)

    override fun writeToNbt(dto: NBTTagCompound) {
        dto.runAction {
            SER_ENERGY double energyState.value
        }
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        energyState.setValueFromSync(dto.getDouble(SER_ENERGY))
    }

    private fun createUiElement(): UiElement = UiElementImpl()

    private inner class UiElementImpl : SyncedUiElement {
        override val syncData: Piecewise
            get() = energyState

        @ClientSide.Strong
        override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
            config.uiPosition.placeElement(
                uiIndex, layout, wrapper,
                BarControl.DoubleTank(
                    config.barBg.drawable,
                    config.barFg.drawable,
                    { energyStored },
                    config.capacity,
                    "J",
                    config.barOrientation,
                    config.barOffsetX,
                    config.barOffsetY
                )
            )
        }
    }

    class Config(
        val capacity: Double,
        val uiPosition: UiPosition,
        val barBg: DrawableData,
        val barFg: SamplableData,
        val barOffsetX: Int,
        val barOffsetY: Int,
        val barOrientation: DrawOrientation
    )

    class Accumulator {
        private val buffers: MutableList<MekanismLaserBuffer> = mutableListOf()

        fun accumulate(buffer: MekanismLaserBuffer) {
            buffers.add(buffer)
        }

        fun getStored(): Double = buffers.sumOf { it.energyStored }

        fun getCapacity(): Double = buffers.sumOf { it.config.capacity }

        fun insert(amount: Double, commit: Boolean): Double {
            var remaining = amount
            buffers.forEach {
                remaining -= it.insert(remaining, commit)
                if (remaining <= 0) return amount
            }
            return amount - remaining
        }

        fun extract(amount: Double, commit: Boolean): Double {
            var remaining = amount
            buffers.forEach {
                remaining -= it.extract(remaining, commit)
                if (remaining <= 0) return amount
            }
            return amount - remaining
        }

        fun copy(): Accumulator {
            val acc = Accumulator()
            buffers.forEach {
                acc.accumulate(it.copy(null))
            }
            return acc
        }
    }

    class JeiBuffer(private val config: Config) {
        private var contents: JeiIngredient<Double>? = null

        fun isEmpty(): Boolean = contents == null

        fun setContents(ingredient: JeiIngredient<Double>) {
            contents = ingredient
        }

        @ClientSide.Physical
        fun createJeiUiElement(contRegion: IntRectangle, guiHelper: IGuiHelper): JeiUiElement<*> {
            val barBg = config.barBg.drawable
            val barFg = config.barFg.drawable
            val pos = config.uiPosition.computePosition(contRegion, barBg.width, barBg.height)
            val barRegion = Rect2i(pos.x + config.barOffsetX, pos.y + config.barOffsetY, barFg.width, barFg.height)
            val ticker = guiHelper.createTickTimer(32, 32, false)
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

    object Type : BufferType<MekanismLaserBuffer, Accumulator, JeiBuffer, JeiAccumulator> {
        override val id: ResourceLocation = CbTweaker.resource("mekanism_laser")

        override val bufferClass: Class<MekanismLaserBuffer>
            get() = MekanismLaserBuffer::class.java
        override val accumulatorClass: Class<Accumulator>
            get() = Accumulator::class.java

        context(_: JsonPath)
        override fun loadBufferFactory(dto: TJson.Object): BufferFactory<MekanismLaserBuffer, JeiBuffer> {
            val capacity = dto.useDoubleValue("capacity") {
                if (it < 0.0) throw SerializationException.withPath("Capacity must be positive!")
                return@useDoubleValue it
            }
            val config = Config(
                capacity,
                dto.useAny("ui_position") { UiPosition.load(it) } ?: UiPosition.CENTER,
                dto.useAny("bar_bg") { DrawableData.loadSliceOrBlank(it, 6, 36) } ?: CbtGuiData.MEKANISM_ENERGY_BAR_BG,
                dto.useAny("bar_fg") { DrawableData.loadSliceOrBlank(it, 4, 34) } ?: CbtGuiData.MEKANISM_ENERGY_BAR_FG,
                dto.expectInt("bar_offset_x") ?: 1,
                dto.expectInt("bar_offset_y") ?: 1,
                dto.useString("bar_orientation") { DrawOrientation.serializer.deserializeFromJson(it) }
                    ?: DrawOrientation.BOTTOM_TO_TOP
            )
            return object : BufferFactory<MekanismLaserBuffer, JeiBuffer> {
                override fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): MekanismLaserBuffer =
                    MekanismLaserBuffer(config, world, pos, 0.0, observer)

                override fun createJeiBuffer(): JeiBuffer = JeiBuffer(config)
            }
        }

        override fun attachCapabilities(target: CapabilityVisitor, buffer: MekanismLaserBuffer) {
            target.visit(Capabilities.LASER_RECEPTOR_CAPABILITY, buffer)
        }

        override fun createAccumulator(): Accumulator = Accumulator()

        override fun accumulate(acc: Accumulator, buffer: MekanismLaserBuffer) {
            acc.accumulate(buffer)
        }

        override fun copyAccumulator(acc: Accumulator): Accumulator = acc.copy()

        @ServerSide
        override fun serializeBufferToNbt(buffer: MekanismLaserBuffer, dto: NBTTagCompound) {
            buffer.writeToNbt(dto)
        }

        @ServerSide
        override fun deserializeBufferFromNbt(buffer: MekanismLaserBuffer, dto: NBTTagCompound) {
            buffer.readFromNbt(dto)
        }

        override fun createUiElement(buffer: MekanismLaserBuffer): UiElement = buffer.createUiElement()

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

    class EnergyMatcher(private val amount: Double, private val doConsume: Boolean) :
        IngredientMatcher<Accumulator, JeiAccumulator> {

        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, determMode: Boolean): Boolean {
            val scaledAmount = amount * consumeFactor
            return scaledAmount <= 0.0 || acc.value.extract(scaledAmount, !doConsume) >= scaledAmount
        }

        private val jeiIngredient: JeiMekanismJoulesIngredient =
            JeiMekanismJoulesIngredient(amount, false, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("energy")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): EnergyMatcher =
                EnergyMatcher(dto.expectDoubleValue("amount"), dto.expectBool("consume") ?: true)
        }
    }

    class PowerMatcher(private val rate: Double) : IngredientMatcher<Accumulator, JeiAccumulator> {
        override fun consumeInitial(acc: Lazy<Accumulator>, consumeFactor: Float, determMode: Boolean): Boolean =
            consume(acc, consumeFactor, false) // make sure there's enough energy to start

        override fun consumePeriodic(acc: Lazy<Accumulator>, consumeFactor: Float): Boolean =
            consume(acc, consumeFactor, true)

        private fun consume(acc: Lazy<Accumulator>, consumeFactor: Float, commit: Boolean): Boolean {
            val scaledAmount = rate * consumeFactor
            return scaledAmount <= 0 || acc.value.extract(scaledAmount, commit) >= scaledAmount
        }

        private val jeiIngredient: JeiMekanismJoulesIngredient =
            JeiMekanismJoulesIngredient(rate, true, JeiIngredient.Role.INPUT)

        override fun getJeiIngredients(): Collection<JeiIngredient<*>> = listOf(jeiIngredient)

        override fun populateJei(acc: JeiAccumulator): Boolean = acc.addIngredient(jeiIngredient)

        object Type : IngredientMatcherType<Accumulator, JeiAccumulator> {
            override val id: ResourceLocation = CbTweaker.resource("power")

            context(_: JsonPath)
            override fun loadMatcher(dto: TJson.Object): PowerMatcher = PowerMatcher(dto.expectDoubleValue("rate"))
        }
    }
}
