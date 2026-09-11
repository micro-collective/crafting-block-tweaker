package st.evening.mc.cbtweaker.common

import cofh.api.block.IDismantleable
import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockFaceShape
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.common.Optional
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.compat.cofh.CoFHCoreCompat
import st.evening.mc.cbtweaker.util.BlockConfigData
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectDouble
import st.evening.mc.prelude.api.data.tjson.expectFloat
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectString
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.RequireMod
import st.evening.mc.prelude.api.util.system.Hoist

class BlockConfig(
    val material: Material,
    val mapColour: MapColor,
    val soundType: SoundType, // TODO custom sound types
    val hardness: Float,
    val resistance: Float,
    val harvestTool: String?,
    val harvestLevel: Int,
    val lightValue: Int,
    val boundingBox: AxisAlignedBB,
    val fullCube: Boolean,
    val renderLayer: BlockRenderLayer,
    val canWrenchDismantle: Boolean
) {
    companion object {
        val DEFAULT: BlockConfig = BlockConfig(
            material = Material.IRON,
            mapColour = Material.IRON.materialMapColor,
            soundType = SoundType.METAL,
            hardness = 3F,
            resistance = 15F,
            harvestTool = "pickaxe",
            harvestLevel = 0,
            lightValue = 0,
            boundingBox = Block.FULL_BLOCK_AABB,
            fullCube = true,
            renderLayer = BlockRenderLayer.SOLID,
            canWrenchDismantle = true
        )

        context(_: JsonPath)
        fun load(dto: TJson.Object): BlockConfig {
            val material = dto.useString("material") {
                BlockConfigData.materials[it] ?: throw SerializationException.withPath("Unknown material: $it")
            } ?: DEFAULT.material
            return BlockConfig(
                material,
                dto.useString("map_colour") {
                    BlockConfigData.mapColours[it] ?: throw SerializationException.withPath("Unknown map colour: $it")
                } ?: material.materialMapColor,
                dto.useString("sound_type") {
                    BlockConfigData.soundTypes[it] ?: throw SerializationException.withPath("Unknown sound type: $it")
                } ?: DEFAULT.soundType,
                dto.expectFloat("hardness") ?: DEFAULT.hardness,
                dto.expectFloat("resistance") ?: DEFAULT.resistance,
                when (val toolClass = dto.expectString("harvest_tool")) {
                    null -> DEFAULT.harvestTool
                    "none" -> null
                    else -> toolClass
                },
                dto.expectInt("harvest_level") ?: DEFAULT.harvestLevel,
                dto.expectInt("light_value") ?: DEFAULT.lightValue,
                dto.useObject("bounding_box") {
                    AxisAlignedBB(
                        it.expectDouble("x1") ?: 0.0,
                        it.expectDouble("y1") ?: 0.0,
                        it.expectDouble("z1") ?: 0.0,
                        it.expectDouble("x2") ?: 1.0,
                        it.expectDouble("y2") ?: 1.0,
                        it.expectDouble("z2") ?: 1.0,
                    )
                } ?: DEFAULT.boundingBox,
                dto.expectBool("full_cube") ?: DEFAULT.fullCube,
                dto.useString("render_layer") {
                    BlockConfigData.renderLayerSerializer.deserializeFromJson(it)
                } ?: DEFAULT.renderLayer,
                dto.expectBool("can_wrench_dismantle") ?: DEFAULT.canWrenchDismantle
            )
        }
    }
}

interface CustomBlockType {
    val blockConfig: BlockConfig
}

@Optional.Interface(iface = "cofh.api.block.IDismantleable", modid = CoFHCoreCompat.MOD_ID)
abstract class CbtCustomBlock(blockConfig: BlockConfig) :
    Block(blockConfig.material.also { Hoist.push(blockConfig) }, blockConfig.mapColour), IDismantleable {

    abstract val blockType: CustomBlockType

    open fun init() {
        val config = blockType.blockConfig
        setSoundType(config.soundType)
        setHardness(config.hardness)
        setResistance(config.resistance)
        config.harvestTool?.let {
            setHarvestLevel(it, config.harvestLevel)
        }
        lightValue = config.lightValue
        creativeTab = CbTweaker.defns.creativeTab
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB =
        blockType.blockConfig.boundingBox

    @Suppress("OVERRIDE_DEPRECATION")
    override fun isFullCube(state: IBlockState): Boolean =
        blockType.blockConfig.fullCube

    // isOpaqueCube is called in the Block constructor, before blockType is available
    @Suppress("OVERRIDE_DEPRECATION", "UNNECESSARY_SAFE_CALL")
    override fun isOpaqueCube(state: IBlockState): Boolean =
        blockType?.blockConfig?.fullCube ?: Hoist.pop<BlockConfig>().fullCube

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getBlockFaceShape(
        world: IBlockAccess, state: IBlockState, pos: BlockPos, face: EnumFacing
    ): BlockFaceShape = if (blockType.blockConfig.fullCube) BlockFaceShape.SOLID else BlockFaceShape.UNDEFINED

    @ClientSide.Physical
    override fun getRenderLayer(): BlockRenderLayer = blockType.blockConfig.renderLayer

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
