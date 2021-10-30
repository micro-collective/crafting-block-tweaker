package xyz.phanta.cbtweaker.util.block;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.phantamanta44.libnine.util.TriBool;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.world.BlockSide;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.util.CapabilityMerger;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MachineSideHandler implements ICapabilityProvider, ISerializable {

    private final Supplier<Direction> dirGetter;
    private final Multimap<Capability<?>, Object> unsidedCapabilities
            = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
    private final Map<Object, SideConfig<?>> sideConfigTable = new LinkedHashMap<>();

    public MachineSideHandler(World world, BlockPos pos, Supplier<Direction> dirGetter, Runnable changeCallback,
                              SortedMap<String, BufferGroup> bufGroups) {
        this.dirGetter = dirGetter;
        for (Map.Entry<String, BufferGroup> bufGroupEntry : bufGroups.entrySet()) {
            bufGroupEntry.getValue().forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    for (B buffer : buffers) {
                        if (bufType.isCapabilitySided(buffer)) {
                            sideConfigTable.put(buffer, new SideConfig<>(
                                    bufType, buffer, world, pos, dirGetter, changeCallback));
                        } else {
                            bufType.attachCapabilities(unsidedCapabilities::put, buffer);
                        }
                    }
                }
            });
        }
    }

    @Nullable
    public SideConfig<?> getSideConfig(Object buffer) {
        return sideConfigTable.get(buffer);
    }

    public Stream<SideConfig<?>> getSideConfigs(EnumFacing facing) {
        BlockSide side = BlockSide.fromDirection(dirGetter.get().face, facing);
        return sideConfigTable.values().stream().filter(c -> c.isEnabled(side));
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (unsidedCapabilities.containsKey(capability)) {
            return true;
        }
        if (facing == null) {
            for (SideConfig<?> sideConfig : sideConfigTable.values()) {
                if (sideConfig.getCapability(capability) != null) {
                    return true;
                }
            }
        } else {
            BlockSide side = BlockSide.fromDirection(dirGetter.get().face, facing);
            for (SideConfig<?> sideConfig : sideConfigTable.values()) {
                if (sideConfig.isEnabled(side) && sideConfig.getCapability(capability) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        List<T> capInstances = new ArrayList<>();
        if (facing == null) {
            for (SideConfig<?> sideConfig : sideConfigTable.values()) {
                T capInstance = sideConfig.getCapability(capability);
                if (capInstance != null) {
                    capInstances.add(capInstance);
                }
            }
        } else {
            BlockSide side = BlockSide.fromDirection(dirGetter.get().face, facing);
            for (SideConfig<?> sideConfig : sideConfigTable.values()) {
                if (sideConfig.isEnabled(side)) {
                    T capInstance = sideConfig.getCapability(capability);
                    if (capInstance != null) {
                        capInstances.add(capInstance);
                    }
                }
            }
        }
        capInstances.addAll((Collection<T>)unsidedCapabilities.get(capability));
        return capInstances.isEmpty() ? null : CapabilityMerger.REGISTRY.merge(capability, capInstances);
    }

    public void tick() {
        for (SideConfig<?> sideConfig : sideConfigTable.values()) {
            sideConfig.tick();
        }
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        for (SideConfig<?> sideConfig : sideConfigTable.values()) {
            sideConfig.serBytes(data);
        }
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        for (SideConfig<?> sideConfig : sideConfigTable.values()) {
            sideConfig.deserBytes(data);
        }
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        NBTTagList configsDto = new NBTTagList();
        for (SideConfig<?> sideConfig : sideConfigTable.values()) {
            NBTTagCompound sideConfigDto = new NBTTagCompound();
            sideConfig.serNBT(sideConfigDto);
            configsDto.appendTag(sideConfigDto);
        }
        tag.setTag("SideConfigs", configsDto);
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        NBTTagList configsDto = tag.getTagList("SideConfigs", Constants.NBT.TAG_COMPOUND);
        Iterator<NBTBase> configDtoIter = configsDto.iterator();
        Iterator<SideConfig<?>> sideConfigIter = sideConfigTable.values().iterator();
        while (configDtoIter.hasNext() && sideConfigIter.hasNext()) {
            sideConfigIter.next().deserNBT((NBTTagCompound)configDtoIter.next());
        }
    }

    public static class SideConfig<B> implements ISerializable {

        private final BufferType<B, ?, ?, ?> bufType;
        private final B buffer;
        private final Runnable changeCallback;

        private byte dirMask = 0;
        private final Map<Capability<?>, Object> capabilities = new HashMap<>();
        private final AutoExportHandler exportHandler;

        public SideConfig(BufferType<B, ?, ?, ?> bufType, B buffer,
                          World world, BlockPos pos, Supplier<Direction> dirGetter, Runnable changeCallback) {
            this.bufType = bufType;
            this.buffer = buffer;
            this.changeCallback = changeCallback;
            TriBool defExportState = bufType.getDefaultExportState(buffer);
            this.exportHandler = defExportState == TriBool.NONE ? AutoExportHandler.Noop.INSTANCE
                    : new AutoExportHandler.Impl<>(bufType, buffer,
                    () -> {
                        Set<EnumFacing> faces = EnumSet.noneOf(EnumFacing.class);
                        EnumFacing frontFace = dirGetter.get().face;
                        for (BlockSide side : BlockSide.VALUES) {
                            if (isEnabled(side)) {
                                faces.add(side.getDirection(frontFace));
                            }
                        }
                        return faces;
                    }, changeCallback, defExportState.value);
            bufType.attachCapabilities(capabilities::put, buffer);
            bufType.configureDefaultSides(this);
        }

        public BufferType<B, ?, ?, ?> getBufferType() {
            return bufType;
        }

        public B getBuffer() {
            return buffer;
        }

        public boolean isEnabled(BlockSide side) {
            return (dirMask & (1 << side.ordinal())) != 0;
        }

        public void setEnabled(BlockSide side, boolean enabled) {
            if (enabled) {
                dirMask |= 1 << side.ordinal();
            } else {
                dirMask &= ~(1 << side.ordinal());
            }
            changeCallback.run();
        }

        public void toggle(BlockSide side) {
            setEnabled(side, !isEnabled(side));
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public <T> T getCapability(Capability<T> capability) {
            return (T)capabilities.get(capability);
        }

        public AutoExportHandler getExportHandler() {
            return exportHandler;
        }

        private void tick() {
            bufType.tick(buffer);
            exportHandler.tick();
        }

        @Override
        public void serBytes(ByteUtils.Writer data) {
            byte mask = dirMask;
            if (exportHandler.isAutoExporting()) {
                mask |= 1 << BlockSide.VALUES.size();
            }
            data.writeByte(mask);
        }

        @Override
        public void deserBytes(ByteUtils.Reader data) {
            byte mask = data.readByte();
            int exportStateMask = 1 << BlockSide.VALUES.size();
            dirMask = (byte)(mask & (exportStateMask - 1));
            exportHandler.setAutoExporting((mask & exportStateMask) != 0);
        }

        @Override
        public void serNBT(NBTTagCompound tag) {
            for (BlockSide side : BlockSide.VALUES) {
                tag.setBoolean(side.name(), isEnabled(side));
            }
            exportHandler.writeToNbt(tag, "AutoExport");
        }

        @Override
        public void deserNBT(NBTTagCompound tag) {
            for (BlockSide side : BlockSide.VALUES) {
                setEnabled(side, tag.getBoolean(side.name()));
            }
            exportHandler.readFromNbt(tag, "AutoExport");
        }

    }

}
