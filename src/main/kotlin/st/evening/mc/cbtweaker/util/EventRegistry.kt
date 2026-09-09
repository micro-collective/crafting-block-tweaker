package st.evening.mc.cbtweaker.util

import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.event.CbtRegistrationEvent

interface Identifiable {
    val id: ResourceLocation
}

abstract class EventRegistry<T : Identifiable, E>(private val objType: Class<T>, private val objName: String) {
    protected val entries: MutableMap<ResourceLocation, E> = mutableMapOf()

    internal open fun init() {
        CbTweaker.logger.info("Loading $objName registry...")
        MinecraftForge.EVENT_BUS.post(CbtRegistrationEvent(objType, ::registerObject))
        CbTweaker.logger.info("Loaded ${entries.size} $objName entries")
    }

    protected abstract fun createEntry(obj: T): E

    private fun registerObject(obj: T) {
        val objId = obj.id
        entries[objId]?.let {
            throw IllegalStateException(
                "Duplicate $objName registry entry! ID: $objId, " +
                    "existing: ${it.javaClass.canonicalName}, new: ${obj.javaClass.canonicalName}"
            )
        }
        entries[objId] = createEntry(obj)
        CbTweaker.logger.debug("Registered $objName {} ({})", objId, obj.javaClass.getCanonicalName())
    }

    operator fun get(key: ResourceLocation): E? = entries[key]

    class Simple<T : Identifiable>(objType: Class<T>, objName: String) :
        EventRegistry<T, T>(objType, objName) {
        companion object {
            inline operator fun <reified T : Identifiable> invoke(objName: String): Simple<T> =
                Simple(T::class.java, objName)
        }

        override fun createEntry(obj: T): T = obj
    }
}
