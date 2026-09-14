package st.evening.mc.cbtweaker.gui.element

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.util.text.TextFormatting
import org.apache.commons.lang3.mutable.MutableBoolean
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.gui.inventory.WrappedUiElement
import st.evening.mc.cbtweaker.gui.inventory.assertWindowId
import st.evening.mc.cbtweaker.network.C2SSetBufferAutoExporting
import st.evening.mc.cbtweaker.network.C2SSetBufferSideEnabled
import st.evening.mc.cbtweaker.util.component.AutoExportHandler
import st.evening.mc.cbtweaker.util.component.BufferConfig
import st.evening.mc.cbtweaker.util.component.SidedBufferConfig
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.ClickResult
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiElement
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.playUiClick
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.math.Vec2i
import st.evening.mc.prelude.api.util.math.containsPoint
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper
import st.evening.mc.prelude.api.util.world.RelativeFace

@ClientSide.Strong
class IoConfigModeControl(private val configState: MutableBoolean) : AbstractGuiElement() {
    override val contentWidth: Int
        get() = CbtGuiResources.IO_CONFIG.width
    override val contentHeight: Int
        get() = CbtGuiResources.IO_CONFIG.height

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
        override val width: Int
            get() = contentWidth
        override val height: Int
            get() = contentHeight

        override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            CbtGuiResources.IO_CONFIG.drawFullSize(partialTicks, posX, posY)
        }

        override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
            if (!containsPoint(mouseX, mouseY)) return false
            context.gui.drawHoveringText(I18n.format(CbtLang.TOOLTIP_CONFIGURE_IO), mouseX, mouseY)
            return true
        }

        override fun onMouseClick(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): ClickResult {
            if ((mouseButton != 0 && mouseButton != 1) || !containsPoint(mouseX, mouseY)) return ClickResult.Ignore
            configState.value = !configState.booleanValue()
            context.gui.mc.soundHandler.playUiClick()
            return ClickResult.Consume
        }
    }
}

@ClientSide.Strong
abstract class IoConfigControl(
    protected val uiIndex: Int,
    protected val configState: MutableBoolean,
    protected val element: GuiElement
) : GuiElement by element {
    companion object {
        private val BUTTON_OFFSETS: Array<Vec2i> = arrayOf(
            Vec2i(6, 6), // FRONT
            Vec2i(0, 6), // LEFT
            Vec2i(6 * 2, 6 * 2), // BACK
            Vec2i(6 * 2, 6), // RIGHT
            Vec2i(6, 0), // UP
            Vec2i(6, 6 * 2) // DOWN
        )
    }

    private abstract inner class Part(val uiIndex: Int, val part: GuiPart) : GuiPart by part {
        protected var configX: Int = computeConfigX()
            private set
        protected var configY: Int = computeConfigY()
            private set

        private fun computeConfigX(): Int = part.posX + (part.width - CbtGuiResources.SIDE_CONFIG.width) / 2

        // the icon is a square but the sprite sheet is a tall rectangle, so use width here
        private fun computeConfigY(): Int = part.posY + (part.height - CbtGuiResources.SIDE_CONFIG.width) / 2

        override fun setPosition(context: GuiContext, x: Int, y: Int, clipBox: Rect2i?) {
            part.setPosition(context, x, y, clipBox)
            configX = computeConfigX()
            configY = computeConfigY()
        }

        override fun drawOverlay(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            part.drawOverlay(context, partialTicks, mouseX, mouseY)
            if (!configState.booleanValue()) return
            RenderingHelper.pushMatrix {
                GlStateManager.translate(0F, 0F, 800F)
                GlStateManager.disableTexture2D()
                GlStateManager.enableBlend()
                GlStateManager.color(0F, 0F, 0F, 0.5F)
                GuiRenderHelper.drawUntexturedSizedQuad(part.posX, part.posY, part.width, part.height)
                RenderingHelper.resetColour()
                GlStateManager.enableTexture2D()
                drawConfig(partialTicks)
            }
        }

        protected abstract fun drawConfig(partialTicks: Float)

        override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean =
            if (configState.booleanValue()) {
                drawConfigTooltip(context, partialTicks, mouseX, mouseY)
            } else {
                part.drawTooltip(context, partialTicks, mouseX, mouseY)
            }

        protected fun GuiScreen.drawOnOffTooltip(mouseX: Int, mouseY: Int, translationKey: String, state: Boolean) {
            drawHoveringText(
                listOf(
                    I18n.format(translationKey),
                    "${TextFormatting.GRAY}${I18n.format(CbtLang.tooltipEnabledDisabled(state))}"
                ),
                mouseX, mouseY
            )
            RenderingHelper.resetColour()
        }

        protected abstract fun drawConfigTooltip(
            context: GuiContext,
            partialTicks: Float,
            mouseX: Int,
            mouseY: Int
        ): Boolean

        override fun onMouseClick(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): ClickResult {
            if (!configState.booleanValue()) return part.onMouseClick(context, mouseX, mouseY, mouseButton)
            if (mouseButton != 0 && mouseButton != 1) return ClickResult.Ignore
            if (handleConfigClick(context, mouseX, mouseY)) {
                context.gui.mc.soundHandler.playUiClick()
                return ClickResult.Consume
            }
            return ClickResult.Ignore
        }

        protected abstract fun handleConfigClick(context: GuiContext, mouseX: Int, mouseY: Int): Boolean

        override fun onMouseRelease(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): Boolean =
            !configState.booleanValue() && part.onMouseRelease(context, mouseX, mouseY, mouseButton)

        override fun onMouseDrag(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int, dragTime: Long) {
            if (configState.booleanValue()) return
            part.onMouseDrag(context, mouseX, mouseY, mouseButton, dragTime)
        }

        override fun onMouseScroll(context: GuiContext, mouseX: Int, mouseY: Int, scroll: Int): Boolean =
            !configState.booleanValue() && part.onMouseScroll(context, mouseX, mouseY, scroll)
    }

    class Sided(
        uiIndex: Int,
        configState: MutableBoolean,
        element: GuiElement,
        private val config: SidedBufferConfig<*>
    ) : IoConfigControl(uiIndex, configState, element) {
        override fun bake(
            contX: Int,
            contY: Int,
            contWidth: Int?,
            contHeight: Int?,
            alignX: DrawAlignment,
            alignY: DrawAlignment
        ): GuiPart = SidedPart(element.bake(contX, contY, contWidth, contHeight, alignX, alignY))

        private inner class SidedPart(part: GuiPart) : Part(uiIndex, part) {
            override fun drawConfig(partialTicks: Float) {
                val x = configX
                val y = configY
                config.exportHandler?.let {
                    CbtGuiResources.sideConfigExport(it.autoExporting).drawFullSize(partialTicks, x, y)
                }
                RelativeFace.entries.forEachIndexed { i, face ->
                    val offset = BUTTON_OFFSETS[i]
                    CbtGuiResources.sideConfig(face, config.isEnabled(face))
                        .drawFullSize(partialTicks, x + offset.x, y + offset.y)
                }
            }

            override fun drawConfigTooltip(
                context: GuiContext,
                partialTicks: Float,
                mouseX: Int,
                mouseY: Int
            ): Boolean {
                val face = (getHoveredZone(mouseX, mouseY) ?: return false).face
                if (face != null) {
                    context.gui.drawOnOffTooltip(mouseX, mouseY, face.translationKey, config.isEnabled(face))
                    return true
                } else {
                    config.exportHandler?.let {
                        context.gui.drawOnOffTooltip(mouseX, mouseY, CbtLang.TOOLTIP_AUTO_EXPORT, it.autoExporting)
                        return true
                    }
                    return false
                }
            }

            override fun handleConfigClick(context: GuiContext, mouseX: Int, mouseY: Int): Boolean {
                val face = (getHoveredZone(mouseX, mouseY) ?: return false).face
                if (face != null) {
                    CbTweaker.defns.c2sSetBufferSideEnabled.sendToServer(
                        C2SSetBufferSideEnabled(context.assertWindowId(), uiIndex, face, !config.isEnabled(face))
                    )
                    return true
                } else {
                    config.exportHandler?.let {
                        CbTweaker.defns.c2sSetBufferAutoExporting.sendToServer(
                            C2SSetBufferAutoExporting(context.assertWindowId(), uiIndex, !it.autoExporting)
                        )
                        return true
                    }
                    return false
                }
            }

            private fun getHoveredZone(mouseX: Int, mouseY: Int): MouseZone? {
                val x = configX
                val y = configY
                if (mouseX < x || mouseY < y) return null
                if (mouseX < x + 6) {
                    if (mouseY < y + 6) {
                        return MouseZone.AUTO_EXPORT
                    } else if (mouseY < y + 12) {
                        return MouseZone.LEFT
                    } // nothing in the bottom-left
                } else if (mouseX < x + 12) {
                    if (mouseY < y + 6) {
                        return MouseZone.UP
                    } else if (mouseY < y + 12) {
                        return MouseZone.FRONT
                    } else if (mouseY < y + 18) {
                        return MouseZone.DOWN
                    }
                } else if (mouseX < x + 18) {
                    if (mouseY < y + 6) {
                        // nothing in the top-right
                    } else if (mouseY < y + 12) {
                        return MouseZone.RIGHT
                    } else if (mouseY < y + 18) {
                        return MouseZone.BACK
                    }
                }
                return null
            }

        }

        private enum class MouseZone(val face: RelativeFace?) {
            FRONT(RelativeFace.FRONT),
            LEFT(RelativeFace.LEFT),
            BACK(RelativeFace.BACK),
            RIGHT(RelativeFace.RIGHT),
            UP(RelativeFace.UP),
            DOWN(RelativeFace.DOWN),
            AUTO_EXPORT(null),
        }
    }

    class Unsided(
        uiIndex: Int,
        configState: MutableBoolean,
        element: GuiElement,
        private val exportHandler: AutoExportHandler<*>
    ) : IoConfigControl(uiIndex, configState, element) {
        override fun bake(
            contX: Int,
            contY: Int,
            contWidth: Int?,
            contHeight: Int?,
            alignX: DrawAlignment,
            alignY: DrawAlignment
        ): GuiPart = UnsidedPart(element.bake(contX, contY, contWidth, contHeight, alignX, alignY))

        private inner class UnsidedPart(part: GuiPart) : Part(uiIndex, part) {
            override fun drawConfig(partialTicks: Float) {
                CbtGuiResources.sideConfigExport(exportHandler.autoExporting)
                    .drawFullSize(partialTicks, configX, configY)
            }

            override fun drawConfigTooltip(
                context: GuiContext,
                partialTicks: Float,
                mouseX: Int,
                mouseY: Int
            ): Boolean {
                if (!isHovered(mouseX, mouseY)) return false
                context.gui.drawOnOffTooltip(mouseX, mouseY, CbtLang.TOOLTIP_AUTO_EXPORT, exportHandler.autoExporting)
                return true
            }

            override fun handleConfigClick(context: GuiContext, mouseX: Int, mouseY: Int): Boolean {
                if (!isHovered(mouseX, mouseY)) return false
                CbTweaker.defns.c2sSetBufferAutoExporting.sendToServer(
                    C2SSetBufferAutoExporting(context.assertWindowId(), uiIndex, !exportHandler.autoExporting)
                )
                return true
            }

            private fun isHovered(mouseX: Int, mouseY: Int): Boolean {
                val x = configX
                val y = configY
                return mouseX >= x && mouseX < x + 6 && mouseY >= y && mouseY < y + 6
            }
        }
    }
}

class IoConfigControlElement(
    private val configState: MutableBoolean,
    override val delegateElement: UiElement,
    private val config: BufferConfig<*>
) : WrappedUiElement, UiElement by delegateElement, UiElementWrapper {
    @ClientSide.Strong
    override fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper) {
        delegateElement.addToGuiScreen(uiIndex, layout, baseSlotIndex, this)
    }

    @ClientSide.Strong
    override fun wrap(uiIndex: Int, element: GuiElement): GuiElement {
        if (config is SidedBufferConfig<*>) {
            return IoConfigControl.Sided(uiIndex, configState, element, config)
        }
        config.exportHandler?.let {
            return IoConfigControl.Unsided(uiIndex, configState, element, it)
        }
        return element
    }

    fun handleSetAutoExporting(exporting: Boolean) {
        config.exportHandler?.let {
            it.autoExporting = exporting
        }
    }

    fun handleSetSideEnabled(face: RelativeFace, enabled: Boolean) {
        if (config !is SidedBufferConfig<*>) return
        config.setEnabled(face, enabled)
    }
}
