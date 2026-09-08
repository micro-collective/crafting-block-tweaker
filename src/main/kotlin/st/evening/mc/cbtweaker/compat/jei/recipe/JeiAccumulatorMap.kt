package st.evening.mc.cbtweaker.compat.jei.recipe

import st.evening.mc.cbtweaker.buffer.BufferType

class JeiAccumulatorMap(bufGroups: Map<String, JeiBufferGroup>) {
    private val groupTable: Map<String, Group> = bufGroups.mapValues { Group(it.value) }

    operator fun contains(bufGroupId: String): Boolean = bufGroupId in groupTable

    operator fun get(bufGroupId: String): Group? = groupTable[bufGroupId]

    class Group(bufGroup: JeiBufferGroup) {
        private val accumTable: MutableMap<BufferType<*, *, *, *>, Any?> = mutableMapOf()

        init {
            bufGroup.forEach(object : JeiBufferGroup.Visitor {
                override fun <JB, JA> visit(bufType: BufferType<*, *, JB, JA>, buffers: Map<String, JB>) {
                    val acc = bufType.createJeiAccumulator()
                    buffers.values.forEach {
                        bufType.jeiAccumulate(acc, it)
                    }
                    accumTable[bufType] = acc
                }
            })
        }

        @Suppress("UNCHECKED_CAST")
        fun <JA> getAccumulator(bufType: BufferType<*, *, *, JA>): JA? = accumTable[bufType] as? JA
    }
}
