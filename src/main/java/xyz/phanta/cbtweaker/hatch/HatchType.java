package xyz.phanta.cbtweaker.hatch;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.buffer.BufferFactory;
import xyz.phanta.cbtweaker.buffer.BufferObserver;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.common.BlockMaterial;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class HatchType<B> {

    private final String id;
    private final BlockMaterial blockMaterial;
    private final BufferType<B, ?, ?, ?> bufferType;
    private final List<TierData<B>> tierData;

    @Nullable
    private HatchBlock hatchBlock = null;

    public HatchType(String id, BlockMaterial blockMaterial,
                     BufferType<B, ?, ?, ?> bufferType, List<TierData<B>> tierData) {
        this.id = id;
        this.blockMaterial = blockMaterial;
        this.bufferType = bufferType;
        this.tierData = tierData;
    }

    public String getId() {
        return id;
    }

    public BlockMaterial getBlockMaterial() {
        return blockMaterial;
    }

    public BufferType<B, ?, ?, ?> getBufferType() {
        return bufferType;
    }

    public int getTierCount() {
        return tierData.size();
    }

    public TierData<B> getTier(int tier) {
        return tierData.get(tier);
    }

    public HatchBlock getHatchBlock() {
        if (hatchBlock == null) {
            hatchBlock = Objects.requireNonNull(
                    (HatchBlock)ForgeRegistries.BLOCKS.getValue(CbtMod.INSTANCE.newResourceLocation("hatch_" + id)));
        }
        return hatchBlock;
    }

    public IBlockState getHatchBlock(int tier) {
        HatchBlock block = getHatchBlock();
        return block.getDefaultState().withProperty(block.getTierProperty(), tier);
    }

    public ItemStack getHatchStack(int count, int tier) {
        HatchBlock block = getHatchBlock();
        return new ItemStack(block, count, tier);
    }

    public static class TierData<B> {

        private final BufferFactory<B, ?> bufferFactory;
        private final UiLayout uiLayout;

        public TierData(BufferFactory<B, ?> bufferFactory, UiLayout uiLayout) {
            this.bufferFactory = bufferFactory;
            this.uiLayout = uiLayout;
        }

        public B createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer) {
            return bufferFactory.createBuffer(world, pos, observer);
        }

        public UiLayout getUiLayout() {
            return uiLayout;
        }

    }

}
