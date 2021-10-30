package xyz.phanta.cbtweaker.structure.block.impl;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerBlock;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatch;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.StructureBlockVisualization;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

// cannot be manually used; rather, is automatically generated during multi-block loading
public class MultiBlockControllerStructureBlockMatcher implements StructureBlockMatcher {

    private final MultiBlockType<?, ?, ?> mbType;

    public MultiBlockControllerStructureBlockMatcher(MultiBlockType<?, ?, ?> mbType) {
        this.mbType = mbType;
    }

    @Nullable
    @Override
    public StructureBlockMatch matchBlock(World world, BlockPos pos, Direction dir) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof MultiBlockControllerBlock)
                || ((MultiBlockControllerBlock)block).getMultiBlockType() != mbType) {
            return null;
        }
        return StructureBlockMatch.Normal.INSTANCE;
    }

    @Override
    public List<StructureBlockVisualization> getVisualization() {
        return Collections.singletonList(() -> mbType.getControllerBlock().getDefaultState());
    }

}
