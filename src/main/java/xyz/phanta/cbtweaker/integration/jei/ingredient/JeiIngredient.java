package xyz.phanta.cbtweaker.integration.jei.ingredient;

import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.client.util.ITooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

public interface JeiIngredient<T> {

    List<T> getIngredients();

    @Nullable
    default IIngredientType<T> getJeiIngredientType() {
        return null;
    }

    Role getRole();

    void renderIcon(int x, int y, T ingredient);

    void getTooltip(T ingredient, List<String> tooltip, ITooltipFlag tooltipFlags);

    enum Role {
        INPUT, OUTPUT
    }

}
