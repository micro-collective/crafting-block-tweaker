package st.evening.mc.cbtweaker.compat.mekanism.gui

import mekanism.common.util.UnitDisplayUtils
import st.evening.mc.cbtweaker.gui.element.BarControl
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation
import st.evening.mc.prelude.api.util.text.toStringSI

@ClientSide.Physical
class HeatBarControl(
    barBackground: GuiDrawable,
    barForeground: GuiSamplable,
    private val getTemperature: () -> Double,
    private val maxTemperature: Double,
    orientation: DrawOrientation,
    barOffsetX: Int,
    barOffsetY: Int
) : BarControl(barBackground, barForeground, orientation, barOffsetX, barOffsetY) {
    override fun getFillFraction(): Float = (getTemperature() / maxTemperature).toFloat().coerceAtMost(1F)

    override fun getTooltipText(): List<String> =
        listOf(UnitDisplayUtils.TemperatureUnit.AMBIENT.convertToK(getTemperature(), true).toStringSI("K"))
}
