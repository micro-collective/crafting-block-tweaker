package xyz.phanta.cbtweaker.common;

import xyz.phanta.cbtweaker.recipe.RecipeLogic;

public interface CraftingBlockType<R, C, D> {

    RecipeLogic<R, C, D, ?, ?> getRecipeLogic();

    C getRecipeConfig();

    D getRecipeDatabase();

}
