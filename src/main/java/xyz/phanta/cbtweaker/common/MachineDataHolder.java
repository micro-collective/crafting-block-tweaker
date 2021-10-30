package xyz.phanta.cbtweaker.common;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class MachineDataHolder<D extends ISerializable> implements ISerializable {

    private final Supplier<D> dataProvider;

    @Nullable
    private D machineData = null;
    @Nullable
    private NBTTagCompound bufferedDataDeser = null;

    public MachineDataHolder(Supplier<D> dataProvider) {
        this.dataProvider = dataProvider;
    }

    public D getData() {
        if (machineData == null) {
            machineData = dataProvider.get();
            if (bufferedDataDeser != null) {
                machineData.deserNBT(bufferedDataDeser);
                bufferedDataDeser = null;
            }
        }
        return machineData;
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        getData().serBytes(data);
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        getData().deserBytes(data);
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        getData().serNBT(tag);
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        if (machineData != null) {
            machineData.deserNBT(tag);
        } else {
            bufferedDataDeser = tag;
        }
    }

}
