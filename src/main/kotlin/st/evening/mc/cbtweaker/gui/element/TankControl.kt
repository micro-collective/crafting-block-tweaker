package st.evening.mc.cbtweaker.gui.element

import net.minecraft.client.resources.I18n
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.gui.inventory.assertWindowId
import st.evening.mc.cbtweaker.network.C2SInteractTankTransfer
import st.evening.mc.cbtweaker.util.gui.FluidBarRenderer
import st.evening.mc.cbtweaker.util.gui.SpriteBarRenderer
import st.evening.mc.cbtweaker.util.gui.TankDrawData
import st.evening.mc.cbtweaker.util.machine.TransferType
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.gui.engine.ClickResult
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiPart
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.playUiClick
import st.evening.mc.prelude.api.util.math.containsPoint

@ClientSide.Strong
abstract class TankControl<T, C>(
    private val uiIndex: Int,
    private val tank: T,
    private val bgTexture: GuiDrawable,
    private val fgOffsetX: Int,
    private val fgOffsetY: Int,
    private val fgWidth: Int,
    private val fgHeight: Int,
    private val interactive: Boolean
) : AbstractGuiElement() {
    constructor(uiIndex: Int, tank: T, uiTank: TankDrawData, interactive: Boolean) : this(
        uiIndex,
        tank,
        uiTank.bgTexture.drawable,
        uiTank.fgOffsetX,
        uiTank.fgOffsetY,
        uiTank.fgWidth,
        uiTank.fgHeight,
        interactive
    )

    override val contentWidth: Int
        get() = bgTexture.width
    override val contentHeight: Int
        get() = bgTexture.height

    override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

    protected abstract fun getContents(tank: T): C?

    protected abstract fun getCapacity(tank: T): Int

    protected abstract fun getAmount(contents: C): Int

    protected abstract fun createBarRenderer(): SpriteBarRenderer<C>

    protected abstract fun getLocalizedName(contents: C): String

    protected open fun getLocalizedEmptyText(): String = I18n.format(CbtLang.TOOLTIP_EMPTY)

    private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
        override val width: Int
            get() = contentWidth
        override val height: Int
            get() = contentHeight

        private val barRenderer: SpriteBarRenderer<C> = createBarRenderer()

        override fun drawBackground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            bgTexture.drawFullSize(partialTicks, posX, posY)
        }

        override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
            barRenderer.drawBar(
                getContents(tank),
                getCapacity(tank),
                posX + fgOffsetX,
                posY + fgOffsetY,
                fgWidth,
                fgHeight,
                partialTicks
            )
        }

        override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
            if (!containsPoint(mouseX, mouseY)) return false
            val contents = getContents(tank)
            if (contents != null && getAmount(contents) > 0) {
                context.gui.drawHoveringText(
                    listOf(
                        getLocalizedName(contents),
                        "${TextFormatting.GRAY}%,d / %,d mB".format(getAmount(contents), getCapacity(tank)),
                        "",
                        "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_INSERT)}",
                        "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_EXTRACT)}"
                    ),
                    mouseX, mouseY
                )
            } else {
                context.gui.drawHoveringText(
                    listOf(
                        getLocalizedEmptyText(),
                        "${TextFormatting.GRAY}0 / %,d mB".format(getCapacity(tank)),
                        "",
                        "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_INSERT)}",
                        "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_EXTRACT)}"
                    ),
                    mouseX, mouseY
                )
            }
            return true
        }

        override fun onMouseClick(context: GuiContext, mouseX: Int, mouseY: Int, mouseButton: Int): ClickResult {
            if (!interactive || (mouseButton != 0 && mouseButton != 1) || !containsPoint(mouseX, mouseY)) {
                return ClickResult.Ignore
            }
            val mc = context.gui.mc
            if (mc.player.inventory.itemStack.isEmpty) return ClickResult.Ignore
            CbTweaker.defns.c2sInteractTankTransfer.sendToServer(
                C2SInteractTankTransfer(
                    context.assertWindowId(),
                    uiIndex,
                    if (mouseButton == 0) TransferType.INSERT else TransferType.EXTRACT
                )
            )
            mc.soundHandler.playUiClick()
            return ClickResult.Consume
        }
    }
}

@ClientSide.Strong
class FluidTankControl(uiIndex: Int, tank: IFluidTank, uiTank: TankDrawData, interactive: Boolean) :
    TankControl<IFluidTank, FluidStack>(uiIndex, tank, uiTank, interactive) {

    override fun getContents(tank: IFluidTank): FluidStack? = tank.fluid

    override fun getCapacity(tank: IFluidTank): Int = tank.capacity

    override fun getAmount(contents: FluidStack): Int = contents.amount

    override fun createBarRenderer(): SpriteBarRenderer<FluidStack> = FluidBarRenderer()

    override fun getLocalizedName(contents: FluidStack): String = contents.localizedName

    override fun getLocalizedEmptyText(): String = I18n.format(CbtLang.TOOLTIP_EMPTY_FLUID)
}
