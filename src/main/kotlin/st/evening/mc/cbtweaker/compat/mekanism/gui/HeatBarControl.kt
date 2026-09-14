package st.evening.mc.cbtweaker.compat.mekanism.gui

import mekanism.common.util.UnitDisplayUtils
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.cbtweaker.gui.element.BarControl
import st.evening.mc.cbtweaker.util.gui.BarDrawData
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.RequireMod
import st.evening.mc.prelude.api.util.text.toStringSI

@RequireMod(MekanismCompat.MOD_ID)
@ClientSide.Physical
class HeatBarControl(bar: BarDrawData, private val getTemperature: () -> Double, private val maxTemperature: Double) :
    BarControl(bar) {

    override fun getFillFraction(): Float = (getTemperature() / maxTemperature).toFloat().coerceAtMost(1F)

    override fun getTooltipText(): List<String> =
        listOf(UnitDisplayUtils.TemperatureUnit.AMBIENT.convertToK(getTemperature(), true).toStringSI("K"))
}
