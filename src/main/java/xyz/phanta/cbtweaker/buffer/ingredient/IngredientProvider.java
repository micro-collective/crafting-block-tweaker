package xyz.phanta.cbtweaker.buffer.ingredient;

import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredientProvider;

import java.util.function.Supplier;

public interface IngredientProvider<A, JA> extends JeiIngredientProvider<JA> {

    default void insertPeriodic(Supplier<A> acc) {
        // NO-OP
    }

    default boolean insertFinal(Supplier<A> acc) {
        return true;
    }

}
