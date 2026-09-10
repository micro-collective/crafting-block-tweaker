package st.evening.mc.cbtweaker.compat.jei.ui.impl

import mezz.jei.api.IGuiHelper
import mezz.jei.api.gui.ITickTimer
import net.minecraft.client.util.ITooltipFlag
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.util.CbtClientHelper
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.drawable.drawFullSizeAsProgress
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation

@ClientSide.Physical
abstract class JeiBarElement<T : Any>(
    val posX: Int,
    val posY: Int,
    val barBg: GuiDrawable,
    val barFg: GuiSamplable,
    val barOffsetX: Int,
    val barOffsetY: Int,
    val barOrientation: DrawOrientation
) : JeiUiElement<T> {
    abstract fun getBarFill(): Float

    override val ingredientRegion: IntRectangle = Rect2i(posX, posY, barBg.width, barBg.height)

    override fun drawElement(ingredient: T?, partialTicks: Float) {
        barBg.drawFullSize(partialTicks, posX, posY)
        barFg.drawFullSizeAsProgress(partialTicks, posX + barOffsetX, posY + barOffsetY, barOrientation, getBarFill())
    }
}

@ClientSide.Physical
class JeiProgressBarElement(
    posX: Int,
    posY: Int,
    private val duration: Int,
    barBg: GuiDrawable,
    barFg: GuiSamplable,
    barOffsetX: Int,
    barOffsetY: Int,
    barOrientation: DrawOrientation,
    guiHelper: IGuiHelper
) : JeiBarElement<Nothing>(posX, posY, barBg, barFg, barOffsetX, barOffsetY, barOrientation) {
    private val ticker: ITickTimer = duration.coerceAtLeast(4).let { guiHelper.createTickTimer(it, it, false) }

    override fun getBarFill(): Float = ticker.value / ticker.maxValue.toFloat()

    override fun getTooltip(ingredient: Nothing?, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += CbtClientHelper.formatTickTime(duration)
    }
}
