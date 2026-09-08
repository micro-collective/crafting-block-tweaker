package st.evening.mc.cbtweaker.gui.element

import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.resources.I18n
import net.minecraft.init.SoundEvents
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.gui.inventory.assertWindowId
import st.evening.mc.cbtweaker.network.C2SSetHatchAutoExporting
import st.evening.mc.cbtweaker.util.component.AutoExportHandler
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.ClickResult
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.OffsetBox
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.containsPoint
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

@ClientSide.Strong
class HatchAutoExportControl(private val exportHandler: AutoExportHandler<*>) : AbstractGuiElement() {
    override val contentWidth: Int
        get() = CbtGuiResources.AUTO_EXPORT_DISABLED.width
    override val contentHeight: Int
        get() = CbtGuiResources.AUTO_EXPORT_DISABLED.height

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
        override val width: Int
            get() = contentWidth
        override val height: Int
            get() = contentHeight

        override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            CbtGuiResources.autoExport(exportHandler.autoExporting).drawFullSize(partialTicks, posX, posY)
        }

        override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
            if (!containsPoint(mouseX, mouseY)) return false
            context.gui.drawHoveringText(
                I18n.format(
                    CbtLang.TOOLTIP_AUTO_EXPORT,
                    "${TextFormatting.GRAY}${I18n.format(CbtLang.tooltipEnabledDisabled(exportHandler.autoExporting))}"
                ),
                mouseX, mouseY
            )
            return true
        }

        override fun onMouseClick(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): ClickResult {
            if (mouseButton != 0 && mouseButton != 1) return ClickResult.Ignore
            CbTweaker.defns.c2sSetHatchAutoExporting.sendToServer(
                C2SSetHatchAutoExporting(context.assertWindowId(), !exportHandler.autoExporting)
            )
            context.gui.mc.soundHandler.playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F)
            )
            return ClickResult.Consume
        }
    }
}

class HatchAutoExportControlElement(private val exportHandler: AutoExportHandler<*>) : UiElement {
    @ClientSide.Strong
    override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
        layout.addChild(
            OffsetBox(wrapper.wrap(uiIndex, HatchAutoExportControl(exportHandler)), -11, -11),
            DrawAlignment.END,
            DrawAlignment.START
        )
    }
}
