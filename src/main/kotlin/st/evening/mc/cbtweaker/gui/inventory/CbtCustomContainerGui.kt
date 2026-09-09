package st.evening.mc.cbtweaker.gui.inventory

import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.prelude.api.container.prefab.PrefabEngineGuiContainer
import st.evening.mc.prelude.api.gui.engine.GuiElementDslContext
import st.evening.mc.prelude.api.gui.engine.LayoutEngine
import st.evening.mc.prelude.api.gui.engine.addChild
import st.evening.mc.prelude.api.gui.engine.prefab.AbsoluteLayout
import st.evening.mc.prelude.api.gui.engine.prefab.BackgroundBox
import st.evening.mc.prelude.api.gui.engine.prefab.InventorySlot
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.gui.engine.prefab.TableLayout
import st.evening.mc.prelude.api.gui.engine.prefab.TextDisplay
import st.evening.mc.prelude.api.gui.engine.setChild
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Strong
open class CbtCustomContainerGui<C : CbtCustomContainer>(container: C) : PrefabEngineGuiContainer<C>(container) {
    override val layoutEngine: LayoutEngine = LayoutEngine.fromDsl(
        container.windowConfig.background.drawable.let { AbsoluteLayout(it.width, it.height) },
        { BackgroundBox(it, container.windowConfig.background.drawable) }
    ) {
        // player inventory
        val playerRegion = container.windowConfig.playerInvRegion
        addChild(playerRegion.posX, playerRegion.posY, TableLayout(4, 9, playerRegion.width, playerRegion.height)) {
            configure {
                configureRow(3, flex = 1F, startPadding = 4)
            }
            for (row in 0..<3) {
                for (col in 0..<9) {
                    setChild(row, col, InventorySlot(col + row * 9 + 9, CbtGuiResources.ITEM_SLOT))
                }
            }
            for (col in 0..<9) {
                setChild(3, col, InventorySlot(col, CbtGuiResources.ITEM_SLOT))
            }
        }
        if (container.windowConfig.renderPlayerName) {
            addChild(playerRegion.posX, playerRegion.posY - 10, TextDisplay.constant(container.playerInv.displayName))
        }

        // machine ui
        val machineRegion = container.windowConfig.machineInvRegion
        addChild(machineRegion.posX, machineRegion.posY, StackLayout(machineRegion.width, machineRegion.height)) {
            container.uiElements.forEachIndexed { i, uiElem ->
                uiElem.addToGuiScreen(i, element, container.getBaseSlotIndex(i), UiElementWrapper.Noop)
            }
        }
        if (container.windowConfig.renderMachineName) {
            addChild(machineRegion.posX, machineRegion.posY - 10, TextDisplay.fromI18n(container.getTranslationKey()))
        }

        addElements(element.contentWidth, element.contentHeight)
    }

    protected open fun GuiElementDslContext<AbsoluteLayout>.addElements(windowWidth: Int, windowHeight: Int) {}
}
