package xyz.phanta.cbtweaker.buffer.ingredient;

import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredientProvider;

import java.util.function.Supplier;

public interface IngredientMatcher<A, JA> extends JeiIngredientProvider<JA> {

    default boolean consumeInitial(Supplier<A> acc, float consumeFactor) {
        return true;
    }

    default boolean consumePeriodic(Supplier<A> acc, float consumeFactor) {
        return true;
    }

}
