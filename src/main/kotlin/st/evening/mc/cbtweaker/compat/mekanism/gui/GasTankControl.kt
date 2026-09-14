package st.evening.mc.cbtweaker.compat.mekanism.gui

import mekanism.api.gas.GasStack
import net.minecraft.client.resources.I18n
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.cbtweaker.compat.mekanism.gas.SingleGasTank
import st.evening.mc.cbtweaker.gui.element.TankControl
import st.evening.mc.cbtweaker.util.gui.SpriteBarRenderer
import st.evening.mc.cbtweaker.util.gui.TankDrawData
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.RequireMod

@RequireMod(MekanismCompat.MOD_ID)
@ClientSide.Strong
class GasTankControl(uiIndex: Int, tank: SingleGasTank, uiTank: TankDrawData, interactive: Boolean) :
    TankControl<SingleGasTank, GasStack>(uiIndex, tank, uiTank, interactive) {

    override fun getContents(tank: SingleGasTank): GasStack? = tank.gas

    override fun getCapacity(tank: SingleGasTank): Int = tank.maxGas

    override fun getAmount(contents: GasStack): Int = contents.amount

    override fun createBarRenderer(): SpriteBarRenderer<GasStack> = GasBarRenderer()

    override fun getLocalizedName(contents: GasStack): String = contents.gas.localizedName

    override fun getLocalizedEmptyText(): String = I18n.format(CbtLang.TOOLTIP_EMPTY_GAS)
}
