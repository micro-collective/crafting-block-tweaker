package xyz.phanta.cbtweaker.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import xyz.phanta.cbtweaker.common.CraftingBlockType;

@ZenRegister
@ZenClass("mods.cbtweaker.CraftingBlock")
public class CrTCraftingBlock<R, C, D> {

    private final CraftingBlockType<R, C, D> cbType;

    public CrTCraftingBlock(CraftingBlockType<R, C, D> cbType) {
        this.cbType = cbType;
    }

    @ZenMethod
    public CrTJsonObjectBuilder addRecipe(String recipeId, CrTJsonObjectBuilder spec) {
        CrTJsonObjectBuilder builder = new CrTJsonObjectBuilder();
        cbType.getRecipeLogic()
                .loadRecipe(recipeId, spec.getJsonObject(), cbType.getRecipeConfig(), cbType.getRecipeDatabase());
        return builder;
    }

}
