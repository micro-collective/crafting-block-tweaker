package st.evening.mc.cbtweaker.util.machine

import gnu.trove.impl.Constants
import gnu.trove.iterator.TObjectIntIterator
import gnu.trove.map.TObjectIntMap
import gnu.trove.map.hash.TObjectIntHashMap
import st.evening.mc.cbtweaker.util.TObjectIntEmptyIterator
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectStringValue
import st.evening.mc.prelude.api.data.tjson.forEachValue

interface ComponentSet {
    fun getCount(componentId: String): Int

    fun iterator(): TObjectIntIterator<String>

    object Empty : ComponentSet {
        override fun getCount(componentId: String): Int = 0

        override fun iterator(): TObjectIntIterator<String> = TObjectIntEmptyIterator()
    }

    companion object {
        context(_: JsonPath)
        fun load(dto: TJson): ComponentSet {
            val components = MutableComponentSet()
            if (dto is TJson.Array) {
                dto.forEachValue {
                    if (!loadComponent(components, it)) {
                        throw SerializationException.withPath("Expected a component ID or a component object!")
                    }
                }
            } else if (!loadComponent(components, dto)) {
                throw SerializationException.withPath(
                    "Expected a component ID, a component object, or an array of components!"
                )
            }
            return components
        }

        context(_: JsonPath)
        private fun loadComponent(dest: MutableComponentSet, dto: TJson): Boolean {
            when (dto) {
                is TJson.String -> {
                    dest.put(dto.value, 1)
                    return true
                }
                is TJson.Object -> {
                    dest.put(dto.expectStringValue("id"), dto.expectInt("count") ?: 1)
                    return true
                }
                else -> return false
            }
        }
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
        components.adjustOrPutValue(componentId, 1, 1)
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
