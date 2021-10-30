package xyz.phanta.cbtweaker.integration.jei.render;

import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface JeiUiElement<T> {

    @Nullable
    default JeiIngredient<T> getIngredient() {
        return null;
    }

    @Nullable
    default IIngredientType<T> getJeiIngredientType() {
        JeiIngredient<T> ing = getIngredient();
        return ing != null ? ing.getJeiIngredientType() : null;
    }

    ScreenRegion getIngredientRegion();

    void render(@Nullable T ingredient);

    default void getTooltip(@Nullable T ingredient, List<String> tooltip, ITooltipFlag tooltipFlags) {
        if (ingredient != null) {
            Objects.requireNonNull(getIngredient()).getTooltip(ingredient, tooltip, tooltipFlags);
        }
    }

    class JeiRenderer<T> implements IIngredientRenderer<T> {

        private final JeiUiElement<T> uiElem;

        public JeiRenderer(JeiUiElement<T> uiElem) {
            this.uiElem = uiElem;
        }

        @Override
        public void render(Minecraft minecraft, int xPosition, int yPosition, @Nullable T ingredient) {
            ScreenRegion ingRegion = uiElem.getIngredientRegion();
            GlStateManager.pushMatrix();
            GlStateManager.translate(xPosition - ingRegion.getX(), yPosition - ingRegion.getY(), 0F);
            GlStateManager.enableBlend();
            uiElem.render(ingredient);
            GlStateManager.popMatrix();
        }

        @Override
        public List<String> getTooltip(Minecraft minecraft, T ingredient, ITooltipFlag tooltipFlag) {
            List<String> tooltip = new ArrayList<>();
            uiElem.getTooltip(ingredient, tooltip, tooltipFlag);
            return tooltip;
        }

    }

}
