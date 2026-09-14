package st.evening.mc.cbtweaker.gui.element

import net.minecraft.client.resources.I18n
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.util.gui.BarDrawData
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
    private val fgOffsetX: Int,
    private val fgOffsetY: Int
) : AbstractGuiElement() {
    constructor(bar: BarDrawData) :
        this(bar.bgTexture.drawable, bar.fgTexture.drawable, bar.orientation, bar.fgOffsetX, bar.fgOffsetY)

    override val contentWidth: Int
        get() = barBackground.width
    override val contentHeight: Int
        get() = barBackground.height

    abstract fun getFillFraction(): Float

    abstract fun getTooltipText(): List<String>?

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    class Progress(bar: BarDrawData, private val getValue: () -> Int, private val getMax: () -> Int) : BarControl(bar) {
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
        bar: BarDrawData,
        private val getAmount: () -> Int,
        capacity: Int,
        private val unitName: String
    ) : BarControl(bar) {
        private val capacityString: String = capacity.toStringSI(unitName)
        private val capacityFloat: Float = capacity.toFloat()

        override fun getFillFraction(): Float = getAmount() / capacityFloat

        override fun getTooltipText(): List<String> = listOf("${getAmount().toStringSI(unitName)} / $capacityString")
    }

    class LongTank(
        bar: BarDrawData,
        private val getAmount: () -> Long,
        capacity: Long,
        private val unitName: String
    ) : BarControl(bar) {
        private val capacityString: String = capacity.toStringSI(unitName)
        private val capacityFloat: Float = capacity.toFloat()

        override fun getFillFraction(): Float = getAmount() / capacityFloat

        override fun getTooltipText(): List<String> = listOf("${getAmount().toStringSI(unitName)} / $capacityString")
    }

    class FloatTank(
        bar: BarDrawData,
        private val getAmount: () -> Float,
        private val capacity: Float,
        private val unitName: String
    ) : BarControl(bar) {
        private val capacityString: String = capacity.toStringSI(unitName)

        override fun getFillFraction(): Float = getAmount() / capacity

        override fun getTooltipText(): List<String> = listOf("${getAmount().toStringSI(unitName)} / $capacityString")
    }

    class DoubleTank(
        bar: BarDrawData,
        private val getAmount: () -> Double,
        private val capacity: Double,
        private val unitName: String
    ) : BarControl(bar) {
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
                posX + fgOffsetX,
                posY + fgOffsetY,
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
