package st.evening.mc.cbtweaker.multiblock

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.world.World
import st.evening.mc.cbtweaker.gui.element.MultiBlockStatusDisplayElement
import st.evening.mc.cbtweaker.gui.element.RedstoneBehaviourControlElement
import st.evening.mc.cbtweaker.gui.inventory.CbtCustomContainer
import st.evening.mc.cbtweaker.gui.inventory.CbtCustomContainerGui
import st.evening.mc.cbtweaker.gui.inventory.MachineContainer
import st.evening.mc.cbtweaker.util.isInInteractionRange
import st.evening.mc.prelude.api.registration.ContainerFactory
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.findTileEntity

class MultiBlockControllerContainer(
    override val machine: MultiBlockControllerTileEntity,
    playerInv: InventoryPlayer
) : CbtCustomContainer(
    playerInv,
    machine.mbType.windowConfig,
    buildList {
        add(MultiBlockStatusDisplayElement(machine))
        machine.rsHandler?.let {
            add(RedstoneBehaviourControlElement(it))
        }
        machine.createMachineUiElement()?.let { add(it) }
    }
), MachineContainer {
    override fun canInteractWith(player: EntityPlayer): Boolean = machine.isInInteractionRange(player)

    override fun getTranslationKey(): String = machine.mbType.translationKey

    object Factory : ContainerFactory {
        @ServerSide
        override fun createServerContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Container? =
            world.findTileEntity<MultiBlockControllerTileEntity>(x, y, z)?.let {
                MultiBlockControllerContainer(it, player.inventory)
            }

        @ClientSide.Strong
        override fun createClientContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): GuiContainer? =
            world.findTileEntity<MultiBlockControllerTileEntity>(x, y, z)?.let {
                CbtCustomContainerGui(MultiBlockControllerContainer(it, player.inventory))
            }
    }
}
