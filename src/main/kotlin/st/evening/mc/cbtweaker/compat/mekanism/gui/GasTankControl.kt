package st.evening.mc.cbtweaker.compat.mekanism.gui

import mekanism.api.gas.GasStack
import st.evening.mc.cbtweaker.compat.mekanism.gas.SingleGasTank
import st.evening.mc.cbtweaker.gui.element.TankControl
import st.evening.mc.cbtweaker.util.gui.SpriteBarRenderer
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Strong
class GasTankControl(
    uiIndex: Int,
    tank: SingleGasTank,
    bgTexture: GuiDrawable,
    fgOffsetX: Int,
    fgOffsetY: Int,
    fgWidth: Int,
    fgHeight: Int,
    interactive: Boolean
) : TankControl<SingleGasTank, GasStack>(
    uiIndex, tank, bgTexture, fgOffsetX, fgOffsetY, fgWidth, fgHeight, interactive
) {
    override fun getContents(tank: SingleGasTank): GasStack? = tank.gas

    override fun getCapacity(tank: SingleGasTank): Int = tank.maxGas

    override fun getAmount(contents: GasStack): Int = contents.amount

    override fun createBarRenderer(): SpriteBarRenderer<GasStack> = GasBarRenderer()

    override fun getLocalizedName(contents: GasStack): String = contents.gas.localizedName
}
