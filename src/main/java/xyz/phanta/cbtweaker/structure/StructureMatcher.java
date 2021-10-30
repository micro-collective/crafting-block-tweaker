package xyz.phanta.cbtweaker.structure;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;

public interface StructureMatcher {

    Iterator<BlockPos> getRegion(World world, BlockPos corePos, Direction dir);

    @Nullable
    StructureMatch findMatch(World world, BlockPos corePos, Direction dir);

    Collection<Pair<Vec3i, StructureBlockMatcher>> getVisualization();

}
