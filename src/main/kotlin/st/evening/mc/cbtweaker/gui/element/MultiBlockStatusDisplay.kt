package st.evening.mc.cbtweaker.gui.element

import net.minecraft.client.resources.I18n
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerTileEntity
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.OffsetBox
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.containsPoint
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

@ClientSide.Physical
class MultiBlockStatusDisplay(private val mbCtrl: MultiBlockControllerTileEntity) : AbstractGuiElement() {
    override val contentWidth: Int
        get() = CbtGuiResources.INFO_DISPLAY_OFF.width
    override val contentHeight: Int
        get() = CbtGuiResources.INFO_DISPLAY_OFF.height

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
        override val width: Int
            get() = contentWidth
        override val height: Int
            get() = contentHeight

        override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            CbtGuiResources.infoDisplay(mbCtrl.assembled).drawFullSize(partialTicks, posX, posY)
        }

        override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
            if (!containsPoint(mouseX, mouseY)) return false
            context.gui.drawHoveringText(
                listOf(
                    if (mbCtrl.assembled) {
                        "${TextFormatting.RED}${I18n.format(CbtLang.TOOLTIP_MB_NOT_ASSEMBLED)}"
                    } else {
                        "${TextFormatting.GREEN}${I18n.format(CbtLang.TOOLTIP_MB_ASSEMBLED)}"
                    },
                    "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_MB_VISUALIZE)}"
                ),
                mouseX, mouseY
            )
            return true
        }
    }
}

class MultiBlockStatusDisplayElement(private val mbCtrl: MultiBlockControllerTileEntity) : UiElement {
    @ClientSide.Strong
    override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
        layout.addChild(
            OffsetBox(wrapper.wrap(uiIndex, MultiBlockStatusDisplay(mbCtrl)), -11, -11),
            DrawAlignment.END,
            DrawAlignment.START
        )
    }
}
