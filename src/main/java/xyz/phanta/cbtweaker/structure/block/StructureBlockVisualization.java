package xyz.phanta.cbtweaker.structure.block;

import io.github.phantamanta44.libnine.util.helper.ItemUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface StructureBlockVisualization {

    IBlockState getBlockState();

    default ItemStack getRepresentative() {
        return ItemUtils.getItemForBlock(getBlockState());
    }

    default void getTooltip(List<String> tooltip, ITooltipFlag tooltipFlags) {
        ItemStack stack = getRepresentative();
        if (stack.isEmpty()) {
            tooltip.add(getBlockState().getBlock().getLocalizedName());
        } else {
            ItemUtils.getStackTooltip(stack, tooltip, tooltipFlags);
        }
    }

}
