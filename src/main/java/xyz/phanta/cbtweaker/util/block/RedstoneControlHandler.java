package xyz.phanta.cbtweaker.util.block;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.IByteSerializable;
import io.github.phantamanta44.libnine.util.world.RedstoneBehaviour;
import net.minecraft.nbt.NBTTagCompound;

public class RedstoneControlHandler implements IByteSerializable {

    private final Runnable changeCallback;

    private RedstoneBehaviour redstoneBehaviour = RedstoneBehaviour.INVERTED;
    private boolean redstoneState = false;

    public RedstoneControlHandler(Runnable changeCallback) {
        this.changeCallback = changeCallback;
    }

    public RedstoneBehaviour getRedstoneBehaviour() {
        return redstoneBehaviour;
    }

    public void setRedstoneBehaviour(RedstoneBehaviour behaviour) {
        if (this.redstoneBehaviour != behaviour) {
            this.redstoneBehaviour = behaviour;
            changeCallback.run();
        }
    }

    public void setRedstoneState(boolean redstoneState) {
        this.redstoneState = redstoneState;
    }

    public boolean canWork() {
        return redstoneBehaviour.canWork(redstoneState);
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        byte mask = (byte)redstoneBehaviour.ordinal();
        data.writeByte(redstoneState ? (byte)(mask | 0x80) : mask);
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        int mask = data.readByte();
        redstoneBehaviour = RedstoneBehaviour.VALUES.get(mask & 0x7F);
        redstoneState = (mask & 0x80) != 0;
    }

    public void writeToNbt(NBTTagCompound tag, String key) {
        tag.setString(key, redstoneBehaviour.name());
    }

    public void readFromNbt(NBTTagCompound tag, String key) {
        try {
            redstoneBehaviour = RedstoneBehaviour.valueOf(tag.getString(key));
        } catch (IllegalArgumentException e) {
            redstoneBehaviour = RedstoneBehaviour.INVERTED;
        }
    }

}
