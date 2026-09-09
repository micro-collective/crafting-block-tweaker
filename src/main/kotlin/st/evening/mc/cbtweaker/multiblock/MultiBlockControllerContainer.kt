package st.evening.mc.cbtweaker.multiblock

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.world.World
import st.evening.mc.cbtweaker.gui.element.MultiBlockStatusDisplay
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

class MultiBlockControllerContainer(
    override val machine: MultiBlockControllerTileEntity,
    playerInv: InventoryPlayer
) : CbtCustomContainer(
    playerInv,
    machine.mbType.windowConfig,
    listOfNotNull(machine.rsHandler),
    listOfNotNull(machine.createMachineUiElement())
), MachineContainer {
    private val expectedStateClock: Int = machine.assemblyStateClock

    override fun canInteractWith(player: EntityPlayer): Boolean =
        machine.assemblyStateClock == expectedStateClock && machine.isInInteractionRange(player)

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
                MultiBlockControllerGui(MultiBlockControllerContainer(it, player.inventory))
            }
    }
}

@ClientSide.Strong
class MultiBlockControllerGui(container: MultiBlockControllerContainer) :
    CbtCustomContainerGui<MultiBlockControllerContainer>(container) {

    override fun GuiElementDslContext<AbsoluteLayout>.addElements(windowWidth: Int, windowHeight: Int) {
        val mbCtrl = container.machine
        val region = container.windowConfig.machineInvRegion
        val x = region.posX + region.width - 11
        val y = region.posY - 11
        addChild(x, y, MultiBlockStatusDisplay(mbCtrl))
        mbCtrl.rsHandler?.let {
            addChild(x - 13, y, RedstoneBehaviourControl(it))
        }
    }
}
