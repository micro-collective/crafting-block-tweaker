package st.evening.mc.cbtweaker.util.recipe

import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.buffer.BufferGroups
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.prelude.api.type.Maybe
import st.evening.mc.prelude.api.type.orNull

interface LazyAccumulatorMap {
    operator fun contains(bufGroupId: String): Boolean

    operator fun get(bufGroupId: String): Group?

    fun copyAccumulators(): LazyAccumulatorMap

    interface Group {
        fun <A> getAccumulator(bufType: BufferType<*, A, *, *>): Lazy<A>
    }

    class Impl(private val bufGroups: BufferGroups) : LazyAccumulatorMap {
        private val groupCache: MutableMap<String, Maybe<BaseGroup>> = mutableMapOf()

        override fun contains(bufGroupId: String): Boolean = bufGroupId in bufGroups

        override fun get(bufGroupId: String): Group? {
            groupCache[bufGroupId]?.let { return it.orNull() }
            val bufGroup = bufGroups[bufGroupId]
            if (bufGroup == null) {
                groupCache[bufGroupId] = Maybe.Nothing()
                return null
            } else {
                val group = BaseGroup(bufGroup)
                groupCache[bufGroupId] = Maybe.Just(group)
                return group
            }
        }

        override fun copyAccumulators(): LazyAccumulatorMap = CopyingMap(this)

        private class BaseGroup(private val bufGroup: BufferGroup) : Group {
            private val accumCache: MutableMap<BufferType<*, *, *, *>, Lazy<*>> = mutableMapOf()

            override fun <A> getAccumulator(bufType: BufferType<*, A, *, *>): Lazy<A> {
                accumCache[bufType]?.let {
                    @Suppress("UNCHECKED_CAST")
                    return it as Lazy<A>
                }
                val accumulator = lazy { bufGroup.accumulateIngredients(bufType) }
                accumCache[bufType] = accumulator
                return accumulator
            }
        }

        private class CopyingMap(private val baseMap: LazyAccumulatorMap) : LazyAccumulatorMap {
            private val groupCache: MutableMap<String, Maybe<CopyingGroup>> = mutableMapOf()

            override fun contains(bufGroupId: String): Boolean = bufGroupId in baseMap

            override fun get(bufGroupId: String): Group? {
                groupCache[bufGroupId]?.let { return it.orNull() }
                val baseGroup = baseMap[bufGroupId]
                if (baseGroup == null) {
                    groupCache[bufGroupId] = Maybe.Nothing()
                    return null
                } else {
                    val group = CopyingGroup(baseGroup)
                    groupCache[bufGroupId] = Maybe.Just(group)
                    return group
                }
            }

            override fun copyAccumulators(): LazyAccumulatorMap {
                return CopyingMap(this)
            }

            private class CopyingGroup(private val baseGroup: Group) : Group {
                private val accumCache: MutableMap<BufferType<*, *, *, *>, Lazy<*>> = mutableMapOf()

                override fun <A> getAccumulator(bufType: BufferType<*, A, *, *>): Lazy<A> {
                    accumCache[bufType]?.let {
                        @Suppress("UNCHECKED_CAST")
                        return it as Lazy<A>
                    }
                    // THE COPY IS LAZY! IF THE ORIGINAL MAP IS MODIFIED, THE COPY'S STATE MAY BECOME INCONSISTENT!!
                    val accumulator = lazy { bufType.copyAccumulator(baseGroup.getAccumulator(bufType).value) }
                    accumCache[bufType] = accumulator
                    return accumulator
                }
            }
        }
    }
}
