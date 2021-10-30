package xyz.phanta.cbtweaker.structure.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gnu.trove.map.TCharObjectMap;
import gnu.trove.map.hash.TCharObjectHashMap;
import io.github.phantamanta44.libnine.util.helper.JsonUtils9;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
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
import xyz.phanta.cbtweaker.structure.block.impl.BlockIdentityStructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.impl.MultiBlockControllerStructureBlockMatcher;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class SimpleStructureMatcher implements StructureMatcher {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("simple");

    private final MatcherCuboid matchRegion;
    private final boolean allowMirror;

    public SimpleStructureMatcher(MatcherCuboid matchRegion, boolean allowMirror) {
        this.matchRegion = matchRegion;
        this.allowMirror = allowMirror;
    }

    @Override
    public Iterator<BlockPos> getRegion(World world, BlockPos corePos, Direction dir) {
        Stream<BlockPos> stream = matchRegion.computePositions(corePos, dir, false);
        if (allowMirror) {
            stream = Stream.concat(stream, matchRegion.computePositions(corePos, dir, true));
        }
        return stream.iterator();
    }

    @Nullable
    @Override
    public StructureMatch findMatch(World world, BlockPos corePos, Direction dir) {
        StructureMatch match = matchRegion.tryMatch(world, corePos, dir, false);
        if (match != null) {
            return match;
        }
        return allowMirror ? matchRegion.tryMatch(world, corePos, dir, true) : null;
    }

    @Override
    public Collection<Pair<Vec3i, StructureBlockMatcher>> getVisualization() {
        return matchRegion.getMatchers();
    }

    public static final StructureMatcherType TYPE = new StructureMatcherType() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public StructureMatcher loadMatcher(MultiBlockType<?, ?, ?> mbType, JsonObject spec) {
            TCharObjectMap<StructureBlockMatcher> palette = loadPalette(spec.getAsJsonObject("palette"));
            palette.put('@', new MultiBlockControllerStructureBlockMatcher(mbType));
            char[][][] blockArray = loadBlockArray(spec.getAsJsonArray("structure"));
            return new SimpleStructureMatcher(
                    new MatcherCuboid(blockArray, findControllerPosition(blockArray), palette),
                    !spec.has("allow_mirror") || spec.get("allow_mirror").getAsBoolean());
        }
    };

    public static TCharObjectMap<StructureBlockMatcher> loadPalette(JsonObject paletteDto) {
        TCharObjectMap<StructureBlockMatcher> palette = new TCharObjectHashMap<>();
        palette.put('.', BlockIdentityStructureBlockMatcher.MATCH_AIR);
        for (Map.Entry<String, JsonElement> paletteEntry : paletteDto.entrySet()) {
            char blockSym = paletteEntry.getKey().charAt(0);
            if (blockSym == ' ' || blockSym == '.' || blockSym == '@') {
                throw new ConfigException(String.format("Invalid palette character: [%c]", blockSym));
            }
            StructureBlockMatcher.MatcherSet matchers = new StructureBlockMatcher.MatcherSet();
            JsonElement matchersDto = paletteEntry.getValue();
            if (matchersDto.isJsonArray()) {
                for (JsonElement matcherDto : matchersDto.getAsJsonArray()) {
                    matchers.addAll(
                            CbtMod.PROXY.getTemplates().getStructureBlockMatcherTemplates().resolve(matcherDto));
                }
            } else {
                matchers.addAll(CbtMod.PROXY.getTemplates().getStructureBlockMatcherTemplates().resolve(matchersDto));
            }
            palette.put(blockSym, matchers);
        }
        return palette;
    }

    public static char[][][] loadBlockArray(JsonArray blockArrayDto) {
        // y and z slices have their min at the end of the array and their max at the top, so we reverse
        return ObjectArrays.reverse(JsonUtils9.stream(blockArrayDto)
                .map(e1 -> ObjectArrays.reverse(JsonUtils9.stream(e1.getAsJsonArray())
                        .map(e2 -> e2.getAsString().toCharArray())
                        .toArray(char[][]::new)))
                .toArray(char[][][]::new));
    }

    public static List<Vec3i> searchBlockArray(char[][][] blockArray, char query) {
        List<Vec3i> results = new ArrayList<>();
        for (int y = 0; y < blockArray.length; y++) {
            char[][] ySlice = blockArray[y];
            for (int z = 0; z < ySlice.length; z++) {
                char[] zSlice = ySlice[z];
                for (int x = 0; x < zSlice.length; x++) {
                    if (zSlice[x] == query) {
                        results.add(new Vec3i(x, y, z));
                    }
                }
            }
        }
        return results;
    }

    public static Vec3i findControllerPosition(char[][][] blockArray) {
        List<Vec3i> ctrlPosCandidates = searchBlockArray(blockArray, '@');
        if (ctrlPosCandidates.isEmpty()) {
            throw new ConfigException("Structure has no controller position!");
        } else if (ctrlPosCandidates.size() > 1) {
            throw new ConfigException("Structure has more than one controller position!");
        }
        return ctrlPosCandidates.get(0);
    }

    @SubscribeEvent
    public static void onRegisterStructureMatchers(CbtRegistrationEvent<StructureMatcherType> event) {
        event.register(TYPE);
    }

}
