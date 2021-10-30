package xyz.phanta.cbtweaker.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import xyz.phanta.cbtweaker.gui.inventory.CbtContainer;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;

import javax.annotation.Nullable;

@SuppressWarnings("NotNullFieldNotInitialized")
public class CPacketUiElementInteraction implements IMessage {

    private int elementIndex;
    private byte[] data;

    public CPacketUiElementInteraction(int elementIndex, byte[] data) {
        this.elementIndex = elementIndex;
        this.data = data;
    }

    public CPacketUiElementInteraction() {
        // NO-OP
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(elementIndex).writeByte(data.length).writeBytes(data);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        elementIndex = buf.readByte();
        data = new byte[buf.readByte()];
        buf.readBytes(data);
    }

    public static class Handler implements IMessageHandler<CPacketUiElementInteraction, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketUiElementInteraction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Container cont = player.openContainer;
                if (cont instanceof CbtContainer) {
                    UiElement element = ((CbtContainer)cont).getUiElement(message.elementIndex);
                    if (element != null) {
                        element.onInteraction(message.data, player);
                    }
                }
            });
            return null;
        }

    }

}
