package st.evening.mc.cbtweaker.gui.inventory

import io.netty.buffer.Unpooled
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IContainerListener
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.sync.SyncHost
import st.evening.mc.prelude.api.data.sync.SyncManager
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.mergeStacksSafe
import st.evening.mc.prelude.api.util.game.onServer
import st.evening.mc.prelude.mod.network.S2CBindSyncedContainer

abstract class CbtCustomContainer(
    val playerInv: InventoryPlayer,
    val windowConfig: WindowConfig,
    containerSyncData: List<Piecewise>,
    override val uiElements: List<UiElement>
) : Container(), UiContainer, SyncHost {
    private val baseSlotIndices: IntArray = IntArray(uiElements.size)
    private val syncProxy: SyncProxy?

    override val syncManager: SyncManager
        get() = (syncProxy ?: throw IllegalStateException("Sync proxy not initialized!")).syncManager

    override val weakValidity: WeakValidity
        get() = syncProxy?.weakValidity ?: WeakValidity.INVALID

    init {
        for (i in 9..<36) {
            addSlotToContainer(Slot(playerInv, i, 0, 0))
        }
        for (i in 0..<9) {
            addSlotToContainer(Slot(playerInv, i, 0, 0))
        }
        uiElements.forEachIndexed { i, uiElem ->
            baseSlotIndices[i] = inventorySlots.size
            uiElem.addToContainer(i, this, windowConfig.machineInvRegion)
        }
        val syncData = containerSyncData.toMutableList()
        uiElements.forEach { element -> // order must be deterministic!
            (element.unwrap() as? SyncedUiElement)?.let {
                syncData += it.syncData
            }
        }
        syncProxy = if (syncData.isNotEmpty()) SyncProxy(syncData) else null
    }

    override fun addSlot(slot: Slot) {
        addSlotToContainer(slot)
    }

    fun getBaseSlotIndex(uiElementIndex: Int): Int = baseSlotIndices[uiElementIndex]

    override fun transferStackInSlot(player: EntityPlayer, index: Int): ItemStack =
        InvHelper.transferStacks(inventorySlots[index], player) { stack ->
            val totalSlotCount = inventorySlots.size
            return@transferStacks if (totalSlotCount > 36) {
                if (index < 36) {
                    mergeStacksSafe(stack, 36, totalSlotCount, false)
                } else {
                    mergeStacksSafe(stack, 0, 36, false)
                }
            } else if (index < 27) { // no machine slots; transfer within the player inventory only
                mergeStacksSafe(stack, 27, 36, false)
            } else {
                mergeStacksSafe(stack, 0, 27, false)
            }
        }

    abstract fun getTranslationKey(): String

    override fun addListener(listener: IContainerListener) {
        super.addListener(listener)
        syncProxy?.let {
            onServer {
                if (listener is EntityPlayerMP) {
                    it.listeningPlayers += listener
                    val syncMgr = it.syncManager.getServer()
                    val data = PacketBuffer(Unpooled.buffer())
                    syncMgr.writeFullState(data)
                    Prelude.defns.s2cBindSyncedContainer.sendTo(
                        S2CBindSyncedContainer(windowId, syncMgr.hostId, data),
                        listener
                    )
                }
            }
        }
    }

    override fun onContainerClosed(player: EntityPlayer) {
        syncProxy?.let {
            onServer {
                if (it.listeningPlayers.remove(player) && it.listeningPlayers.isEmpty()) {
                    it.weakValidity = WeakValidity.INVALID
                }
            }
        }
    }

    @ServerSide
    override fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M) {
        syncProxy?.dispatchSync(packetType, message)
    }

    private class SyncProxy(syncState: List<Piecewise>) : SyncHost {
        @ServerSide
        val listeningPlayers: MutableList<EntityPlayerMP> = mutableListOf()

        override var weakValidity: WeakValidity = WeakValidity.WEAK_VALID

        override val syncManager: SyncManager = SyncManager.create(this, ListStateComposite.fromStates(syncState))

        @ServerSide
        override fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M) {
            listeningPlayers.forEach { packetType.sendTo(message, it) }
        }
    }
}
