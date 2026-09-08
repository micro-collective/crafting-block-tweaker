package st.evening.mc.cbtweaker.structure

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.cbtweaker.network.C2SVisualizationLevel
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.util.game.ClientHelper
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.getColouredTooltip
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

@ClientSide.Strong
class VisualizationGui : GuiScreen() {
    companion object {
        private const val VIS_OFF_X: Int = 5
        private const val VIS_OFF_Y: Int = 5

        private fun isOverControllerSlot(mouseX: Int, mouseY: Int): Boolean =
            mouseX >= 1 && mouseY >= 93 && mouseX < 17 && mouseY < 109
    }

    private var windowX: Int = 0
    private var windowY: Int = 0

    override fun initGui() {
        windowX = (width - CbtGuiResources.GUI_MB_VIS_BG.width) / 2
        windowY = (height - CbtGuiResources.GUI_MB_VIS_BG.height) / 2
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val visX = windowX + VIS_OFF_X
        val visY = windowY + VIS_OFF_Y
        val visMouseX = mouseX - visX
        val visMouseY = mouseY - visY
        val slotX = visX + 1
        val slotY = visY + 93

        drawDefaultBackground()
        CbtGuiResources.GUI_MB_VIS_BG.drawFullSize(partialTicks, windowX, windowY)
        CbtGuiResources.GUI_MB_VIS.drawFullSize(partialTicks, visX, visY)

        val state = ClientVisualizationState.getState()
        if (state == null) {
            RenderingHelper.pushMatrix {
                GlStateManager.scale(0.75F, 0.75F, 0.75F)
                drawCenteredString(
                    fontRenderer,
                    I18n.format(CbtLang.GUI_VIS_TOOL_NOT_BOUND),
                    width * 2 / 3,
                    (windowY + VIS_OFF_Y + 44) * 4 / 3,
                    0xFF0000
                )
            }
        } else {
            state.renderer.handleMouseMovement(visMouseX, visMouseY)

            GlStateManager.enableDepth()
            state.renderer.render(visX, visY, visMouseX, visMouseY)

            RenderingHelper.pushMatrix {
                GlStateManager.scale(0.75F, 0.75F, 0.75F)
                drawString(fontRenderer, I18n.format(state.mbType.translationKey), visX + 35, visY + 138, 0x404040)
            }
            GuiRenderHelper.drawItemAndOverlay(slotX, slotY, state.mbStack)
            GlStateManager.disableDepth()
        }

        val mouseOverSlot = isOverControllerSlot(visMouseX, visMouseY)
        if (mouseOverSlot) {
            GlStateManager.enableBlend()
            GlStateManager.disableTexture2D()
            GlStateManager.color(1F, 1F, 1F, 0.5F)
            GuiRenderHelper.drawUntexturedQuad(slotX, slotY, slotX + 16, slotY + 16)
            RenderingHelper.resetColour()
            GlStateManager.enableTexture2D()
        }

        val tooltip = mutableListOf<String>()
        if (mouseOverSlot) {
            state?.mbStack?.getColouredTooltip(tooltip, ClientHelper.tooltipFlags)
        } else if (state != null) {
            state.renderer.getTooltip(tooltip, visMouseX, visMouseY, ClientHelper.tooltipFlags)
        } else {
            VisualizationRenderer.getUiTooltip(tooltip, visMouseX, visMouseY)
        }
        if (tooltip.isNotEmpty()) {
            drawHoveringText(tooltip, mouseX, mouseY)
        }
    }

    override fun doesGuiPauseGame(): Boolean = false

    protected override fun mouseClicked(mX: Int, mY: Int, button: Int) {
        val state = ClientVisualizationState.getState() ?: return
        if (state.renderer.handleClick(mX - windowX - VIS_OFF_X, mY - windowY - VIS_OFF_Y, button)) {
            val newLevel = state.renderer.level
            if (VisualizationToolItem.getLevel(state.visToolStack) != newLevel) {
                CbTweaker.defns.c2sVisualizationLevel.sendToServer(C2SVisualizationLevel(state.hand, newLevel))
            }
        }
    }

    protected override fun keyTyped(typed: Char, keyCode: Int) {
        if (keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            mc.displayGuiScreen(null)
        }
    }
}
