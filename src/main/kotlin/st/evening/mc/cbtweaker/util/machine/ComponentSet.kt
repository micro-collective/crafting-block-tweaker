package st.evening.mc.cbtweaker.util.machine

import gnu.trove.impl.Constants
import gnu.trove.iterator.TObjectIntIterator
import gnu.trove.map.TObjectIntMap
import gnu.trove.map.hash.TObjectIntHashMap
import st.evening.mc.cbtweaker.util.TObjectIntEmptyIterator

interface ComponentSet {
    fun getCount(componentId: String): Int

    fun iterator(): TObjectIntIterator<String>

    object Empty : ComponentSet {
        override fun getCount(componentId: String): Int = 0

        override fun iterator(): TObjectIntIterator<String> = TObjectIntEmptyIterator()
    }
}

class MutableComponentSet : ComponentSet {
    companion object {
        fun copyOf(o: ComponentSet): MutableComponentSet = MutableComponentSet().also { it.addAll(o) }
    }

    private val components: TObjectIntMap<String> = TObjectIntHashMap(
        Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, 0
    )

    fun put(componentId: String, count: Int) {
        if (count > 0) {
            components.adjustOrPutValue(componentId, count, count)
        }
    }

    fun put(componentId: String) {
        put(componentId, 1)
    }

    fun addAll(o: ComponentSet) {
        val iter = o.iterator()
        while (iter.hasNext()) {
            iter.advance()
            put(iter.key(), iter.value())
        }
    }

    override fun getCount(componentId: String): Int = components.get(componentId)

    override fun iterator(): TObjectIntIterator<String> = components.iterator()
}
