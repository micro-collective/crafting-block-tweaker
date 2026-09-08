package st.evening.mc.cbtweaker.compat.jei.ui.impl

import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiUi
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.util.CbtMathHelper
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntArithmetic
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.math.Vec2i
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

@ClientSide.Physical
class JeiIconElement<T : Any>(
    x: Int,
    y: Int,
    override val jeiIngredient: JeiIngredient<T>?,
    private val bufGroupId: String?
) : JeiUiElement<T> {
    override val ingredientRegion: IntRectangle = Rect2i(x, y, 16, 16)

    override fun drawElement(ingredient: T?, partialTicks: Float) {
        if (ingredient != null) {
            jeiIngredient?.drawIcon(ingredientRegion.posX, ingredientRegion.posY, ingredient, partialTicks)
        }
    }

    override fun getTooltip(ingredient: T?, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        if (ingredient != null) {
            jeiIngredient?.getTooltip(ingredient, tooltip, tooltipFlags)
        }
        bufGroupId?.let {
            tooltip += TextFormatting.AQUA.toString() +
                I18n.format(CbtLang.TOOLTIP_BUFFER_GROUP, "${TextFormatting.WHITE}$it")
        }
    }

    companion object {
        fun layOutIconGroup(
            container: JeiUi,
            contX: Int,
            contY: Int,
            contWidth: Int,
            contHeight: Int,
            alignX: DrawAlignment,
            alignY: DrawAlignment,
            ingredients: Collection<Pair<JeiIngredient<*>, String?>>
        ) {
            val slotPosList = arrayOfNulls<Vec2i>(ingredients.size)
            val dims = CbtMathHelper.layOutSlotGroup(17, 17, slotPosList)
            @Suppress("UNCHECKED_CAST")
            slotPosList as Array<Vec2i>
            val groupX = contX + alignX.computeOffset(IntArithmetic, dims.x, contWidth)
            val groupY = contY + alignY.computeOffset(IntArithmetic, dims.y, contHeight)
            ingredients.forEachIndexed { i, (ing, bufGroupId) ->
                val (slotX, slotY) = slotPosList[i]
                container.addJeiUiElement(JeiIconElement(groupX + slotX, groupY + slotY, ing, bufGroupId))
            }
        }
    }
}
