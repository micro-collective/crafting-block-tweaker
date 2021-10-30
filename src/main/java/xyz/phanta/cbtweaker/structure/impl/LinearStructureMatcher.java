package xyz.phanta.cbtweaker.structure.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gnu.trove.map.TCharObjectMap;
import io.github.phantamanta44.libnine.util.math.MathUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;
import xyz.phanta.cbtweaker.structure.MatcherCuboid;
import xyz.phanta.cbtweaker.structure.StructureMatch;
import xyz.phanta.cbtweaker.structure.StructureMatcher;
import xyz.phanta.cbtweaker.structure.StructureMatcherType;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.impl.MultiBlockControllerStructureBlockMatcher;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber
public class LinearStructureMatcher implements StructureMatcher {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("linear");

    private final MatcherCuboid startRegion, repeatRegion;
    @Nullable
    private final MatcherCuboid endRegion;
    private final RepeatDir repeatDir;
    private final int repeatLength;
    private final int minInstances, maxInstances;
    private final boolean allowMirror;

    @Nullable
    private Collection<Pair<Vec3i, StructureBlockMatcher>> visCache = null;

    public LinearStructureMatcher(MatcherCuboid startRegion, MatcherCuboid repeatRegion,
                                  @Nullable MatcherCuboid endRegion,
                                  RepeatDir repeatDir, int minInstances, int maxInstances, boolean allowMirror) {
        this.startRegion = startRegion;
        this.repeatRegion = repeatRegion;
        this.endRegion = endRegion;
        this.repeatDir = repeatDir;
        this.repeatLength = repeatDir.axis.getLength(
                repeatRegion.getSizeX(), repeatRegion.getSizeY(), repeatRegion.getSizeZ());
        this.minInstances = minInstances;
        this.maxInstances = maxInstances;
        this.allowMirror = allowMirror;
    }

    @Override
    public Iterator<BlockPos> getRegion(World world, BlockPos corePos, Direction dir) {
        Set<BlockPos> positions = new HashSet<>();
        addBlocks(positions, corePos, dir, false);
        if (allowMirror) {
            addBlocks(positions, corePos, dir, true);
        }
        return positions.iterator();
    }

    private void addBlocks(Set<BlockPos> dest, BlockPos corePos, Direction dir, boolean mirror) {
        startRegion.computePositions(corePos, dir, mirror).forEach(dest::add);
        if (endRegion != null) {
            for (int i = 0; i < minInstances; i++) {
                addBlocksForRegion(dest, repeatRegion, corePos, dir, mirror, i);
            }
            addBlocksForRegion(dest, endRegion, corePos, dir, mirror, minInstances);
            for (int i = minInstances; i < maxInstances; i++) {
                addBlocksForRegion(dest, repeatRegion, corePos, dir, mirror, i);
                addBlocksForRegion(dest, endRegion, corePos, dir, mirror, i + 1);
            }
        } else {
            for (int i = 0; i < maxInstances; i++) {
                addBlocksForRegion(dest, repeatRegion, corePos, dir, mirror, i);
            }
        }
    }

    private void addBlocksForRegion(Set<BlockPos> dest, MatcherCuboid region,
                                    BlockPos corePos, Direction dir, boolean mirror, int repeatCount) {
        region.computePositions(
                        dir.transform(corePos.offset(repeatDir.face, repeatCount * repeatLength), corePos, mirror),
                        dir, mirror)
                .forEach(dest::add);
    }

    @Nullable
    @Override
    public StructureMatch findMatch(World world, BlockPos corePos, Direction dir) {
        StructureMatch match = findMatch(world, corePos, dir, false);
        if (match != null) {
            return match;
        }
        return allowMirror ? findMatch(world, corePos, dir, true) : null;
    }

    @Nullable
    private StructureMatch findMatch(World world, BlockPos corePos, Direction dir, boolean mirror) {
        StructureMatch match = startRegion.tryMatch(world, corePos, dir, mirror);
        if (match == null) {
            return null;
        }
        int repeatCount = 0;
        while (repeatCount < maxInstances) {
            if (tryMatchRegion(match, world, corePos, dir, mirror, repeatRegion, repeatCount)) {
                break;
            }
            ++repeatCount;
        }
        if (repeatCount < minInstances) {
            return null;
        }
        if (endRegion == null) {
            return match;
        }
        if (tryMatchRegion(match, world, corePos, dir, mirror, endRegion, repeatCount)) {
            return null;
        }
        return match;
    }

    // returns true if the match fails
    private boolean tryMatchRegion(StructureMatch dest, World world, BlockPos corePos, Direction dir, boolean mirror,
                                   MatcherCuboid region, int repeatCount) {
        BlockPos offsetCorePos = dir.transform(
                corePos.offset(repeatDir.face, repeatCount * repeatLength), corePos, mirror);
        StructureMatch match = region.tryMatch(world, offsetCorePos, dir, mirror);
        if (match == null) {
            return true;
        }
        Vec3i offsetVec = corePos.subtract(offsetCorePos);
        dest.addFrom(match, offsetVec.getX(), offsetVec.getY(), offsetVec.getZ());
        return false;
    }

    @Override
    public Collection<Pair<Vec3i, StructureBlockMatcher>> getVisualization() {
        if (visCache == null) {
            visCache = new ArrayList<>(startRegion.getMatchers());
            Vec3i dirVec = repeatDir.face.getDirectionVec();
            int dirX = dirVec.getX(), dirY = dirVec.getY(), dirZ = dirVec.getZ();
            for (int i = 0; i < minInstances; i++) {
                addVisualizationBlocks(visCache, repeatRegion, dirX, dirY, dirZ, i);
            }
            if (endRegion != null) {
                addVisualizationBlocks(visCache, endRegion, dirX, dirY, dirZ, minInstances);
            }
        }
        return visCache;
    }

    private void addVisualizationBlocks(Collection<Pair<Vec3i, StructureBlockMatcher>> dest,
                                        MatcherCuboid region, int dirX, int dirY, int dirZ, int repeatCount) {
        int offX = dirX * repeatCount * repeatLength;
        int offY = dirY * repeatCount * repeatLength;
        int offZ = dirZ * repeatCount * repeatLength;
        region.getMatchers().stream()
                .map(m -> Pair.of(MathUtils.add(m.getLeft(), offX, offY, offZ), m.getRight()))
                .forEach(dest::add);
    }

    public static final StructureMatcherType TYPE = new StructureMatcherType() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public StructureMatcher loadMatcher(MultiBlockType<?, ?, ?> mbType, JsonObject spec) {
            RepeatDir repeatDir = RepeatDir.fromString(spec.get("repeat_direction").getAsString());
            TCharObjectMap<StructureBlockMatcher> palette
                    = SimpleStructureMatcher.loadPalette(spec.getAsJsonObject("palette"));
            palette.put('@', new MultiBlockControllerStructureBlockMatcher(mbType));

            char[][][] startBlkArr = SimpleStructureMatcher.loadBlockArray(spec.getAsJsonArray("structure_start"));
            Vec3i ctrlPos = SimpleStructureMatcher.findControllerPosition(startBlkArr);
            MatcherCuboid startRegion = new MatcherCuboid(startBlkArr, ctrlPos, palette);
            int startSizeX = startRegion.getSizeX();
            int startSizeY = startRegion.getSizeY();
            int startSizeZ = startRegion.getSizeZ();

            MatcherCuboid repeatRegion = parseExtensionRegion(spec.getAsJsonArray("structure_repeat"),
                    repeatDir, ctrlPos, palette, "repeat", startSizeX, startSizeY, startSizeZ);
            if (repeatRegion == null) {
                throw new ConfigException("Repeat region cannot be empty!");
            }
            MatcherCuboid endRegion = parseExtensionRegion(spec.getAsJsonArray("structure_end"),
                    repeatDir, ctrlPos, palette, "end", startSizeX, startSizeY, startSizeZ);

            return new LinearStructureMatcher(startRegion, repeatRegion, endRegion, repeatDir,
                    spec.get("min_instances").getAsInt(), spec.get("max_instances").getAsInt(),
                    !spec.has("allow_mirror") || spec.get("allow_mirror").getAsBoolean());
        }
    };

    @Nullable
    private static MatcherCuboid parseExtensionRegion(JsonArray blockArrayDto, RepeatDir repeatDir, Vec3i ctrlPos,
                                                      TCharObjectMap<StructureBlockMatcher> palette, String name,
                                                      int startSizeX, int startSizeY, int startSizeZ) {
        char[][][] blockArray = SimpleStructureMatcher.loadBlockArray(blockArrayDto);
        Vec3i extDims = computeSize(blockArray);
        int extSizeX = extDims.getX(), extSizeY = extDims.getY(), extSizeZ = extDims.getZ();
        if (extSizeX == 0 || extSizeY == 0 || extSizeZ == 0) {
            return null;
        }
        if (!repeatDir.axis.checkFlush(startSizeX, startSizeY, startSizeZ, extSizeX, extSizeY, extSizeZ)) {
            throw new ConfigException(String.format(
                    "Dimension mismatch for %s region! Expected (%d, %d, %d) but got (%d, %d, %d)!",
                    name, startSizeX, startSizeY, startSizeZ, extSizeX, extSizeY, extSizeZ));
        }
        Vec3i cxnPos = repeatDir.getConnectionPosition(
                startSizeX, startSizeY, startSizeZ, extSizeX, extSizeY, extSizeZ);
        return new MatcherCuboid(blockArray, MathUtils.subtract(ctrlPos, cxnPos), palette);
    }

    private static Vec3i computeSize(char[][][] blockArray) {
        int sizeX = 0, sizeZ = 0;
        for (char[][] ySlice : blockArray) {
            for (char[] zSlice : ySlice) {
                if (zSlice.length > sizeX) {
                    sizeX = zSlice.length;
                }
            }
            if (ySlice.length > sizeZ) {
                sizeZ = ySlice.length;
            }
        }
        return new Vec3i(sizeX, blockArray.length, sizeZ);
    }

    @SubscribeEvent
    public static void onRegisterStructureMatchers(CbtRegistrationEvent<StructureMatcherType> event) {
        event.register(TYPE);
    }

    private enum RepeatDir {

        X_POS(EnumFacing.EAST, RepeatAxis.X) {
            @Override
            public Vec3i getConnectionPosition(int baseSizeX, int baseSizeY, int baseSizeZ,
                                               int extSizeX, int extSizeY, int extSizeZ) {
                return new Vec3i(baseSizeX, 0, 0);
            }
        },
        X_NEG(EnumFacing.WEST, RepeatAxis.X) {
            @Override
            public Vec3i getConnectionPosition(int baseSizeX, int baseSizeY, int baseSizeZ,
                                               int extSizeX, int extSizeY, int extSizeZ) {
                return new Vec3i(-extSizeX, 0, 0);
            }
        },
        Y_POS(EnumFacing.UP, RepeatAxis.Y) {
            @Override
            public Vec3i getConnectionPosition(int baseSizeX, int baseSizeY, int baseSizeZ,
                                               int extSizeX, int extSizeY, int extSizeZ) {
                return new Vec3i(0, baseSizeY, 0);
            }
        },
        Y_NEG(EnumFacing.DOWN, RepeatAxis.Y) {
            @Override
            public Vec3i getConnectionPosition(int baseSizeX, int baseSizeY, int baseSizeZ,
                                               int extSizeX, int extSizeY, int extSizeZ) {
                return new Vec3i(0, -extSizeY, 0);
            }
        },
        Z_POS(EnumFacing.SOUTH, RepeatAxis.Z) {
            @Override
            public Vec3i getConnectionPosition(int baseSizeX, int baseSizeY, int baseSizeZ,
                                               int extSizeX, int extSizeY, int extSizeZ) {
                return new Vec3i(0, 0, baseSizeZ);
            }
        },
        Z_NEG(EnumFacing.NORTH, RepeatAxis.Z) {
            @Override
            public Vec3i getConnectionPosition(int baseSizeX, int baseSizeY, int baseSizeZ,
                                               int extSizeX, int extSizeY, int extSizeZ) {
                return new Vec3i(0, 0, -extSizeZ);
            }
        };

        public static RepeatDir fromString(String str) {
            switch (str.toLowerCase(Locale.ROOT)) {
                case "+x":
                    return X_POS;
                case "-x":
                    return X_NEG;
                case "+y":
                    return Y_POS;
                case "-y":
                    return Y_NEG;
                case "+z":
                    return Z_POS;
                case "-z":
                    return Z_NEG;
            }
            throw new ConfigException("Direction must be of the form [+-][xyz], but got: " + str);
        }

        public final EnumFacing face;
        public final RepeatAxis axis;

        RepeatDir(EnumFacing face, RepeatAxis axis) {
            this.face = face;
            this.axis = axis;
        }

        public abstract Vec3i getConnectionPosition(int baseSizeX, int baseSizeY, int baseSizeZ,
                                                    int extSizeX, int extSizeY, int extSizeZ);

    }

    private enum RepeatAxis {

        X {
            @Override
            public int getLength(int sizeX, int sizeY, int sizeZ) {
                return sizeX;
            }

            @Override
            public boolean checkFlush(int aSizeX, int aSizeY, int aSizeZ, int bSizeX, int bSizeY, int bSizeZ) {
                return aSizeY == bSizeY && aSizeZ == bSizeZ;
            }
        },
        Y {
            @Override
            public int getLength(int sizeX, int sizeY, int sizeZ) {
                return sizeY;
            }

            @Override
            public boolean checkFlush(int aSizeX, int aSizeY, int aSizeZ, int bSizeX, int bSizeY, int bSizeZ) {
                return aSizeX == bSizeX && aSizeZ == bSizeZ;
            }
        },
        Z {
            @Override
            public int getLength(int sizeX, int sizeY, int sizeZ) {
                return sizeZ;
            }

            @Override
            public boolean checkFlush(int aSizeX, int aSizeY, int aSizeZ, int bSizeX, int bSizeY, int bSizeZ) {
                return aSizeX == bSizeX && aSizeY == bSizeY;
            }
        };

        public abstract int getLength(int sizeX, int sizeY, int sizeZ);

        public abstract boolean checkFlush(int aSizeX, int aSizeY, int aSizeZ, int bSizeX, int bSizeY, int bSizeZ);

    }

}
