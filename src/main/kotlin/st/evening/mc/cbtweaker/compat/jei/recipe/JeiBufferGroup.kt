package st.evening.mc.cbtweaker.compat.jei.recipe

import mezz.jei.api.IJeiHelpers
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle

class JeiBufferGroup {
    private val bufferTable: MutableMap<BufferType<*, *, *, *>, MutableMap<String, *>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <JB> getBuffers(bufType: BufferType<*, *, JB, *>): Map<String, JB> =
        (bufferTable[bufType] as? Map<String, JB>) ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    fun <JB> getBuffer(bufType: BufferType<*, *, JB, *>, name: String): JB? = bufferTable[bufType]?.get(name) as? JB

    fun <JB> addBuffer(bufType: BufferType<*, *, JB, *>, name: String, buffer: JB) {
        val buffers = bufferTable[bufType]
        if (buffers != null) {
            @Suppress("UNCHECKED_CAST")
            (buffers as MutableMap<String, JB>)[name] = buffer
        } else {
            bufferTable[bufType] = mutableMapOf(name to buffer)
        }
    }

    fun forEach(visitor: Visitor) {
        bufferTable.forEach { (bufType, buffers) ->
            @Suppress("UNCHECKED_CAST")
            visitor.visit(bufType as BufferType<*, *, Any, *>, buffers as Map<String, Any>)
        }
    }

    interface Visitor {
        fun <JB, JA> visit(bufType: BufferType<*, *, JB, JA>, buffers: Map<String, JB>)
    }
}

@ClientSide.Physical
class JeiUiElementConstructVisitor(
    private val container: JeiUi,
    private val region: IntRectangle,
    private val jeiHelpers: IJeiHelpers
) : JeiBufferGroup.Visitor {
    override fun <JB, JA> visit(bufType: BufferType<*, *, JB, JA>, buffers: Map<String, JB>) {
        buffers.values.forEach { buffer ->
            bufType.createJeiUiElements(buffer, region, jeiHelpers).forEach {
                container.addJeiUiElement(it)
            }
        }
    }
}
