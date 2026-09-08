package st.evening.mc.cbtweaker.compat.jei.ui

import mezz.jei.api.ingredients.IIngredientRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.util.ITooltipFlag
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.render.RenderingHelper

@ClientSide.Physical
interface JeiUiElement<T : Any> {
    val jeiIngredient: JeiIngredient<T>?
        get() = null

    val ingredientRegion: IntRectangle

    fun drawElement(ingredient: T?, partialTicks: Float)

    fun getTooltip(ingredient: T?, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        if (ingredient != null) {
            jeiIngredient?.getTooltip(ingredient, tooltip, tooltipFlags)
        }
    }

    class JeiRenderer<T : Any>(private val element: JeiUiElement<T>) : IIngredientRenderer<T> {
        override fun render(minecraft: Minecraft, xPosition: Int, yPosition: Int, ingredient: T?) {
            RenderingHelper.pushMatrix {
                val region = element.ingredientRegion
                RenderingHelper.translate2(xPosition - region.posX, yPosition - region.posY)
                GlStateManager.enableBlend()
                element.drawElement(ingredient, minecraft.renderPartialTicks)
            }
        }

        override fun getTooltip(minecraft: Minecraft, ingredient: T, tooltipFlag: ITooltipFlag): List<String> {
            val tooltip = mutableListOf<String>()
            element.getTooltip(ingredient, tooltip, tooltipFlag)
            return tooltip
        }
    }
}
