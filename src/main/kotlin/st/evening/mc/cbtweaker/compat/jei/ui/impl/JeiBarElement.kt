package st.evening.mc.cbtweaker.compat.jei.ui.impl

import mezz.jei.api.IGuiHelper
import mezz.jei.api.gui.ITickTimer
import net.minecraft.client.util.ITooltipFlag
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.util.CbtClientHelper
import st.evening.mc.cbtweaker.util.gui.BarDrawData
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
    val fgOffsetX: Int,
    val fgOffsetY: Int,
    val barOrientation: DrawOrientation
) : JeiUiElement<T> {
    constructor(posX: Int, posY: Int, bar: BarDrawData) :
        this(posX, posY, bar.bgTexture.drawable, bar.fgTexture.drawable, bar.fgOffsetX, bar.fgOffsetY, bar.orientation)

    abstract fun getBarFill(): Float

    override val ingredientRegion: IntRectangle = Rect2i(posX, posY, barBg.width, barBg.height)

    override fun drawElement(ingredient: T?, partialTicks: Float) {
        barBg.drawFullSize(partialTicks, posX, posY)
        barFg.drawFullSizeAsProgress(partialTicks, posX + fgOffsetX, posY + fgOffsetY, barOrientation, getBarFill())
    }
}

@ClientSide.Physical
class JeiProgressBarElement(
    posX: Int,
    posY: Int,
    bar: BarDrawData,
    private val duration: Int,
    guiHelper: IGuiHelper,
    reverse: Boolean = false
) : JeiBarElement<Nothing>(posX, posY, bar) {
    private val ticker: ITickTimer = duration.coerceAtLeast(4).let { guiHelper.createTickTimer(it, it, reverse) }

    override fun getBarFill(): Float = ticker.value / ticker.maxValue.toFloat()

    override fun getTooltip(ingredient: Nothing?, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += CbtClientHelper.formatTickTime(duration)
    }
}
