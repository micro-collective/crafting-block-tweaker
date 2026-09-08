package st.evening.mc.cbtweaker.network

import io.netty.util.ReferenceCounted
import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerTileEntity
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.network.S2CMessageHandler
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.network.onClientThread
import st.evening.mc.prelude.api.util.world.useTileEntity

class S2CBindMultiBlockAssembly(
    private val pos: BlockPos,
    private val hostId: Int,
    private val data: PacketBuffer
) : ReferenceCounted by data {
    object Serializer : NetworkSerializer<S2CBindMultiBlockAssembly> {
        override fun serializeToNetwork(buf: PacketBuffer, x: S2CBindMultiBlockAssembly) {
            buf.writeBlockPos(x.pos)
            buf.writeVarInt(x.hostId)
            val data = x.data
            buf.writeBytes(data, data.readerIndex(), data.readableBytes())
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): S2CBindMultiBlockAssembly {
            buf.retain()
            return S2CBindMultiBlockAssembly(buf.readBlockPos(), buf.readVarInt(), buf)
        }
    }

    object Handler : S2CMessageHandler<S2CBindMultiBlockAssembly> {
        @ClientSide.Strong
        override fun handleS2CMessage(netHandler: NetHandlerPlayClient, message: S2CBindMultiBlockAssembly) {
            message.data.retain()
            netHandler.onClientThread {
                try {
                    Minecraft.getMinecraft().world.useTileEntity<MultiBlockControllerTileEntity>(message.pos) {
                        it.bindSync(message.hostId, message.data)
                    }
                } finally {
                    message.data.release()
                }
            }
        }
    }
}
