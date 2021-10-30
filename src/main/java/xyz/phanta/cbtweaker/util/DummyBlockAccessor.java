package xyz.phanta.cbtweaker.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class DummyBlockAccessor implements IBlockAccess {

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return lightValue;
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return Biomes.PLAINS;
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return 0;
    }

    @Override
    public WorldType getWorldType() {
        return WorldType.FLAT;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return getBlockState(pos).isSideSolid(this, pos, side);
    }

    public static class MapBacked extends DummyBlockAccessor {

        private final Map<BlockPos, IBlockState> blockTable = new HashMap<>();

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            IBlockState state = blockTable.get(pos);
            return state != null ? state : Blocks.AIR.getDefaultState();
        }

        public void setBlockState(BlockPos pos, IBlockState state) {
            blockTable.put(pos, state);
        }

        public Collection<Map.Entry<BlockPos, IBlockState>> getEntries() {
            return blockTable.entrySet();
        }

    }

}
