package st.evening.mc.cbtweaker.util

import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.event.CbtRegistrationEvent

interface Identifiable {
    val id: ResourceLocation
}

open class EventRegistry<T : Identifiable>(private val objType: Class<T>, private val objName: String) {
    companion object {
        inline operator fun <reified T : Identifiable> invoke(objName: String): EventRegistry<T> =
            EventRegistry(T::class.java, objName)
    }

    protected val entries: MutableMap<ResourceLocation, T> = mutableMapOf()

    internal open fun init() {
        CbTweaker.logger.info("Loading $objName registry...")
        MinecraftForge.EVENT_BUS.post(CbtRegistrationEvent(objType, ::registerObject))
        CbTweaker.logger.info("Loaded ${entries.size} $objName entries")
    }

    private fun registerObject(obj: T) {
        val objId = obj.id
        entries[objId]?.let {
            throw IllegalStateException(
                "Duplicate $objName registry entry! ID: $objId, " +
                    "existing: ${it.javaClass.canonicalName}, new: ${obj.javaClass.canonicalName}"
            )
        }
        entries[objId] = obj
        CbTweaker.logger.debug("Registered $objName {} ({})", objId, obj.javaClass.getCanonicalName())
    }

    operator fun get(key: ResourceLocation): T? = entries[key]
}
