package st.evening.mc.cbtweaker.compat.mekanism.jei

import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.RequireMod
import st.evening.mc.prelude.api.util.text.toStringPercentage
import st.evening.mc.prelude.api.util.text.toStringSI

@RequireMod(MekanismCompat.MOD_ID)
class JeiMekanismHeatIngredient(
    val amount: Double,
    val unitName: String,
    override val role: JeiIngredient.Role,
    val chance: Float = 1F
) : JeiIngredient<Double> {
    constructor(amount: Double, isRate: Boolean, role: JeiIngredient.Role, chance: Float = 1F) :
        this(amount, if (isRate) "J/t" else "J", role, chance) // are these actually joules...?

    override fun getIngredients(): List<Double> = listOf(amount)

    @ClientSide.Physical
    override fun drawIcon(x: Int, y: Int, ingredient: Double, partialTicks: Float) {
        CbtGuiResources.MEKANISM_ICON_HEAT.drawFullSize(partialTicks, x, y)
    }

    @ClientSide.Physical
    override fun getTooltip(ingredient: Double, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += amount.toStringSI(unitName)
        if (chance < 1F) {
            tooltip += "${TextFormatting.GOLD}(${chance.toStringPercentage()})"
        }
    }
}
