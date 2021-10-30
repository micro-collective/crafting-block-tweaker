package xyz.phanta.cbtweaker.integration.jei;

import io.github.phantamanta44.libnine.util.render.TextureRegion;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.integration.jei.render.TextureRegionDrawable;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;

public class MultiBlockRecipeCategory<R, C, D> implements IRecipeCategory<CraftingBlockRecipeWrapper> {

    private final MultiBlockType<R, C, D> mbType;
    private final IDrawable background;

    public MultiBlockRecipeCategory(MultiBlockType<R, C, D> mbType) {
        this.mbType = mbType;
        UiLayout layout = mbType.getUiLayout();
        ScreenRegion machInvRegion = layout.getMachineInventoryRegion();
        TextureRegion bgTex = layout.getBackgroundTexture();
        this.background = new TextureRegionDrawable(bgTex.getTexture().getRegion(
                bgTex.getX() + machInvRegion.getX(), bgTex.getY() + machInvRegion.getY(),
                machInvRegion.getWidth(), machInvRegion.getHeight()));
    }

    @Override
    public String getUid() {
        return CbtMod.MOD_ID + ".multiblock." + mbType.getId();
    }

    @Override
    public String getTitle() {
        return I18n.format(mbType.getTranslationKey());
    }

    @Override
    public String getModName() {
        return CbtJeiPlugin.getLocalizedCbtModName();
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout,
                          CraftingBlockRecipeWrapper recipeWrapper, IIngredients ingredients) {
        recipeWrapper.layOutRecipe(recipeLayout);
    }

}
