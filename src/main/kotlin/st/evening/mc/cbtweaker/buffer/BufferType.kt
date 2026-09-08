package st.evening.mc.cbtweaker.buffer

import mezz.jei.api.IJeiHelpers
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.common.BlockBehaviour
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.util.Identifiable
import st.evening.mc.cbtweaker.util.component.SidedBufferConfig
import st.evening.mc.cbtweaker.util.machine.MutableComponentSet
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.util.game.CapabilityVisitor
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace

interface BufferObserver {
    fun onIngredientsChanged()

    fun onComponentsChanged()

    object Noop : BufferObserver {
        override fun onIngredientsChanged() {}

        override fun onComponentsChanged() {}
    }
}

interface BufferFactory<B, JB> {
    fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): B

    fun createJeiBuffer(): JB
}

interface BufferType<B, A, JB, JA> : Identifiable, BlockBehaviour<B> {
    val bufferClass: Class<B>

    val accumulatorClass: Class<A>

    context(_: JsonPath)
    fun loadBufferFactory(dto: TJson.Object): BufferFactory<B, JB>

    fun attachCapabilities(target: CapabilityVisitor, buffer: B) {}

    fun collectComponents(components: MutableComponentSet, buffer: B) {}

    fun tick(buffer: B) {}

    fun createAccumulator(): A

    fun accumulate(acc: A, buffer: B)

    fun copyAccumulator(acc: A): A

    fun serializeBufferToNbt(buffer: B, dto: NBTTagCompound)

    fun deserializeBufferFromNbt(buffer: B, dto: NBTTagCompound)

    fun getBufferSyncState(buffer: B): Piecewise? = null

    fun createUiElement(buffer: B): UiElement?

    fun createJeiAccumulator(): JA

    fun jeiAccumulate(acc: JA, buffer: JB)

    @ClientSide.Physical
    fun createJeiUiElements(buffer: JB, contRegion: IntRectangle, jeiHelpers: IJeiHelpers): Collection<JeiUiElement<*>>
}

interface SidedBufferType<B, A, JB, JA> : BufferType<B, A, JB, JA> {
    fun isCapabilitySided(buffer: B): Boolean = true

    fun configureDefaultSides(sideConfig: SidedBufferConfig<B>) {}
}

interface AutoExportingBufferType<B, A, JB, JA> : BufferType<B, A, JB, JA> {
    fun getDefaultAutoExportState(buffer: B): Boolean?

    @ServerSide
    fun handleAutoExport(buffer: B, front: BlockSide, faces: Set<RelativeFace>, ticker: TickModulator)
}
