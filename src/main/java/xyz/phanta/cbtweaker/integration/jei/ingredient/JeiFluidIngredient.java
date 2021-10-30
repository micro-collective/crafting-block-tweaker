package xyz.phanta.cbtweaker.integration.jei.ingredient;

import io.github.phantamanta44.libnine.util.render.FluidRenderUtils;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class JeiFluidIngredient implements JeiIngredient<FluidStack> {

    private final FluidStack fluid;
    private final Role role;

    public JeiFluidIngredient(FluidStack fluid, Role role) {
        this.fluid = fluid;
        this.role = role;
    }

    public FluidStack getFluid() {
        return fluid;
    }

    @Override
    public List<FluidStack> getIngredients() {
        return Collections.singletonList(fluid);
    }

    @Nullable
    @Override
    public IIngredientType<FluidStack> getJeiIngredientType() {
        return VanillaTypes.FLUID;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public void renderIcon(int x, int y, FluidStack ingredient) {
        FluidRenderUtils.renderFluidIntoGuiCleanly(x, y, 16, 16, ingredient, ingredient.amount);
    }

    @Override
    public void getTooltip(FluidStack ingredient, List<String> tooltip, ITooltipFlag tooltipFlags) {
        tooltip.add(ingredient.getFluid().getRarity(ingredient).color + ingredient.getLocalizedName());
        tooltip.add(TextFormatting.GRAY + String.format("%,d mB", ingredient.amount));
    }

}
