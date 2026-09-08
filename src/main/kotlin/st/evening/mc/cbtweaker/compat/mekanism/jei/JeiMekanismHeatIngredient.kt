package st.evening.mc.cbtweaker.compat.mekanism.jei

import net.minecraft.client.util.ITooltipFlag
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.text.toStringSI

class JeiMekanismHeatIngredient(val amount: Double, val unitName: String, override val role: JeiIngredient.Role) :
    JeiIngredient<Double> {

    constructor(amount: Double, isRate: Boolean, role: JeiIngredient.Role) :
        this(amount, if (isRate) "J/t" else "J", role) // are these actually joules...?

    override fun getIngredients(): List<Double> = listOf(amount)

    @ClientSide.Physical
    override fun drawIcon(x: Int, y: Int, ingredient: Double, partialTicks: Float) {
        CbtGuiResources.MEKANISM_ICON_HEAT.drawFullSize(partialTicks, x, y)
    }

    @ClientSide.Physical
    override fun getTooltip(ingredient: Double, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += amount.toStringSI(unitName)
    }
}
