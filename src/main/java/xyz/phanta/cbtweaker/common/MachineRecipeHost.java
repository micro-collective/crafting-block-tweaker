package xyz.phanta.cbtweaker.common;

import xyz.phanta.cbtweaker.recipe.RecipeLogic;

public interface MachineRecipeHost<R, C, D, S, E> {

    RecipeLogic<R, C, D, S, E> getRecipeLogic();

    C getRecipeConfig();

    D getRecipeDatabase();

    void setDirty();

}
