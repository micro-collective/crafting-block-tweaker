package xyz.phanta.cbtweaker.structure.block.impl;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.helper.ItemUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.hatch.HatchBlock;
import xyz.phanta.cbtweaker.hatch.HatchTileEntity;
import xyz.phanta.cbtweaker.hatch.HatchType;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatch;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcherType;
import xyz.phanta.cbtweaker.structure.block.StructureBlockVisualization;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class HatchStructureBlockMatcher implements StructureBlockMatcher {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("hatch");

    private final HatchType<?> hatchType;
    private final String groupId;
    private final int tierMin, tierMax;

    @Nullable
    private List<StructureBlockVisualization> visList = null;

    public HatchStructureBlockMatcher(HatchType<?> hatchType, String groupId, int tierMin, int tierMax) {
        this.hatchType = hatchType;
        this.groupId = groupId;
        this.tierMin = tierMin;
        this.tierMax = tierMax;
    }

    @Nullable
    @Override
    public StructureBlockMatch matchBlock(World world, BlockPos pos, Direction dir) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof HatchBlock)) {
            return null;
        }
        HatchBlock hatchBlock = (HatchBlock)block;
        if (hatchBlock.getHatchType() != hatchType) {
            return null;
        }
        int tier = state.getValue(hatchBlock.getTierProperty());
        if (tier < tierMin || tier > tierMax) {
            return null;
        }
        return new StructureBlockMatch.Hatch(
                groupId, (HatchTileEntity)Objects.requireNonNull(world.getTileEntity(pos)));
    }

    @Override
    public List<StructureBlockVisualization> getVisualization() {
        if (visList == null) {
            int hatchMaxTier = hatchType.getTierCount() - 1;
            visList = IntStream.rangeClosed(Math.min(tierMin, hatchMaxTier), Math.min(tierMax, hatchMaxTier))
                    .<StructureBlockVisualization>mapToObj(i -> {
                        IBlockState state = hatchType.getHatchBlock(i);
                        ItemStack stack = hatchType.getHatchStack(1, i);
                        return new StructureBlockVisualization() {
                            @Override
                            public IBlockState getBlockState() {
                                return state;
                            }

                            @Override
                            public ItemStack getRepresentative() {
                                return stack;
                            }

                            @Override
                            public void getTooltip(List<String> tooltip, ITooltipFlag tooltipFlags) {
                                ItemUtils.getStackTooltip(getRepresentative(), tooltip, tooltipFlags);
                                tooltip.add(TextFormatting.AQUA
                                        + I18n.format(CbtLang.TOOLTIP_BUFFER_GROUP, TextFormatting.WHITE + groupId));
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
            String hatchTypeId = spec.get("hatch").getAsString();
            HatchType<?> hatchType = CbtMod.PROXY.getHatches().lookUp(hatchTypeId);
            if (hatchType == null) {
                throw new ConfigException("Unknown hatch type: " + hatchTypeId);
            }
            return new HatchStructureBlockMatcher(hatchType, spec.get("hatch_group").getAsString(),
                    spec.has("tier_min") ? spec.get("tier_min").getAsInt() : 0,
                    spec.has("tier_max") ? spec.get("tier_max").getAsInt() : Integer.MAX_VALUE);
        }
    };

    @SubscribeEvent
    public static void onRegisterStructureBlockMatchers(CbtRegistrationEvent<StructureBlockMatcherType> event) {
        event.register(TYPE);
    }

}
