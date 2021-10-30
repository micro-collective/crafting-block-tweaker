package xyz.phanta.cbtweaker.integration.jei.ingredient;

import io.github.phantamanta44.libnine.util.helper.ItemUtils;
import io.github.phantamanta44.libnine.util.render.RenderUtils;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.List;

public class JeiOreDictIngredient implements JeiIngredient<ItemStack> {

    private final String oreName;
    private final Role role;

    public JeiOreDictIngredient(String oreName, Role role) {
        this.oreName = oreName;
        this.role = role;
    }

    public String getOreName() {
        return oreName;
    }

    @Override
    public List<ItemStack> getIngredients() {
        return OreDictionary.getOres(oreName, false);
    }

    @Nullable
    @Override
    public IIngredientType<ItemStack> getJeiIngredientType() {
        return VanillaTypes.ITEM;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public void renderIcon(int x, int y, ItemStack ingredient) {
        RenderUtils.renderItemIntoGui(x, y, ingredient);
    }

    @Override
    public void getTooltip(ItemStack ingredient, List<String> tooltip, ITooltipFlag tooltipFlags) {
        ItemUtils.getStackTooltip(ingredient, tooltip, tooltipFlags);
        tooltip.add(String.format(TextFormatting.DARK_GRAY + "(%s)", oreName));
    }

}
