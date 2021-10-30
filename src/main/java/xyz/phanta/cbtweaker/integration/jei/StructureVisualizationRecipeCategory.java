package xyz.phanta.cbtweaker.integration.jei;

import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.integration.jei.render.TextureRegionDrawable;

public class StructureVisualizationRecipeCategory implements IRecipeCategory<StructureRecipeWrapper> {

    @Override
    public String getUid() {
        return CbtMod.MOD_ID + ".structure_visualization";
    }

    @Override
    public String getTitle() {
        return I18n.format(CbtLang.JEI_CATEGORY_MULTIBLOCK_STRUCTURE);
    }

    @Override
    public String getModName() {
        return CbtJeiPlugin.getLocalizedCbtModName();
    }

    @Override
    public IDrawable getBackground() {
        return new TextureRegionDrawable(CbtTextureResources.GUI_MB_VIS);
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, StructureRecipeWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup itemGroup = recipeLayout.getItemStacks();
        itemGroup.init(0, false, 0, 92);
        itemGroup.set(ingredients);
    }

}
