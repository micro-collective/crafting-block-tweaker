package st.evening.mc.cbtweaker.compat.jei.structure

import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe.IRecipeCategory
import net.minecraft.client.resources.I18n
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.compat.jei.render.JeiDrawableWrapper
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.cbtweaker.util.CbtClientHelper
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
class StructureVisualizationRecipeCategory : IRecipeCategory<StructureRecipeWrapper> {
    private val bg: JeiDrawableWrapper = JeiDrawableWrapper(CbtGuiResources.GUI_MB_VIS)

    override fun getUid(): String = "${CbtConsts.MOD_ID}.structure_visualization"

    override fun getTitle(): String = I18n.format(CbtLang.JEI_CATEGORY_MULTIBLOCK_STRUCTURE)

    override fun getModName(): String = CbtClientHelper.getLocalizedModName()

    override fun getBackground(): IDrawable = bg

    override fun setRecipe(
        recipeLayout: IRecipeLayout,
        recipeWrapper: StructureRecipeWrapper,
        ingredients: IIngredients
    ) {
        recipeLayout.itemStacks.run {
            init(0, false, 0, 92)
            set(ingredients)
        }
    }
}
