package st.evening.mc.cbtweaker.singleblock

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.world.World
import org.apache.commons.lang3.mutable.MutableBoolean
import st.evening.mc.cbtweaker.gui.element.IoConfigControlElement
import st.evening.mc.cbtweaker.gui.element.IoConfigModeControl
import st.evening.mc.cbtweaker.gui.element.RedstoneBehaviourControl
import st.evening.mc.cbtweaker.gui.inventory.CbtCustomContainer
import st.evening.mc.cbtweaker.gui.inventory.CbtCustomContainerGui
import st.evening.mc.cbtweaker.gui.inventory.MachineContainer
import st.evening.mc.cbtweaker.util.isInInteractionRange
import st.evening.mc.prelude.api.gui.engine.GuiElementDslContext
import st.evening.mc.prelude.api.gui.engine.addChild
import st.evening.mc.prelude.api.gui.engine.prefab.AbsoluteLayout
import st.evening.mc.prelude.api.registration.ContainerFactory
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.findTileEntity

class SingleBlockMachineContainer(
    override val machine: SingleBlockMachineTileEntity,
    playerInv: InventoryPlayer,
    val ioConfigState: MutableBoolean = MutableBoolean(false)
) : CbtCustomContainer(
    playerInv,
    machine.sbType.windowConfig,
    buildList {
        addAll(machine.bufHandler.configSyncState)
        machine.rsHandler?.let { add(it) }
    },
    buildList {
        machine.createMachineUiElement()?.let { add(it) }
        val bufHandler = machine.bufHandler
        machine.createBufferUiElements().forEach { (bufGroupId, subTable) ->
            subTable.forEach { (bufType, uiElems) ->
                uiElems.forEach { (bufName, uiElem) ->
                    add(
                        IoConfigControlElement(
                            ioConfigState,
                            uiElem,
                            bufHandler.getConfig(bufGroupId, bufType, bufName)!!
                        )
                    )
                }
            }
        }
    }
), MachineContainer {
    override fun canInteractWith(player: EntityPlayer): Boolean = machine.isInInteractionRange(player)

    override fun getTranslationKey(): String = machine.sbType.translationKey

    object Factory : ContainerFactory {
        @ServerSide
        override fun createServerContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Container? =
            world.findTileEntity<SingleBlockMachineTileEntity>(x, y, z)?.let {
                SingleBlockMachineContainer(it, player.inventory)
            }

        @ClientSide.Strong
        override fun createClientContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): GuiContainer? =
            world.findTileEntity<SingleBlockMachineTileEntity>(x, y, z)?.let {
                SingleBlockMachineGui(SingleBlockMachineContainer(it, player.inventory))
            }
    }
}

@ClientSide.Strong
class SingleBlockMachineGui(container: SingleBlockMachineContainer) :
    CbtCustomContainerGui<SingleBlockMachineContainer>(container) {

    override fun GuiElementDslContext<AbsoluteLayout>.addElements(windowWidth: Int, windowHeight: Int) {
        val sbMachine = container.machine
        val region = container.windowConfig.machineInvRegion
        val x = region.posX + region.width - 11
        val y = region.posY - 11
        addChild(x, y, IoConfigModeControl(container.ioConfigState))
        sbMachine.rsHandler?.let {
            addChild(x - 13, y, RedstoneBehaviourControl(it))
        }
    }

    override fun isPointInRegion( // only used to check whether the mouse is over a slot or not
        rectX: Int, rectY: Int, rectWidth: Int, rectHeight: Int, pointX: Int, pointY: Int
    ): Boolean = !container.ioConfigState.booleanValue() &&
        super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY)
}
