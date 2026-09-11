package st.evening.mc.cbtweaker.util

import net.minecraft.block.SoundType
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.item.EnumRarity
import net.minecraft.util.BlockRenderLayer
import st.evening.mc.prelude.api.data.ser.EnumSerializer
import java.lang.reflect.Modifier

object BlockConfigData {
    val materials: Map<String, Material> = scrape(Material::class.java)
    val soundTypes: Map<String, SoundType> = scrape(SoundType::class.java)
    val mapColours: Map<String, MapColor> = scrape(MapColor::class.java)

    val renderLayerSerializer: EnumSerializer<BlockRenderLayer> = EnumSerializer()
    val raritySerializer: EnumSerializer<EnumRarity> = EnumSerializer()

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
