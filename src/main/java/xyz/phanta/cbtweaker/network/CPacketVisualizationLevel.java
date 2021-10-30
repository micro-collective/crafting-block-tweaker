package xyz.phanta.cbtweaker.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import xyz.phanta.cbtweaker.structure.VisualizationToolItem;

import javax.annotation.Nullable;

@SuppressWarnings("NotNullFieldNotInitialized")
public class CPacketVisualizationLevel implements IMessage {

    private EnumHand hand;
    @Nullable
    private Integer level;

    public CPacketVisualizationLevel(EnumHand hand, @Nullable Integer level) {
        this.hand = hand;
        this.level = level;
    }

    public CPacketVisualizationLevel() {
        // NO-OP
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte mask = 0;
        if (hand == EnumHand.OFF_HAND) {
            mask |= 0x1;
        }
        if (level != null) {
            mask |= 0x2;
        }
        buf.writeByte(mask);
        if (level != null) {
            buf.writeShort(level);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        byte mask = buf.readByte();
        hand = (mask & 0x1) != 0 ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        level = (mask & 0x2) != 0 ? (int)buf.readShort() : null;
    }

    public static class Handler implements IMessageHandler<CPacketVisualizationLevel, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketVisualizationLevel message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack stack = player.getHeldItem(message.hand);
                if (stack.getItem() == VisualizationToolItem.ITEM) {
                    VisualizationToolItem.setLevel(stack, message.level);
                }
            });
            return null;
        }

    }

}
