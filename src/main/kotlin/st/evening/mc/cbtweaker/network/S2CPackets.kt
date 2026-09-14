package st.evening.mc.cbtweaker.network

import io.netty.util.ReferenceCounted
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.PacketBuffer
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentTranslation
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerTileEntity
import st.evening.mc.prelude.api.data.ser.EnumSerializer
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.network.S2CMessageHandler
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.network.onClientThread
import st.evening.mc.prelude.api.util.world.useTileEntity

class S2CClientEffect(val effectType: Type) {
    enum class Type {
        CONFIG_COPY, CONFIG_COPY_EMPTY, CONFIG_PASTE;

        companion object {
            val serializer: EnumSerializer<Type> = EnumSerializer()
        }
    }

    object Serializer : NetworkSerializer<S2CClientEffect> {
        override fun serializeToNetwork(buf: PacketBuffer, x: S2CClientEffect) {
            Type.serializer.serializeToNetwork(buf, x.effectType)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): S2CClientEffect =
            S2CClientEffect(Type.serializer.deserializeFromNetwork(buf))
    }

    object Handler : S2CMessageHandler<S2CClientEffect> {
        @ClientSide.Strong
        override fun handleS2CMessage(netHandler: NetHandlerPlayClient, message: S2CClientEffect) {
            netHandler.onClientThread {
                when (message.effectType) {
                    Type.CONFIG_COPY ->
                        notify(TextComponentTranslation(CbtLang.NOTIF_CONFIG_COPY), CbTweaker.defns.soundConfigCopy)
                    Type.CONFIG_COPY_EMPTY -> notify(TextComponentTranslation(CbtLang.NOTIF_CONFIG_COPY_EMPTY), null)
                    Type.CONFIG_PASTE ->
                        notify(TextComponentTranslation(CbtLang.NOTIF_CONFIG_PASTE), CbTweaker.defns.soundConfigPaste)
                }
            }
        }

        @ClientSide.Strong
        private fun notify(text: ITextComponent, sound: SoundEvent?) {
            val mc = Minecraft.getMinecraft()
            mc.player.sendStatusMessage(text, true)
            if (sound != null) {
                mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(sound, 1F))
            }
        }
    }
}

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
