package xyz.phanta.cbtweaker.structure.block.impl;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.helper.ItemUtils;
import io.github.phantamanta44.libnine.util.helper.OreDictUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatch;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcherType;
import xyz.phanta.cbtweaker.structure.block.StructureBlockVisualization;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class OreDictionaryStructureBlockMatcher implements StructureBlockMatcher {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("ore_dict");

    private final String oreName;
    @Nullable
    private final String compId;
    private final boolean visualize;

    @Nullable
    private List<StructureBlockVisualization> visList = null;

    public OreDictionaryStructureBlockMatcher(String oreName, @Nullable String compId, boolean visualize) {
        this.oreName = oreName;
        this.compId = compId;
        this.visualize = visualize;
    }

    @Nullable
    @Override
    public StructureBlockMatch matchBlock(World world, BlockPos pos, Direction dir) {
        IBlockState state = world.getBlockState(pos).withRotation(dir.getOpposite().getRotationFromNorth());
        return OreDictUtils.matchesOredict(ItemUtils.getItemForBlock(state), oreName)
                ? StructureBlockMatch.maybeComponent(compId) : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<StructureBlockVisualization> getVisualization() {
        if (!visualize) {
            return Collections.emptyList();
        }
        if (visList == null) {
            visList = OreDictionary.getOres(oreName, false).stream()
                    .filter(s -> s.getItem() instanceof ItemBlock)
                    .map(s -> {
                        ItemBlock item = (ItemBlock)s.getItem();
                        IBlockState state = item.getBlock().getStateFromMeta(item.getMetadata(s.getMetadata()));
                        // should mostly work
                        return new StructureBlockVisualization() {
                            @Override
                            public IBlockState getBlockState() {
                                return state;
                            }

                            @Override
                            public ItemStack getRepresentative() {
                                return s;
                            }

                            @Override
                            public void getTooltip(List<String> tooltip, ITooltipFlag tooltipFlags) {
                                ItemUtils.getStackTooltip(s, tooltip, tooltipFlags);
                                tooltip.add(String.format(TextFormatting.DARK_GRAY + "(%s)", oreName));
                            }
                        };
                    })
                    .collect(Collectors.toList());
        }
        return visList;
    }

    public static final StructureBlockMatcherType TYPE = new StructureBlockMatcherType() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public StructureBlockMatcher loadMatcher(JsonObject spec) {
            return new OreDictionaryStructureBlockMatcher(spec.get("ore").getAsString(),
                    spec.has("comp") ? spec.get("comp").getAsString() : null,
                    !spec.has("visualize") || spec.get("visualize").getAsBoolean());
        }
    };

    @SubscribeEvent
    public static void onRegisterStructureBlockMatchers(CbtRegistrationEvent<StructureBlockMatcherType> event) {
        event.register(TYPE);
    }

}
