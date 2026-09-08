package st.evening.mc.cbtweaker.gui.inventory

import io.netty.buffer.Unpooled
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IContainerListener
import net.minecraft.inventory.Slot
import net.minecraft.network.PacketBuffer
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.sync.SyncHost
import st.evening.mc.prelude.api.data.sync.SyncManager
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.onServer
import st.evening.mc.prelude.mod.network.S2CBindSyncedContainer

abstract class CbtCustomContainer(
    val playerInv: InventoryPlayer,
    val windowConfig: WindowConfig,
    override val uiElements: List<UiElement>
) : Container(), UiContainer, SyncHost {
    private val baseSlotIndices: IntArray = IntArray(uiElements.size)
    private val syncProxy: SyncProxy?

    override val syncManager: SyncManager
        get() = (syncProxy ?: throw IllegalStateException("Sync proxy not initialized!")).syncManager

    override val weakValidity: WeakValidity
        get() = WeakValidity.WEAK_VALID

    init {
        for (i in 0..<playerInv.sizeInventory) {
            addSlotToContainer(Slot(playerInv, i, 0, 0))
        }
        uiElements.forEachIndexed { i, uiElem ->
            baseSlotIndices[i] = inventorySlots.size
            uiElem.addToContainer(i, this, windowConfig.machineInvRegion)
        }
        val syncData = uiElements.mapNotNull { // order must be deterministic!
            (it.unwrap() as? SyncedUiElement)?.syncData
        }
        syncProxy = if (syncData.isNotEmpty()) SyncProxy(syncData) else null
    }

    override fun addSlot(slot: Slot) {
        addSlotToContainer(slot)
    }

    fun getBaseSlotIndex(uiElementIndex: Int): Int = baseSlotIndices[uiElementIndex]

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

    @ServerSide
    override fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M) {
        syncProxy?.dispatchSync(packetType, message)
    }

    private class SyncProxy(syncState: List<Piecewise>) : SyncHost {
        @ServerSide
        val listeningPlayers: MutableList<EntityPlayerMP> = mutableListOf()

        override val syncManager: SyncManager = SyncManager.create(this, ListStateComposite.fromStates(syncState))

        override val weakValidity: WeakValidity
            get() = WeakValidity.WEAK_VALID

        @ServerSide
        override fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M) {
            listeningPlayers.forEach { packetType.sendTo(message, it) }
        }
    }
}
