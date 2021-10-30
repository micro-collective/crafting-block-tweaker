package xyz.phanta.cbtweaker.integration.mekanism.jei;

import io.github.phantamanta44.libnine.util.format.FormatUtils;
import mekanism.common.util.UnitDisplayUtils;
import net.minecraft.client.util.ITooltipFlag;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;

import java.util.Collections;
import java.util.List;

public class JeiMekanismHeatIngredient implements JeiIngredient<Double> {

    private final double amount;
    private final boolean isRate;
    private final Role role;

    public JeiMekanismHeatIngredient(double amount, boolean isRate, Role role) {
        this.amount = amount;
        this.isRate = isRate;
        this.role = role;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public List<Double> getIngredients() {
        return Collections.singletonList(amount);
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public void renderIcon(int x, int y, Double ingredient) {
        CbtTextureResources.MEKANISM_ICON_HEAT.draw(x, y);
    }

    @Override
    public void getTooltip(Double ingredient, List<String> tooltip, ITooltipFlag tooltipFlags) {
        tooltip.add(isRate ? FormatUtils.formatSI(amount, "K/t")
                : FormatUtils.formatSI(UnitDisplayUtils.TemperatureUnit.AMBIENT.convertToK(amount, true), "K"));
    }

}
