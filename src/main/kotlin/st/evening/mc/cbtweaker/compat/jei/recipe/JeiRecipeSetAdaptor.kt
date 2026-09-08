package st.evening.mc.cbtweaker.compat.jei.recipe

import mezz.jei.api.IJeiHelpers
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle

@ClientSide.Physical
interface JeiUi {
    fun addJeiUiElement(uiElem: JeiUiElement<*>)
}

interface JeiRecipeSetAdaptor<R> {
    val jeiDiscriminator: String?

    @ClientSide.Physical
    fun getJeiCategoryName(): String

    @ClientSide.Physical
    fun getJeiBackground(): GuiDrawable

    @ClientSide.Physical
    fun addJeiUiElements(recipe: R, container: JeiUi, region: IntRectangle, jeiHelpers: IJeiHelpers)
}
