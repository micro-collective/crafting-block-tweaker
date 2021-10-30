package xyz.phanta.cbtweaker.structure.block.impl;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.gameobject.BlockIdentity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatch;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcherType;
import xyz.phanta.cbtweaker.structure.block.StructureBlockVisualization;
import xyz.phanta.cbtweaker.util.Direction;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class BlockIdentityStructureBlockMatcher implements StructureBlockMatcher {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("block");

    public static final StructureBlockMatcher MATCH_AIR
            = new BlockIdentityStructureBlockMatcher(BlockIdentity.AIR, null, false);

    private final BlockIdentity ident;
    @Nullable
    private final String compId;
    private final boolean visualize;

    public BlockIdentityStructureBlockMatcher(BlockIdentity ident, @Nullable String compId, boolean visualize) {
        this.ident = ident;
        this.compId = compId;
        this.visualize = visualize;
    }

    @Nullable
    @Override
    public StructureBlockMatch matchBlock(World world, BlockPos pos, Direction dir) {
        return ident.matches(world.getBlockState(pos).withRotation(dir.getOpposite().getRotationFromNorth()))
                ? StructureBlockMatch.maybeComponent(compId) : null;
    }

    @Override
    public List<StructureBlockVisualization> getVisualization() {
        return visualize ? Collections.singletonList(ident::createState) : Collections.emptyList();
    }

    public static final StructureBlockMatcherType TYPE = new StructureBlockMatcherType() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public StructureBlockMatcher loadMatcher(JsonObject spec) {
            return new BlockIdentityStructureBlockMatcher(DataLoadUtils.loadBlockIdentity(spec),
                    spec.has("comp") ? spec.get("comp").getAsString() : null,
                    !spec.has("visualize") || spec.get("visualize").getAsBoolean());
        }
    };

    @SubscribeEvent
    public static void onRegisterStructureBlockMatchers(CbtRegistrationEvent<StructureBlockMatcherType> event) {
        event.register(TYPE);
    }

}
