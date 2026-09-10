package st.evening.mc.cbtweaker.common

import cofh.api.block.IDismantleable
import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.Optional
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.compat.cofh.CoFHCoreCompat
import st.evening.mc.cbtweaker.util.BlockConfigData
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectFloat
import st.evening.mc.prelude.api.data.tjson.forEachInt
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.RequireMod

class BlockConfig(
    val material: Material,
    val soundType: SoundType, // TODO custom sound types
    val hardness: Float,
    val resistance: Float,
    val harvestLevel: Map<String, Int>,
    val canWrenchDismantle: Boolean
) {
    companion object {
        val DEFAULT: BlockConfig = BlockConfig(
            Material.IRON,
            SoundType.METAL,
            hardness = 3F,
            resistance = 15F,
            mapOf("pickaxe" to 0),
            true
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
            } ?: DEFAULT.harvestLevel,
            dto.expectBool("can_wrench_dismantle") ?: DEFAULT.canWrenchDismantle
        )
    }
}

interface CustomBlockType {
    val blockConfig: BlockConfig
}

@Optional.Interface(iface = "cofh.api.block.IDismantleable", modid = CoFHCoreCompat.MOD_ID)
abstract class CbtCustomBlock(material: Material) : Block(material), IDismantleable {
    abstract val blockType: CustomBlockType

    open fun init() {
        val config = blockType.blockConfig
        setSoundType(config.soundType)
        setHardness(config.hardness)
        setResistance(config.resistance)
        config.harvestLevel.forEach { (toolClass, harvestLevel) ->
            setHarvestLevel(toolClass, harvestLevel)
        }
        creativeTab = CbTweaker.defns.creativeTab
    }

    @RequireMod(CoFHCoreCompat.MOD_ID)
    @Suppress("KIC_LEAKY_DECLARATION")
    override fun canDismantle(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer): Boolean =
        blockType.blockConfig.canWrenchDismantle

    @RequireMod(CoFHCoreCompat.MOD_ID)
    @Suppress("KIC_LEAKY_DECLARATION")
    override fun dismantleBlock(
        world: World,
        pos: BlockPos,
        state: IBlockState,
        player: EntityPlayer,
        returnDrops: Boolean
    ): ArrayList<ItemStack> {
        val drop = ItemStack(this, 1, damageDropped(state))
        world.setBlockToAir(pos) // calls breakBlock to handle destruction behaviour
        if (!returnDrops) {
            InvHelper.dropItem(drop, world, pos)
        }
        return arrayListOf(drop)
    }
}
