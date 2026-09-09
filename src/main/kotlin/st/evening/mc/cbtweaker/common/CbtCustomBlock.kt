package st.evening.mc.cbtweaker.common

import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import st.evening.mc.cbtweaker.util.BlockConfigData
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectFloat
import st.evening.mc.prelude.api.data.tjson.forEachInt
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useString

class BlockConfig(
    val material: Material,
    val soundType: SoundType, // TODO custom sound types
    val hardness: Float,
    val resistance: Float,
    val harvestLevel: Map<String, Int>
) {
    companion object {
        val DEFAULT: BlockConfig = BlockConfig(
            Material.IRON,
            SoundType.METAL,
            hardness = 3F,
            resistance = 15F,
            mapOf("pickaxe" to 0)
        )

        context(_: JsonPath)
        fun load(dto: TJson.Object): BlockConfig = BlockConfig(
            dto.useString("material") {
                BlockConfigData.materials[it] ?: throw SerializationException.withPath("Unknown material: $it")
            } ?: DEFAULT.material,
            dto.useString("sound_type") {
                BlockConfigData.soundTypes[it] ?: throw SerializationException.withPath("Unknown sound type: $it")
            } ?: DEFAULT.soundType,
            dto.expectFloat("hardness") ?: DEFAULT.hardness,
            dto.expectFloat("resistance") ?: DEFAULT.resistance,
            dto.useObject("harvest_level") {
                buildMap {
                    it.forEachInt { toolClass, harvestLevel ->
                        put(toolClass, harvestLevel)
                    }
                }
            } ?: DEFAULT.harvestLevel
        )
    }
}

interface CustomBlockType {
    val blockConfig: BlockConfig
}

abstract class CbtCustomBlock(material: Material) : Block(material) {
    abstract val blockType: CustomBlockType

    open fun init() {
        val config = blockType.blockConfig
        setSoundType(config.soundType)
        setHardness(config.hardness)
        setResistance(config.resistance)
        config.harvestLevel.forEach { (toolClass, harvestLevel) ->
            setHarvestLevel(toolClass, harvestLevel)
        }
    }

    // TODO wrench disassembly support might be useful
}
