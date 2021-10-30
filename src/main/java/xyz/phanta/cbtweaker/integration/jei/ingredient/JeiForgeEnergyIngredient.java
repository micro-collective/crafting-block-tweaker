package xyz.phanta.cbtweaker.integration.jei.ingredient;

import net.minecraft.client.util.ITooltipFlag;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;

import java.util.Collections;
import java.util.List;

public class JeiForgeEnergyIngredient implements JeiIngredient<Integer> {

    private final int amount;
    private final String unitSymbol;
    private final Role role;

    public JeiForgeEnergyIngredient(int amount, String unitSymbol, Role role) {
        this.amount = amount;
        this.unitSymbol = unitSymbol;
        this.role = role;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public List<Integer> getIngredients() {
        return Collections.singletonList(amount);
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public void renderIcon(int x, int y, Integer ingredient) {
        CbtTextureResources.ICON_ENERGY.draw(x, y);
    }

    @Override
    public void getTooltip(Integer ingredient, List<String> tooltip, ITooltipFlag tooltipFlags) {
        tooltip.add(String.format("%,d %s", ingredient, unitSymbol));
    }

}
