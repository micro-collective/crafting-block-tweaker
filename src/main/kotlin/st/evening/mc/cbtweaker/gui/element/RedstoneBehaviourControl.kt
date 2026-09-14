package st.evening.mc.cbtweaker.gui.element

import net.minecraft.client.resources.I18n
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.cbtweaker.gui.inventory.assertWindowId
import st.evening.mc.cbtweaker.network.C2SSetRedstoneBehaviour
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.ClickResult
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiPart
import st.evening.mc.prelude.api.util.data.cycleEnum
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.playUiClick
import st.evening.mc.prelude.api.util.math.containsPoint

@ClientSide.Strong
class RedstoneBehaviourControl(private val rsHandler: RedstoneControlHandler) : AbstractGuiElement() {
    override val contentWidth: Int
        get() = CbtGuiResources.REDSTONE_BEHAVIOUR_INVERTED.width
    override val contentHeight: Int
        get() = CbtGuiResources.REDSTONE_BEHAVIOUR_INVERTED.height

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
        override val width: Int
            get() = contentWidth
        override val height: Int
            get() = contentHeight

        override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            CbtGuiResources.redstoneBehaviour(rsHandler.redstoneBehaviour).drawFullSize(partialTicks, posX, posY)
        }

        override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
            if (!containsPoint(mouseX, mouseY)) return false
            context.gui.drawHoveringText(
                listOf(
                    I18n.format(CbtLang.TOOLTIP_REDSTONE_BEHAVIOUR),
                    "${TextFormatting.GRAY}${rsHandler.redstoneBehaviour.getLocalizedName()}"
                ),
                mouseX, mouseY
            )
            return true
        }

        override fun onMouseClick(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): ClickResult {
            if (!containsPoint(mouseX, mouseY)) return ClickResult.Ignore
            when (mouseButton) {
                0 -> {
                    CbTweaker.defns.c2sSetRedstoneBehaviour.sendToServer(
                        C2SSetRedstoneBehaviour(context.assertWindowId(), rsHandler.redstoneBehaviour.cycleEnum(1))
                    )
                    context.gui.mc.soundHandler.playUiClick()
                    return ClickResult.Consume
                }
                1 -> {
                    CbTweaker.defns.c2sSetRedstoneBehaviour.sendToServer(
                        C2SSetRedstoneBehaviour(context.assertWindowId(), rsHandler.redstoneBehaviour.cycleEnum(-1))
                    )
                    context.gui.mc.soundHandler.playUiClick(1.2F)
                    return ClickResult.Consume
                }
                else -> return ClickResult.Ignore
            }
        }
    }
}
