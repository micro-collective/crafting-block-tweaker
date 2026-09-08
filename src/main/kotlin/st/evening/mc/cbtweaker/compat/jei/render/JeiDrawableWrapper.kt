package st.evening.mc.cbtweaker.compat.jei.render

import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IDrawableStatic
import net.minecraft.client.Minecraft
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
open class JeiDrawableWrapper(private val drawable: GuiDrawable) : IDrawable {
    override fun getWidth(): Int = drawable.width

    override fun getHeight(): Int = drawable.height

    override fun draw(minecraft: Minecraft, xOffset: Int, yOffset: Int) {
        drawable.drawFullSize(minecraft.renderPartialTicks, xOffset, yOffset)
    }
}

@ClientSide.Physical
class JeiSamplableWrapper(private val samplable: GuiSamplable) : JeiDrawableWrapper(samplable), IDrawableStatic {
    override fun draw(
        minecraft: Minecraft, xOffset: Int, yOffset: Int,
        maskTop: Int, maskBottom: Int, maskLeft: Int, maskRight: Int
    ) {
        val width = samplable.width
        val height = samplable.height
        samplable.drawPart(
            minecraft.renderPartialTicks,
            xOffset + maskLeft,
            yOffset + maskTop,
            width - maskLeft - maskRight,
            height - maskTop - maskBottom,
            maskLeft / width.toFloat(),
            maskTop / height.toFloat(),
            maskRight / width.toFloat(),
            maskBottom / height.toFloat()
        )
    }
}
