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
abstract class JeiBarElement<T : Any>(val posX: Int, val posY: Int) : JeiUiElement<T> {
    abstract val barBg: GuiDrawable
    abstract val barFg: GuiSamplable
    abstract val barOffsetX: Int
    abstract val barOffsetY: Int
    abstract val barOrientation: DrawOrientation

    abstract fun getBarFill(): Float

    override val ingredientRegion: IntRectangle = Rect2i(posX, posY, posX + barBg.width, posY + barBg.height)

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
    override val barBg: GuiDrawable,
    override val barFg: GuiSamplable,
    override val barOffsetX: Int,
    override val barOffsetY: Int,
    override val barOrientation: DrawOrientation,
    guiHelper: IGuiHelper
) : JeiBarElement<Nothing>(posX, posY) {
    private val ticker: ITickTimer = duration.coerceAtLeast(4).let { guiHelper.createTickTimer(it, it, false) }

    override fun getBarFill(): Float = ticker.value / ticker.maxValue.toFloat()

    override fun getTooltip(ingredient: Nothing?, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += CbtClientHelper.formatTickTime(duration)
    }
}
