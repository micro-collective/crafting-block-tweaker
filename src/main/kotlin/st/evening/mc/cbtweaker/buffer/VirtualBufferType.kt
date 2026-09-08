package st.evening.mc.cbtweaker.buffer

import mezz.jei.api.IJeiHelpers
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.util.game.CapabilityVisitor
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle

abstract class VirtualBufferType<B> : BufferType<B, Unit, Unit, Unit> {
    override val accumulatorClass: Class<Unit>
        get() = Unit::class.java

    context(_: JsonPath)
    override fun loadBufferFactory(dto: TJson.Object): BufferFactory<B, Unit> {
        val bufFactory = loadVirtualBufferFactory(dto)
        return object : BufferFactory<B, Unit> {
            override fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): B =
                bufFactory.createBuffer(world, pos, observer)

            override fun createJeiBuffer(): Unit = Unit
        }
    }

    context(_: JsonPath)
    protected abstract fun loadVirtualBufferFactory(dto: TJson.Object): VirtualBufferFactory<B>

    override fun attachCapabilities(target: CapabilityVisitor, buffer: B) {}

    override fun createAccumulator(): Unit = Unit

    override fun accumulate(acc: Unit, buffer: B) {}

    override fun copyAccumulator(acc: Unit): Unit = Unit

    override fun createJeiAccumulator(): Unit = Unit

    override fun jeiAccumulate(acc: Unit, buffer: Unit) {}

    @ClientSide.Physical
    override fun createJeiUiElements(
        buffer: Unit,
        contRegion: IntRectangle,
        jeiHelpers: IJeiHelpers
    ): Collection<JeiUiElement<*>> = emptyList()

    protected fun interface VirtualBufferFactory<B> {
        fun createBuffer(world: World, pos: BlockPos, observer: BufferObserver): B
    }
}
