package st.evening.mc.cbtweaker.compat.jei.structure

import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IRecipeWrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.item.ItemStack
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerBlock
import st.evening.mc.cbtweaker.structure.StructureMatcher
import st.evening.mc.cbtweaker.structure.VisualizationRenderer
import st.evening.mc.prelude.api.util.game.ClientHelper
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

@ClientSide.Physical
class StructureRecipeWrapper(private val mbCtrlBlock: MultiBlockControllerBlock, structMatcher: StructureMatcher<*>) :
    IRecipeWrapper {
    private val visRenderer: VisualizationRenderer = VisualizationRenderer(structMatcher)

    override fun getIngredients(ingredients: IIngredients) {
        ingredients.setOutput(VanillaTypes.ITEM, ItemStack(mbCtrlBlock))
    }

    override fun drawInfo(minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int) {
        visRenderer.handleMouseMovement(mouseX, mouseY)
        visRenderer.render(0, 0, mouseX, mouseY)
        RenderingHelper.pushMatrix {
            GlStateManager.scale(0.75F, 0.75F, 0.75F)
            minecraft.fontRenderer.drawString(
                I18n.format(mbCtrlBlock.mbType.translationKey), 28, 131, GuiRenderHelper.DEFAULT_TEXT_COLOUR
            )
        }
    }

    override fun getTooltipStrings(mouseX: Int, mouseY: Int): List<String> =
        mutableListOf<String>().also { visRenderer.getTooltip(it, mouseX, mouseY, ClientHelper.tooltipFlags) }

    override fun handleClick(minecraft: Minecraft, mouseX: Int, mouseY: Int, mouseButton: Int): Boolean =
        visRenderer.handleClick(mouseX, mouseY, mouseButton)
}
