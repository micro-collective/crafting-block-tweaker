package st.evening.mc.cbtweaker.singleblock

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.world.World
import org.apache.commons.lang3.mutable.MutableBoolean
import st.evening.mc.cbtweaker.gui.element.IoConfigControlElement
import st.evening.mc.cbtweaker.gui.element.IoConfigModeControlElement
import st.evening.mc.cbtweaker.gui.element.RedstoneBehaviourControlElement
import st.evening.mc.cbtweaker.gui.inventory.CbtCustomContainer
import st.evening.mc.cbtweaker.gui.inventory.CbtCustomContainerGui
import st.evening.mc.cbtweaker.gui.inventory.MachineContainer
import st.evening.mc.cbtweaker.util.isInInteractionRange
import st.evening.mc.prelude.api.registration.ContainerFactory
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.findTileEntity

class SingleBlockMachineContainer(
    override val machine: SingleBlockMachineTileEntity,
    playerInv: InventoryPlayer
) : CbtCustomContainer(
    playerInv,
    machine.sbType.windowConfig,
    buildList {
        val sideConfigState = MutableBoolean(false)
        add(IoConfigModeControlElement(sideConfigState))
        machine.rsHandler?.let {
            add(RedstoneBehaviourControlElement(it))
        }
        machine.createMachineUiElement()?.let { add(it) }
        val bufHandler = machine.bufHandler
        machine.createBufferUiElements().forEach { (bufGroupId, subTable) ->
            subTable.forEach { (bufType, uiElems) ->
                uiElems.forEach { (bufName, uiElem) ->
                    add(
                        IoConfigControlElement(
                            sideConfigState,
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
                CbtCustomContainerGui(SingleBlockMachineContainer(it, player.inventory))
            }
    }
}
