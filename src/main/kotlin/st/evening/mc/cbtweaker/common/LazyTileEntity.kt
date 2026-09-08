package st.evening.mc.cbtweaker.common

import io.netty.buffer.Unpooled
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.sync.SyncHost
import st.evening.mc.prelude.api.util.data.buildArrayFromPacketBuffer
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.assertPhysicalClient
import st.evening.mc.prelude.api.util.world.onClient
import st.evening.mc.prelude.api.util.world.onServer

abstract class LazyTileEntity<T : NbtCompoundSerializable> : TileEntity() {
    companion object {
        private const val SER_SYNC_ID: String = $$"p$sid"
        private const val SER_DATA: String = $$"p$data"
    }

    private var dataCache: T? = null
    private var bufferedDataDeser: NBTTagCompound? = null
    private var syncProxy: SyncHost? = null

    protected val data: T
        get() {
            dataCache?.let { return it }
            val data = initData()
            dataCache = data
            bufferedDataDeser?.let {
                data.readFromNbt(it)
                bufferedDataDeser = null
            }
            return data
        }

    protected abstract fun initData(): T

    protected fun initSync(syncProxy: SyncHost?) {
        this.syncProxy = syncProxy
    }

    override fun getUpdatePacket(): SPacketUpdateTileEntity? =
        syncProxy?.let { SPacketUpdateTileEntity(pos, 0, updateTag) }

    @OptIn(SidednessAssertion::class)
    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        assertPhysicalClient {
            handleUpdateTag(pkt.nbtCompound)
        }
    }

    override fun getUpdateTag(): NBTTagCompound {
        val tag = super.getUpdateTag()
        world.onServer {
            syncProxy?.syncManager?.getServer()?.let { syncMgr ->
                tag.runAction {
                    SER_SYNC_ID int syncMgr.hostId
                    SER_DATA byteArray buildArrayFromPacketBuffer { syncMgr.writeFullState(it) }
                }
            }
        }
        return tag
    }

    override fun handleUpdateTag(tag: NBTTagCompound) {
        super.handleUpdateTag(tag)
        world.onClient {
            syncProxy?.syncManager?.getClient()?.let { syncMgr ->
                syncMgr.registerClientSide(tag.getInteger(SER_SYNC_ID))
                syncMgr.readFullState(PacketBuffer(Unpooled.wrappedBuffer(tag.getByteArray(SER_DATA))))
            }
        }
    }

    override fun writeToNBT(tag: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(tag)
        data.writeToNbt(tag)
        return tag
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        val data = dataCache
        if (data != null) {
            data.readFromNbt(tag)
        } else {
            bufferedDataDeser = tag
        }
    }
}
