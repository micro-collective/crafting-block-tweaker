package st.evening.mc.cbtweaker.compat.jei.ingredient.impl

import net.minecraft.client.util.ITooltipFlag
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.util.game.ClientSide

class JeiForgeEnergyIngredient(
    val amount: Int,
    val unitName: String,
    override val role: JeiIngredient.Role
) : JeiIngredient<Int> {
    override fun getIngredients(): List<Int> = listOf(amount)

    @ClientSide.Physical
    override fun drawIcon(x: Int, y: Int, ingredient: Int, partialTicks: Float) {
        CbtGuiResources.ICON_ENERGY.drawFullSize(partialTicks, x, y)
    }

    @ClientSide.Physical
    override fun getTooltip(ingredient: Int, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += "%,d %s".format(ingredient, unitName)
    }
}
