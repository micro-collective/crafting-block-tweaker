package st.evening.mc.cbtweaker.util.sync

import net.minecraft.tileentity.TileEntity
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.sync.SyncHost
import st.evening.mc.prelude.api.data.sync.SyncManager
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.network.sendToAllTracking
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.getTileEntityWeakValidity

class TileEntitySyncProxy(private val host: TileEntity, syncState: Piecewise) : SyncHost {
    override val syncManager: SyncManager = SyncManager.create(this, syncState)

    override val weakValidity: WeakValidity
        get() = host.getTileEntityWeakValidity()

    @ServerSide
    override fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M) {
        packetType.sendToAllTracking(message, host)
    }
}
