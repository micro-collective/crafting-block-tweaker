package st.evening.mc.cbtweaker.network

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumHand
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.gui.element.IoConfigControlElement
import st.evening.mc.cbtweaker.gui.inventory.MachineContainer
import st.evening.mc.cbtweaker.gui.inventory.UiContainer
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.unwrap
import st.evening.mc.cbtweaker.hatch.HatchContainer
import st.evening.mc.cbtweaker.structure.VisualizationToolItem
import st.evening.mc.cbtweaker.util.machine.TransferType
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.network.C2SMessageHandler
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.machine.RedstoneBehaviour
import st.evening.mc.prelude.api.util.network.onServerThread
import st.evening.mc.prelude.api.util.world.RelativeFace
import kotlin.experimental.and

class C2SVisualizationLevel(val hand: EnumHand, val level: Int?) {
    object Serializer : NetworkSerializer<C2SVisualizationLevel> {
        override fun serializeToNetwork(buf: PacketBuffer, x: C2SVisualizationLevel) {
            val level = x.level
            if (level != null) {
                buf.writeByte(if (x.hand == EnumHand.OFF_HAND) 0x3 else 0x2).writeShort(level)
            } else {
                buf.writeByte(if (x.hand == EnumHand.OFF_HAND) 0x1 else 0x0)
            }
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): C2SVisualizationLevel {
            val mask = buf.readByte()
            return C2SVisualizationLevel(
                if (mask and 0x1 != 0.toByte()) EnumHand.OFF_HAND else EnumHand.MAIN_HAND,
                if (mask and 0x2 != 0.toByte()) buf.readShort().toInt() else null
            )
        }
    }

    object Handler : C2SMessageHandler<C2SVisualizationLevel> {
        @ServerSide
        override fun handleC2SMessage(netHandler: NetHandlerPlayServer, message: C2SVisualizationLevel) {
            netHandler.onServerThread {
                val stack = netHandler.player.getHeldItem(message.hand)
                if (stack.item == CbTweaker.defns.itemVisualizationTool) {
                    VisualizationToolItem.setLevel(stack, message.level)
                }
            }
        }
    }
}

class C2SSetHatchAutoExporting(private val windowId: Int, private val exporting: Boolean) {
    object Serializer : NetworkSerializer<C2SSetHatchAutoExporting> {
        override fun serializeToNetwork(buf: PacketBuffer, x: C2SSetHatchAutoExporting) {
            buf.writeByte(if (x.exporting) x.windowId or 0x80 else x.windowId)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): C2SSetHatchAutoExporting {
            val k = buf.readByte()
            return C2SSetHatchAutoExporting(k.toInt() and 0x7F, k and 0x80.toByte() != 0.toByte())
        }
    }

    object Handler : C2SMessageHandler<C2SSetHatchAutoExporting> {
        @ServerSide
        override fun handleC2SMessage(netHandler: NetHandlerPlayServer, message: C2SSetHatchAutoExporting) {
            netHandler.onServerThread {
                val container = netHandler.player.openContainer ?: return@onServerThread
                if (container.windowId != message.windowId || container !is HatchContainer) return@onServerThread
                container.hatch.exportHandler?.autoExporting = message.exporting
            }
        }
    }
}

class C2SSetRedstoneBehaviour(private val windowId: Int, private val behaviour: RedstoneBehaviour) {
    object Serializer : NetworkSerializer<C2SSetRedstoneBehaviour> {
        override fun serializeToNetwork(buf: PacketBuffer, x: C2SSetRedstoneBehaviour) {
            buf.writeByte(x.windowId)
            RedstoneBehaviour.serializer.serializeToNetwork(buf, x.behaviour)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): C2SSetRedstoneBehaviour =
            C2SSetRedstoneBehaviour(buf.readByte().toInt(), RedstoneBehaviour.serializer.deserializeFromNetwork(buf))
    }

    object Handler : C2SMessageHandler<C2SSetRedstoneBehaviour> {
        @ServerSide
        override fun handleC2SMessage(netHandler: NetHandlerPlayServer, message: C2SSetRedstoneBehaviour) {
            netHandler.onServerThread {
                val container = netHandler.player.openContainer ?: return@onServerThread
                if (container.windowId != message.windowId || container !is MachineContainer) return@onServerThread
                container.machine.rsHandler?.updateRedstoneBehaviour(message.behaviour)
            }
        }
    }
}

class C2SSetBufferAutoExporting(
    override val windowId: Int,
    override val uiIndex: Int,
    private val exporting: Boolean
) : UiElementMessage {
    object Serializer : NetworkSerializer<C2SSetBufferAutoExporting> {
        override fun serializeToNetwork(buf: PacketBuffer, x: C2SSetBufferAutoExporting) {
            buf.writeByte(if (x.exporting) x.windowId or 0x80 else x.windowId)
            buf.writeVarInt(x.uiIndex)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): C2SSetBufferAutoExporting {
            val k = buf.readByte()
            return C2SSetBufferAutoExporting(k.toInt() and 0x7F, buf.readVarInt(), k and 0x80.toByte() != 0.toByte())
        }
    }

    object Handler : RawUiElementMessageHandler<C2SSetBufferAutoExporting>() {
        @ServerSide
        override fun handleRawUiElementMessage(
            netHandler: NetHandlerPlayServer,
            uiElem: UiElement,
            message: C2SSetBufferAutoExporting
        ) {
            if (uiElem !is IoConfigControlElement) return
            uiElem.handleSetAutoExporting(message.exporting)
        }
    }
}

class C2SSetBufferSideEnabled(
    override val windowId: Int,
    override val uiIndex: Int,
    private val face: RelativeFace,
    private val enabled: Boolean
) : UiElementMessage {
    object Serializer : NetworkSerializer<C2SSetBufferSideEnabled> {
        override fun serializeToNetwork(buf: PacketBuffer, x: C2SSetBufferSideEnabled) {
            buf.writeByte(if (x.enabled) x.windowId or 0x80 else x.windowId)
            buf.writeVarInt(x.uiIndex)
            RelativeFace.serializer.serializeToNetwork(buf, x.face)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): C2SSetBufferSideEnabled {
            val k = buf.readByte()
            return C2SSetBufferSideEnabled(
                k.toInt() and 0x7F,
                buf.readVarInt(),
                RelativeFace.serializer.deserializeFromNetwork(buf),
                k and 0x80.toByte() != 0.toByte()
            )
        }
    }

    object Handler : RawUiElementMessageHandler<C2SSetBufferSideEnabled>() {
        @ServerSide
        override fun handleRawUiElementMessage(
            netHandler: NetHandlerPlayServer,
            uiElem: UiElement,
            message: C2SSetBufferSideEnabled
        ) {
            if (uiElem !is IoConfigControlElement) return
            uiElem.handleSetSideEnabled(message.face, message.enabled)
        }
    }
}

class C2SInteractTankTransfer(
    override val windowId: Int,
    override val uiIndex: Int,
    private val transferType: TransferType
) : UiElementMessage {
    object Serializer : NetworkSerializer<C2SInteractTankTransfer> {
        override fun serializeToNetwork(buf: PacketBuffer, x: C2SInteractTankTransfer) {
            buf.writeByte(
                when (x.transferType) {
                    TransferType.INSERT -> x.windowId
                    TransferType.EXTRACT -> x.windowId or 0x80
                }
            )
            buf.writeVarInt(x.uiIndex)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): C2SInteractTankTransfer {
            val k = buf.readByte()
            return C2SInteractTankTransfer(
                k.toInt() and 0x7F,
                buf.readVarInt(),
                if (k and 0x80.toByte() != 0.toByte()) TransferType.EXTRACT else TransferType.INSERT
            )
        }
    }

    object Handler : UiElementMessageHandler<C2SInteractTankTransfer>() {
        @ServerSide
        override fun handleUiElementMessage(
            netHandler: NetHandlerPlayServer,
            uiElem: UiElement,
            message: C2SInteractTankTransfer
        ) {
            if (uiElem !is Listener) return
            uiElem.handleTankTransfer(netHandler.player, message.transferType)
        }
    }

    interface Listener {
        @ServerSide
        fun handleTankTransfer(player: EntityPlayerMP, transferType: TransferType)
    }
}

interface UiElementMessage {
    val windowId: Int
    val uiIndex: Int
}

abstract class RawUiElementMessageHandler<M : UiElementMessage> : C2SMessageHandler<M> {
    @ServerSide
    override fun handleC2SMessage(netHandler: NetHandlerPlayServer, message: M) {
        netHandler.onServerThread {
            val container = netHandler.player.openContainer ?: return@onServerThread
            if (container.windowId != message.windowId || container !is UiContainer) return@onServerThread
            val uiElems = container.uiElements
            if (message.uiIndex !in uiElems.indices) return@onServerThread
            handleRawUiElementMessage(netHandler, uiElems[message.uiIndex], message)
        }
    }

    @ServerSide
    protected abstract fun handleRawUiElementMessage(netHandler: NetHandlerPlayServer, uiElem: UiElement, message: M)
}

abstract class UiElementMessageHandler<M : UiElementMessage> : RawUiElementMessageHandler<M>() {
    @ServerSide
    override fun handleRawUiElementMessage(netHandler: NetHandlerPlayServer, uiElem: UiElement, message: M) {
        handleUiElementMessage(netHandler, uiElem.unwrap(), message)
    }

    @ServerSide
    protected abstract fun handleUiElementMessage(netHandler: NetHandlerPlayServer, uiElem: UiElement, message: M)
}
