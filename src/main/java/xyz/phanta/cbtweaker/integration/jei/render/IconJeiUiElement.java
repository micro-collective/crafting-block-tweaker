package xyz.phanta.cbtweaker.integration.jei.render;

import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.TextFormatting;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;

import javax.annotation.Nullable;
import java.util.List;

public class IconJeiUiElement<T> implements JeiUiElement<T> {

    private final JeiIngredient<T> ingredient;
    @Nullable
    private final String bufGroupId;
    private final ScreenRegion region;

    public IconJeiUiElement(int x, int y, JeiIngredient<T> ingredient, @Nullable String bufGroupId) {
        this.ingredient = ingredient;
        this.bufGroupId = bufGroupId;
        this.region = new ScreenRegion(x, y, 16, 16);
    }

    @Nullable
    @Override
    public JeiIngredient<T> getIngredient() {
        return ingredient;
    }

    @Nullable
    @Override
    public IIngredientType<T> getJeiIngredientType() {
        return ingredient.getJeiIngredientType();
    }

    @Override
    public ScreenRegion getIngredientRegion() {
        return region;
    }

    @Override
    public void render(@Nullable T ingredient) {
        if (ingredient != null) {
            this.ingredient.renderIcon(region.getX(), region.getY(), ingredient);
        }
    }

    @Override
    public void getTooltip(@Nullable T ingredient, List<String> tooltip, ITooltipFlag tooltipFlags) {
        if (ingredient != null) {
            this.ingredient.getTooltip(ingredient, tooltip, tooltipFlags);
        }
        if (bufGroupId != null) {
            tooltip.add(TextFormatting.AQUA
                    + I18n.format(CbtLang.TOOLTIP_BUFFER_GROUP, TextFormatting.WHITE + bufGroupId));
        }
    }

}
