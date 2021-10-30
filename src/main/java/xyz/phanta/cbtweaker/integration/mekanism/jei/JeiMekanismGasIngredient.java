package xyz.phanta.cbtweaker.integration.mekanism.jei;

import mekanism.api.gas.GasStack;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.TextFormatting;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.mekanism.gui.InteractiveGasTankGuiComponent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class JeiMekanismGasIngredient implements JeiIngredient<GasStack> {

    private final GasStack gas;
    private final Role role;

    public JeiMekanismGasIngredient(GasStack gas, Role role) {
        this.gas = gas;
        this.role = role;
    }

    public GasStack getGas() {
        return gas;
    }

    @Override
    public List<GasStack> getIngredients() {
        return Collections.singletonList(gas);
    }

    @Nullable
    @Override
    public IIngredientType<GasStack> getJeiIngredientType() {
        return MekanismJEI.TYPE_GAS;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public void renderIcon(int x, int y, GasStack ingredient) {
        InteractiveGasTankGuiComponent.renderGas(ingredient, ingredient.amount, x, y, 16, 16);
    }

    @Override
    public void getTooltip(GasStack ingredient, List<String> tooltip, ITooltipFlag tooltipFlags) {
        tooltip.add(ingredient.getGas().getLocalizedName());
        tooltip.add(TextFormatting.GRAY + String.format("%,d mB", ingredient.amount));
    }

}
