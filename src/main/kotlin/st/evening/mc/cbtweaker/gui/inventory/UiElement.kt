package st.evening.mc.cbtweaker.gui.inventory

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Slot
import st.evening.mc.prelude.api.data.sync.SyncContainer
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle

interface UiContainer {
    val uiElements: List<UiElement>

    fun addSlot(slot: Slot)
}

fun interface UiElementWrapper {
    @ClientSide.Strong
    fun wrap(uiIndex: Int, element: GuiElement): GuiElement

    object Noop : UiElementWrapper {
        @ClientSide.Strong
        override fun wrap(uiIndex: Int, element: GuiElement): GuiElement = element
    }
}

interface UiElement {
    fun addToContainer(uiIndex: Int, container: UiContainer, region: IntRectangle) {}

    @ClientSide.Strong
    fun addToGuiScreen(uiIndex: Int, layout: StackLayout, baseSlotIndex: Int, wrapper: UiElementWrapper)
}

interface SyncedUiElement : UiElement, SyncContainer

interface WrappedUiElement : UiElement {
    val delegateElement: UiElement
}

fun UiElement.unwrap(): UiElement = if (this is WrappedUiElement) delegateElement.unwrap() else this

@ClientSide.Physical
fun GuiContext.assertWindowId(): Int = (gui as GuiContainer).inventorySlots.windowId
