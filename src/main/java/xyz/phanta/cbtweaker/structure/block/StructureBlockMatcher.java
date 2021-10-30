package xyz.phanta.cbtweaker.structure.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface StructureBlockMatcher {

    @Nullable
    StructureBlockMatch matchBlock(World world, BlockPos pos, Direction dir);

    List<StructureBlockVisualization> getVisualization();

    class MatcherSet implements StructureBlockMatcher {

        public static MatcherSet of(StructureBlockMatcher... matchers) {
            MatcherSet matcherSet = new MatcherSet();
            matcherSet.addAll(Arrays.asList(matchers));
            return matcherSet;
        }

        private final List<StructureBlockMatcher> matchers = new ArrayList<>();
        @Nullable
        private List<StructureBlockVisualization> visList;

        public void add(StructureBlockMatcher matcher) {
            matchers.add(matcher);
        }

        public void addAll(Collection<StructureBlockMatcher> matchers) {
            this.matchers.addAll(matchers);
        }

        @Nullable
        @Override
        public StructureBlockMatch matchBlock(World world, BlockPos pos, Direction dir) {
            for (StructureBlockMatcher matcher : matchers) {
                StructureBlockMatch match = matcher.matchBlock(world, pos, dir);
                if (match != null) {
                    return match;
                }
            }
            return null;
        }

        @Override
        public List<StructureBlockVisualization> getVisualization() {
            if (visList == null) {
                visList = matchers.stream().flatMap(m -> m.getVisualization().stream()).collect(Collectors.toList());
            }
            return visList;
        }

    }

}
