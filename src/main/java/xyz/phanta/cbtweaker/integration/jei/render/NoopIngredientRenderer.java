package xyz.phanta.cbtweaker.integration.jei.render;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class NoopIngredientRenderer implements IIngredientRenderer<Object> {

    private static final NoopIngredientRenderer INSTANCE = new NoopIngredientRenderer();

    @SuppressWarnings("unchecked")
    public static <T> IIngredientRenderer<T> getInstance() {
        return (IIngredientRenderer<T>)INSTANCE;
    }

    private NoopIngredientRenderer() {
        // NO-OP
    }

    @Override
    public void render(Minecraft minecraft, int xPosition, int yPosition, @Nullable Object ingredient) {
        // NO-OP
    }

    @Override
    public List<String> getTooltip(Minecraft minecraft, Object ingredient, ITooltipFlag tooltipFlag) {
        return Collections.emptyList();
    }

}
