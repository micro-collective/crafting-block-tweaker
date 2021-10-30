package xyz.phanta.cbtweaker.hatch;

import com.google.common.base.Optional;
import io.github.phantamanta44.libnine.util.math.IntRange;
import io.github.phantamanta44.libnine.util.world.WorldBlockPos;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyHelper;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.common.CbtCustomBlock;
import xyz.phanta.cbtweaker.gui.CbtGuiIds;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;

public class HatchBlock extends CbtCustomBlock implements ITileEntityProvider {

    @Nullable
    private static TierProperty ctorTierProp = null;

    public static HatchBlock construct(HatchType<?> hatchType) {
        // createBlockState gets called *at construction time, in the superconstructor*
        // so we have to do this dumb hack to be able to pass the property to createBlockState
        ctorTierProp = new TierProperty(hatchType.getTierCount());
        HatchBlock block = new HatchBlock(hatchType);
        ctorTierProp = null;
        return block;
    }

    private final HatchType<?> hatchType;
    private final IProperty<Integer> tierProp;

    private HatchBlock(HatchType<?> hatchType) {
        super(hatchType.getBlockMaterial());
        this.hatchType = hatchType;
        this.tierProp = Objects.requireNonNull(ctorTierProp);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, Objects.requireNonNull(ctorTierProp));
    }

    public HatchType<?> getHatchType() {
        return hatchType;
    }

    public IProperty<Integer> getTierProperty() {
        return tierProp;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (int i = 0; i < hatchType.getTierCount(); i++) {
            items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(tierProp);
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(tierProp, meta);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new HatchTileEntity();
    }

    @Override
    public String getTranslationKey() {
        return CbtMod.MOD_ID + ".hatch." + hatchType.getId();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        HatchTileEntity hatch = (HatchTileEntity)world.getTileEntity(pos);
        if (hatch == null) {
            return false;
        }
        if (hatch.handleInteraction(player, hand, facing)) {
            return true;
        }
        CbtMod.INSTANCE.getGuiHandler().openGui(player, CbtGuiIds.HATCH, new WorldBlockPos(world, pos));
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        HatchTileEntity hatch = (HatchTileEntity)world.getTileEntity(pos);
        if (hatch != null) {
            hatch.dropContents();
        }
        super.breakBlock(world, pos, state);
    }

    private static class TierProperty extends PropertyHelper<Integer> {

        private final IntRange tierRange;

        protected TierProperty(int tierCount) {
            super("tier", Integer.class);
            this.tierRange = new IntRange(0, tierCount);
        }

        @Override
        public Collection<Integer> getAllowedValues() {
            return tierRange;
        }

        @SuppressWarnings("Guava")
        @Override
        public Optional<Integer> parseValue(String value) {
            try {
                int tier = Integer.parseInt(value);
                return tierRange.contains(tier) ? Optional.of(tier) : Optional.absent();
            } catch (NumberFormatException e) {
                return Optional.absent();
            }
        }

        @Override
        public String getName(Integer value) {
            return value.toString();
        }

    }

}
