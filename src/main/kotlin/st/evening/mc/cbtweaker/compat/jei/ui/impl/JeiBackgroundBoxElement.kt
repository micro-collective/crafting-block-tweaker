package st.evening.mc.cbtweaker.compat.jei.ui.impl

import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle

@ClientSide.Physical
class JeiBackgroundBoxElement(override val ingredientRegion: IntRectangle, private val padding: Int) :
    JeiUiElement<Nothing> {

    override fun drawElement(ingredient: Nothing?, partialTicks: Float) {
        CbtGuiResources.ITEM_SLOT.draw(
            partialTicks,
            ingredientRegion.posX - padding,
            ingredientRegion.posY - padding,
            ingredientRegion.width + padding + padding,
            ingredientRegion.height + padding + padding
        )
    }
}
