package st.evening.mc.cbtweaker.util.world

import net.minecraft.block.state.IBlockState
import net.minecraft.init.Biomes
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.WorldType
import net.minecraft.world.biome.Biome
import st.evening.mc.prelude.api.util.game.ClientSide

abstract class DummyBlockAccessor : IBlockAccess {
    override fun getTileEntity(pos: BlockPos): TileEntity? = null

    @ClientSide.Physical
    override fun getCombinedLight(pos: BlockPos, lightValue: Int): Int = lightValue

    override fun isAirBlock(pos: BlockPos): Boolean = getBlockState(pos).let { it.block.isAir(it, this, pos) }

    @ClientSide.Physical
    override fun getBiome(pos: BlockPos): Biome = Biomes.PLAINS

    override fun getStrongPower(pos: BlockPos, direction: EnumFacing): Int = 0

    @ClientSide.Physical
    override fun getWorldType(): WorldType = WorldType.FLAT

    override fun isSideSolid(pos: BlockPos, side: EnumFacing, default: Boolean): Boolean =
        getBlockState(pos).isSideSolid(this, pos, side)

    class MapBacked : DummyBlockAccessor() {
        private val blockTable: MutableMap<BlockPos, IBlockState> = mutableMapOf()

        val entries: Collection<Map.Entry<BlockPos, IBlockState>>
            get() = blockTable.entries

        override fun getBlockState(pos: BlockPos): IBlockState = blockTable[pos] ?: Blocks.AIR.defaultState

        fun setBlockState(pos: BlockPos, state: IBlockState) {
            blockTable[pos] = state
        }
    }

}
