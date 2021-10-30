package xyz.phanta.cbtweaker.integration.jei;

import io.github.phantamanta44.libnine.util.render.RenderUtils;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerBlock;
import xyz.phanta.cbtweaker.structure.StructureMatcher;
import xyz.phanta.cbtweaker.structure.VisualizationRenderer;

import java.util.ArrayList;
import java.util.List;

public class StructureRecipeWrapper implements IRecipeWrapper {

    private final MultiBlockControllerBlock mbCtrlBlock;
    private final VisualizationRenderer visRenderer;

    public StructureRecipeWrapper(MultiBlockControllerBlock mbCtrlBlock, StructureMatcher structMatcher) {
        this.mbCtrlBlock = mbCtrlBlock;
        this.visRenderer = new VisualizationRenderer(structMatcher);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setOutput(VanillaTypes.ITEM, new ItemStack(mbCtrlBlock));
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        visRenderer.handleMouseMovement(mouseX, mouseY);
        visRenderer.render(0, 0, mouseX, mouseY);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.75F, 0.75F, 0.75F);
        minecraft.fontRenderer.drawString(
                I18n.format(mbCtrlBlock.getMultiBlockType().getTranslationKey()), 28, 131, 0x404040);
        GlStateManager.popMatrix();
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        List<String> tooltip = new ArrayList<>();
        visRenderer.getTooltip(tooltip, mouseX, mouseY, RenderUtils.getTooltipFlags());
        return tooltip;
    }

    @Override
    public boolean handleClick(Minecraft minecraft, int mouseX, int mouseY, int mouseButton) {
        return visRenderer.handleClick(mouseX, mouseY, mouseButton);
    }

}
