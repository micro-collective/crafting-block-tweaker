package xyz.phanta.cbtweaker.hatch;

import io.github.phantamanta44.libnine.client.model.ParameterizedItemModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.util.item.CustomModelItem;

import javax.annotation.Nullable;
import java.util.Objects;

public class HatchBlockItem extends ItemBlock implements CustomModelItem, ParameterizedItemModel.IContextSensitive {

    private final HatchBlock block;

    public HatchBlockItem(HatchBlock block) {
        super(block);
        this.block = block;
        setHasSubtypes(true);
    }

    @Override
    public HatchBlock getBlock() {
        return block;
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return block.getTranslationKey() + "." + stack.getMetadata();
    }

    @Override
    public void collectItemModels(ModelAcceptor modelAcceptor) {
        String path = Objects.requireNonNull(getRegistryName()).getPath();
        for (int i = 0; i < block.getHatchType().getTierCount(); i++) {
            modelAcceptor.accept(i, path);
        }
    }

    @Override
    public void getModelMutations(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase holder,
                                  ParameterizedItemModel.Mutation m) {
        m.mutate("tier", Integer.toString(stack.getMetadata()));
    }

}
