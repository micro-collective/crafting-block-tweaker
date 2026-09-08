package st.evening.mc.cbtweaker.compat.mekanism.jei

import mekanism.common.util.UnitDisplayUtils
import net.minecraft.client.util.ITooltipFlag
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.text.toStringSI

class JeiMekanismTemperatureIngredient(val amount: Double, override val role: JeiIngredient.Role) :
    JeiIngredient<Double> {

    override fun getIngredients(): List<Double> = listOf(amount)

    @ClientSide.Physical
    override fun drawIcon(x: Int, y: Int, ingredient: Double, partialTicks: Float) {
        CbtGuiResources.MEKANISM_ICON_HEAT.drawFullSize(partialTicks, x, y)
    }

    @ClientSide.Physical
    override fun getTooltip(ingredient: Double, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += UnitDisplayUtils.TemperatureUnit.AMBIENT.convertToK(amount, true).toStringSI("K")
    }
}
