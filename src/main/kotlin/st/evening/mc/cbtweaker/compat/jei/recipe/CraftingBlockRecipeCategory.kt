package st.evening.mc.cbtweaker.compat.jei.recipe

import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe.IRecipeCategory
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.compat.jei.render.JeiDrawableWrapper
import st.evening.mc.cbtweaker.util.CbtClientHelper
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
class CraftingBlockRecipeCategory<R>(val id: String, val adaptor: JeiRecipeSetAdaptor<R>) :
    IRecipeCategory<CraftingBlockRecipeWrapper> {

    private val bg: IDrawable = JeiDrawableWrapper(adaptor.getJeiBackground())

    override fun getUid(): String =
        adaptor.jeiDiscriminator?.let { "${CbtConsts.MOD_ID}.recipe.$id.$it" } ?: "${CbtConsts.MOD_ID}.recipe.$id"

    override fun getTitle(): String = adaptor.getJeiCategoryName()

    override fun getModName(): String = CbtClientHelper.getLocalizedModName()

    override fun getBackground(): IDrawable = bg

    override fun setRecipe(
        recipeLayout: IRecipeLayout,
        recipeWrapper: CraftingBlockRecipeWrapper,
        ingredients: IIngredients
    ) {
        recipeWrapper.layOutRecipe(recipeLayout)
    }
}
