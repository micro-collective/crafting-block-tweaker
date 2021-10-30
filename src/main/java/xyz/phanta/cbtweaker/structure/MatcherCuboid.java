package xyz.phanta.cbtweaker.structure;

import gnu.trove.map.TCharObjectMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatch;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class MatcherCuboid {

    private final Map<Vec3i, Pair<Vec3i, StructureBlockMatcher>> posMatcherTable = new HashMap<>();
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    public MatcherCuboid(char[][][] blockArray, Vec3i origin, TCharObjectMap<StructureBlockMatcher> palette) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int arrY = 0; arrY < blockArray.length; arrY++) {
            char[][] ySlice = blockArray[arrY];
            for (int arrZ = 0; arrZ < ySlice.length; arrZ++) {
                char[] zSlice = ySlice[arrZ];
                for (int arrX = 0; arrX < zSlice.length; arrX++) {
                    char matcherChar = zSlice[arrX];
                    if (matcherChar == ' ') {
                        continue;
                    }
                    StructureBlockMatcher matcher = palette.get(matcherChar);
                    if (matcher == null) {
                        throw new ConfigException("Character not in palette: " + matcherChar);
                    }
                    int x = arrX - origin.getX(), y = arrY - origin.getY(), z = arrZ - origin.getZ();
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                    if (z < minZ) {
                        minZ = z;
                    }
                    if (z > maxZ) {
                        maxZ = z;
                    }
                    Vec3i offset = new Vec3i(x, y, z);
                    posMatcherTable.put(offset, Pair.of(offset, matcher));
                }
            }
        }
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public Collection<Pair<Vec3i, StructureBlockMatcher>> getMatchers() {
        return posMatcherTable.values();
    }

    public Stream<BlockPos> computePositions(BlockPos originPos, Direction dir, boolean mirror) {
        return posMatcherTable.values().stream().map(s -> computeOffsetPos(originPos, s.getLeft(), dir, mirror));
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getSizeX() {
        return maxX - minX + 1;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getSizeY() {
        return maxY - minY + 1;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getSizeZ() {
        return maxZ - minZ + 1;
    }

    @Nullable
    public StructureMatch tryMatch(World world, BlockPos originPos, Direction dir, boolean mirror) {
        StructureMatch structMatch = new StructureMatch();
        for (Pair<Vec3i, StructureBlockMatcher> posMatcher : posMatcherTable.values()) {
            BlockPos pos = computeOffsetPos(originPos, posMatcher.getLeft(), dir, mirror);
            StructureBlockMatch blockMatch = posMatcher.getRight().matchBlock(world, pos, dir);
            if (blockMatch == null) {
                return null;
            }
            structMatch.addPosition(pos);
            if (blockMatch instanceof StructureBlockMatch.Component) {
                StructureBlockMatch.Component compMatch = (StructureBlockMatch.Component)blockMatch;
                structMatch.addComponent(compMatch.getComponentId());
            } else if (blockMatch instanceof StructureBlockMatch.Hatch) {
                StructureBlockMatch.Hatch hatchMatch = (StructureBlockMatch.Hatch)blockMatch;
                structMatch.addHatch(hatchMatch.getGroupId(), hatchMatch.getHatch());
            }
        }
        return structMatch;
    }

    private static BlockPos computeOffsetPos(BlockPos basePos, Vec3i offset, Direction dir, boolean mirror) {
        return basePos.add(dir.transform(offset, mirror));
    }

}
