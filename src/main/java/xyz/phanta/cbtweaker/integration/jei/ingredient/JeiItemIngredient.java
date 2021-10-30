package xyz.phanta.cbtweaker.integration.jei.ingredient;

import io.github.phantamanta44.libnine.util.helper.ItemUtils;
import io.github.phantamanta44.libnine.util.render.RenderUtils;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class JeiItemIngredient implements JeiIngredient<ItemStack> {

    private final List<ItemStack> matchingStacks;
    private final Role role;

    public JeiItemIngredient(List<ItemStack> matchingStacks, Role role) {
        this.matchingStacks = matchingStacks;
        this.role = role;
    }

    public JeiItemIngredient(ItemStack matchingStack, Role role) {
        this(Collections.singletonList(matchingStack), role);
    }

    @Override
    public List<ItemStack> getIngredients() {
        return matchingStacks;
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
    }

}
