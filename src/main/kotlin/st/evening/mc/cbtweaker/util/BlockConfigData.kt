package st.evening.mc.cbtweaker.util

import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import java.lang.reflect.Modifier

object BlockConfigData {
    val materials: Map<String, Material> = scrape(Material::class.java)
    val soundTypes: Map<String, SoundType> = scrape(SoundType::class.java)

    private fun <T> scrape(dataClass: Class<T>): Map<String, T> {
        val result = mutableMapOf<String, T>()
        dataClass.declaredFields.forEach {
            if (it.modifiers and Modifier.STATIC != 0 && dataClass.isAssignableFrom(it.type)) {
                @Suppress("UNCHECKED_CAST")
                result[it.name] = it.get(null) as T
            }
        }
        return result
    }
}
