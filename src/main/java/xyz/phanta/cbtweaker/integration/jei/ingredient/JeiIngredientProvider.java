package xyz.phanta.cbtweaker.integration.jei.ingredient;

import java.util.Collection;

public interface JeiIngredientProvider<JA> {

    boolean populateJei(JA acc);

    Collection<JeiIngredient<?>> getJeiIngredients();

}
