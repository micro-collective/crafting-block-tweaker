package st.evening.mc.cbtweaker.gui.element

import net.minecraft.client.resources.I18n
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerTileEntity
import st.evening.mc.cbtweaker.structure.StructureHighlightParticle
import st.evening.mc.cbtweaker.util.getRotationFromNorth
import st.evening.mc.cbtweaker.util.offsetWithRotation
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.ClickResult
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiPart
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.playUiClick
import st.evening.mc.prelude.api.util.math.containsPoint

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
                        "${TextFormatting.GREEN}${I18n.format(CbtLang.TOOLTIP_MB_ASSEMBLED)}"
                    } else {
                        "${TextFormatting.RED}${I18n.format(CbtLang.TOOLTIP_MB_NOT_ASSEMBLED)}"
                    },
                    "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_MB_VISUALIZE)}"
                ),
                mouseX, mouseY
            )
            return true
        }

        override fun onMouseClick(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): ClickResult {
            if ((mouseButton != 0 && mouseButton != 1) || !containsPoint(mouseX, mouseY)) return ClickResult.Ignore
            val world = mbCtrl.world
            val ctrlPos = mbCtrl.pos
            val rot = world.getBlockState(ctrlPos).getValue(BlockSidedIfc.PROP_FACING).getRotationFromNorth()
            val mc = context.gui.mc
            val (matchers, mirrorX) = mbCtrl.getStructureVisualization()
            val fx = mc.effectRenderer
            matchers.forEach { (offset, matcher) ->
                if (matcher.visualization.isEmpty()) return@forEach
                val pos = ctrlPos.offsetWithRotation(offset, rot, mirrorX)
                fx.addEffect(StructureHighlightParticle(world, pos, 100))
            }
            mc.soundHandler.playUiClick()
            return ClickResult.Consume
        }
    }
}
