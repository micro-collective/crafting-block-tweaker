package st.evening.mc.cbtweaker.gui.element

import net.minecraft.client.resources.I18n
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.drawable.drawFullSizeAsProgress
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiPart
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.containsPoint
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation
import st.evening.mc.prelude.api.util.text.toStringPercentage
import st.evening.mc.prelude.api.util.text.toStringSI

@ClientSide.Physical
abstract class BarControl(
    private val barBackground: GuiDrawable,
    private val barForeground: GuiSamplable,
    private val orientation: DrawOrientation,
    private val barOffsetX: Int,
    private val barOffsetY: Int
) : AbstractGuiElement() {
    override val contentWidth: Int
        get() = barBackground.width
    override val contentHeight: Int
        get() = barBackground.height

    abstract fun getFillFraction(): Float

    abstract fun getTooltipText(): List<String>?

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    class Progress(
        barBackground: GuiDrawable,
        barForeground: GuiSamplable,
        private val getValue: () -> Int,
        private val getMax: () -> Int,
        orientation: DrawOrientation,
        barOffsetX: Int,
        barOffsetY: Int
    ) : BarControl(barBackground, barForeground, orientation, barOffsetX, barOffsetY) {
        override fun getFillFraction(): Float {
            val max = getMax()
            return if (max <= 0) 0F else (getValue() / max.toFloat())
        }

        override fun getTooltipText(): List<String> {
            val progress = getFillFraction()
            return listOf(if (progress <= 0F) I18n.format(CbtLang.TOOLTIP_IDLE) else progress.toStringPercentage())
        }
    }

    class IntTank(
        barBackground: GuiDrawable,
        barForeground: GuiSamplable,
        private val getAmount: () -> Int,
        capacity: Int,
        private val unitName: String,
        orientation: DrawOrientation,
        barOffsetX: Int,
        barOffsetY: Int
    ) : BarControl(barBackground, barForeground, orientation, barOffsetX, barOffsetY) {
        private val capacityString: String = capacity.toStringSI(unitName)
        private val capacityFloat: Float = capacity.toFloat()

        override fun getFillFraction(): Float = getAmount() / capacityFloat

        override fun getTooltipText(): List<String> = listOf("${getAmount().toStringSI(unitName)} / $capacityString")
    }

    class LongTank(
        barBackground: GuiDrawable,
        barForeground: GuiSamplable,
        private val getAmount: () -> Long,
        capacity: Long,
        private val unitName: String,
        orientation: DrawOrientation,
        barOffsetX: Int,
        barOffsetY: Int
    ) : BarControl(barBackground, barForeground, orientation, barOffsetX, barOffsetY) {
        private val capacityString: String = capacity.toStringSI(unitName)
        private val capacityFloat: Float = capacity.toFloat()

        override fun getFillFraction(): Float = getAmount() / capacityFloat

        override fun getTooltipText(): List<String> = listOf("${getAmount().toStringSI(unitName)} / $capacityString")
    }

    class FloatTank(
        barBackground: GuiDrawable,
        barForeground: GuiSamplable,
        private val getAmount: () -> Float,
        private val capacity: Float,
        private val unitName: String,
        orientation: DrawOrientation,
        barOffsetX: Int,
        barOffsetY: Int
    ) : BarControl(barBackground, barForeground, orientation, barOffsetX, barOffsetY) {
        private val capacityString: String = capacity.toStringSI(unitName)

        override fun getFillFraction(): Float = getAmount() / capacity

        override fun getTooltipText(): List<String> = listOf("${getAmount().toStringSI(unitName)} / $capacityString")
    }

    class DoubleTank(
        barBackground: GuiDrawable,
        barForeground: GuiSamplable,
        private val getAmount: () -> Double,
        private val capacity: Double,
        private val unitName: String,
        orientation: DrawOrientation,
        barOffsetX: Int,
        barOffsetY: Int
    ) : BarControl(barBackground, barForeground, orientation, barOffsetX, barOffsetY) {
        private val capacityString: String = capacity.toStringSI(unitName)

        override fun getFillFraction(): Float = (getAmount() / capacity).toFloat()

        override fun getTooltipText(): List<String> = listOf("${getAmount().toStringSI(unitName)} / $capacityString")
    }

    private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
        override val width: Int
            get() = contentWidth
        override val height: Int
            get() = contentHeight

        override fun drawBackground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            barBackground.drawFullSize(partialTicks, posX, posY)
        }

        override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            barForeground.drawFullSizeAsProgress(
                partialTicks,
                posX + barOffsetX,
                posY + barOffsetY,
                orientation,
                getFillFraction()
            )
        }

        override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
            if (!containsPoint(mouseX, mouseY)) return false
            getTooltipText()?.let { context.gui.drawHoveringText(it, mouseX, mouseY) }
            return true
        }
    }
}
