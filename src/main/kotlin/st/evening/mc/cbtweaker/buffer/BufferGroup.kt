package st.evening.mc.cbtweaker.buffer

import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMap
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiBufferGroup
import st.evening.mc.cbtweaker.util.machine.MutableComponentSet
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson

typealias BufferGroups = Object2ObjectSortedMap<String, BufferGroup>

class BufferGroup {
    // iteration order must be deterministic so UI elements are constructed in the same order on the server and client
    private val bufferTable: Object2ObjectSortedMap<BufferType<*, *, *, *>, Object2ObjectSortedMap<String, *>> =
        Object2ObjectRBTreeMap(compareBy { it.id })

    @Suppress("UNCHECKED_CAST")
    fun <B> getBuffers(bufType: BufferType<B, *, *, *>): Map<String, B> =
        bufferTable[bufType] as? Map<String, B> ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    fun <B> getBuffer(bufType: BufferType<B, *, *, *>, name: String): B? = bufferTable[bufType]?.get(name) as? B

    fun <B, A> accumulateIngredients(bufType: BufferType<B, A, *, *>): A {
        val acc = bufType.createAccumulator()
        getBuffers(bufType).values.forEach {
            bufType.accumulate(acc, it)
        }
        return acc
    }

    fun <B> addBuffer(bufType: BufferType<B, *, *, *>, name: String, buffer: B) {
        val buffers = bufferTable[bufType]
        if (buffers != null) {
            @Suppress("UNCHECKED_CAST")
            (buffers as MutableMap<String, B>)[name] = buffer
        } else {
            val newBuffers = Object2ObjectRBTreeMap<String, Any>()
            newBuffers[name] = buffer
            bufferTable[bufType] = newBuffers
        }
    }

    fun forEach(visitor: Visitor) {
        bufferTable.forEach { (bufType, buffers) ->
            @Suppress("UNCHECKED_CAST")
            visitor.visit(bufType as BufferType<Any, *, *, *>, buffers as Object2ObjectSortedMap<String, Any>)
        }
    }

    interface Visitor {
        fun <B, A> visit(bufType: BufferType<B, A, *, *>, buffers: Object2ObjectSortedMap<String, B>)
    }

    class Factory {
        private val factoryTable: MutableMap<BufferType<*, *, *, *>, MutableMap<String, out BufferFactory<*, *>>> =
            mutableMapOf()

        fun <B> addFactory(bufType: BufferType<B, *, *, *>, name: String, factory: BufferFactory<B, *>) {
            val factories = factoryTable[bufType]
            if (factories != null) {
                @Suppress("UNCHECKED_CAST")
                (factories as MutableMap<String, BufferFactory<B, *>>)[name] = factory
            } else {
                factoryTable[bufType] = mutableMapOf(name to factory)
            }
        }

        context(_: JsonPath)
        fun <B> loadFactory(bufType: BufferType<B, *, *, *>, name: String, dto: TJson.Object) {
            addFactory(bufType, name, bufType.loadBufferFactory(dto))
        }

        fun createBufferGroup(world: World, pos: BlockPos, observer: BufferObserver): BufferGroup {
            val bufGroup = BufferGroup()
            factoryTable.forEach { (bufType, factories) ->
                factories.forEach { (name, factory) ->
                    @Suppress("UNCHECKED_CAST")
                    bufGroup.addBuffer(
                        bufType as BufferType<Any, *, *, *>,
                        name,
                        (factory as BufferFactory<Any, *>).createBuffer(world, pos, observer)
                    )
                }
            }
            return bufGroup
        }

        fun createJeiBufferGroup(): JeiBufferGroup {
            val bufGroup = JeiBufferGroup()
            factoryTable.forEach { (bufType, factories) ->
                factories.forEach { (name, factory) ->
                    @Suppress("UNCHECKED_CAST")
                    bufGroup.addBuffer(
                        bufType as BufferType<*, *, Any, *>,
                        name,
                        (factory as BufferFactory<*, Any>).createJeiBuffer()
                    )
                }
            }
            return bufGroup
        }
    }
}

class ComponentCollectVisitor(private val dest: MutableComponentSet) : BufferGroup.Visitor {
    override fun <B, A> visit(bufType: BufferType<B, A, *, *>, buffers: Object2ObjectSortedMap<String, B>) {
        buffers.values.forEach {
            bufType.collectComponents(dest, it)
        }
    }
}

fun Iterable<BufferGroup>.collectComponents(dest: MutableComponentSet) {
    val visitor = ComponentCollectVisitor(dest)
    forEach { it.forEach(visitor) }
}
