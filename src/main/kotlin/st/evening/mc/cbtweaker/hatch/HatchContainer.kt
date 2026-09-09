package st.evening.mc.cbtweaker.hatch

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.gui.element.HatchAutoExportControl
import st.evening.mc.cbtweaker.gui.inventory.CbtCustomContainer
import st.evening.mc.cbtweaker.gui.inventory.CbtCustomContainerGui
import st.evening.mc.cbtweaker.util.isInInteractionRange
import st.evening.mc.prelude.api.gui.engine.GuiElementDslContext
import st.evening.mc.prelude.api.gui.engine.addChild
import st.evening.mc.prelude.api.gui.engine.prefab.AbsoluteLayout
import st.evening.mc.prelude.api.registration.ContainerFactory
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.findTileEntity

class HatchContainer(val hatch: HatchTileEntity, playerInv: InventoryPlayer) : CbtCustomContainer(
    playerInv,
    hatch.hatchType.getTier(hatch.tier).windowConfig,
    listOfNotNull(hatch.exportHandler),
    listOfNotNull(hatch.createUiElement())
) {
    override fun canInteractWith(player: EntityPlayer): Boolean = hatch.isInInteractionRange(player)

    override fun getTranslationKey(): String = "${CbtConsts.MOD_ID}.hatch.${hatch.hatchType.id}.${hatch.tier}.name"

    object Factory : ContainerFactory {
        @ServerSide
        override fun createServerContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Container? =
            world.findTileEntity<HatchTileEntity>(x, y, z)?.let { HatchContainer(it, player.inventory) }

        @ClientSide.Strong
        override fun createClientContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): GuiContainer? =
            world.findTileEntity<HatchTileEntity>(x, y, z)?.let { HatchGui(HatchContainer(it, player.inventory)) }
    }
}

@ClientSide.Strong
class HatchGui(container: HatchContainer) : CbtCustomContainerGui<HatchContainer>(container) {
    override fun GuiElementDslContext<AbsoluteLayout>.addElements(windowWidth: Int, windowHeight: Int) {
        container.hatch.exportHandler?.let {
            val region = container.windowConfig.machineInvRegion
            addChild(region.posX + region.width - 11, region.posY - 11, HatchAutoExportControl(it))
        }
    }
}
