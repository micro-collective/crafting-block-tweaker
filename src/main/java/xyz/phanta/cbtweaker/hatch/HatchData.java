package xyz.phanta.cbtweaker.hatch;

import io.github.phantamanta44.libnine.util.TriBool;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.buffer.BufferObserver;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerTileEntity;
import xyz.phanta.cbtweaker.util.RefreshState;
import xyz.phanta.cbtweaker.util.block.AutoExportHandler;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class HatchData<B> implements BufferObserver, ISerializable {

    private static final Set<EnumFacing> ALL_FACES = EnumSet.allOf(EnumFacing.class);

    private final HatchTileEntity hatch;
    private final HatchType<B> hatchType;
    private final int hatchTier;
    private final B buffer;

    private final AutoExportHandler exportHandler;

    private final Set<MultiBlockControllerTileEntity> linkedMbControllers = new HashSet<>();
    private RefreshState dirtyState = RefreshState.NONE;

    public HatchData(HatchTileEntity hatch, HatchType<B> hatchType, int hatchTier) {
        this.hatch = hatch;
        this.hatchType = hatchType;
        this.hatchTier = hatchTier;
        World world = hatch.getWorld();
        BlockPos pos = hatch.getPos();
        this.buffer = hatchType.getTier(hatchTier).createBuffer(world, pos, this);
        TriBool defExportState = hatchType.getBufferType().getDefaultExportState(buffer);
        this.exportHandler = defExportState == TriBool.NONE ? AutoExportHandler.Noop.INSTANCE
                : new AutoExportHandler.Impl<>(hatchType.getBufferType(), buffer,
                () -> ALL_FACES, hatch::setDirty, defExportState.value);
    }

    public HatchType<B> getHatchType() {
        return hatchType;
    }

    public int getHatchTier() {
        return hatchTier;
    }

    public B getBuffer() {
        return buffer;
    }

    public AutoExportHandler getExportHandler() {
        return exportHandler;
    }

    public UiElement createUiElement() {
        return hatchType.getBufferType().createUiElement(buffer);
    }

    public void attachCapabilities(ICapabilityInstanceConsumer target) {
        hatchType.getBufferType().attachCapabilities(target, buffer);
    }

    @Override
    public void onIngredientsChanged() {
        dirtyState = RefreshState.SOFT_REFRESH;
        hatch.setDirty();
    }

    @Override
    public void onComponentsChanged() {
        dirtyState = RefreshState.HARD_REFRESH;
        hatch.setDirty();
    }

    public void associate(MultiBlockControllerTileEntity mbCtrl) {
        linkedMbControllers.add(mbCtrl);
    }

    public void disassociate(MultiBlockControllerTileEntity mbCtrl) {
        linkedMbControllers.remove(mbCtrl);
    }

    public void addToGroup(BufferGroup group) {
        group.addBuffer(hatchType.getBufferType(), buffer);
    }

    public boolean handleInteraction(World world, BlockPos pos, IBlockState state,
                                     EntityPlayer player, EnumHand hand, EnumFacing face) {
        return hatchType.getBufferType().handleInteraction(buffer, state, player, hand, face);
    }

    public void dropContents(World world, BlockPos pos) {
        hatchType.getBufferType().dropContents(buffer);
    }

    public void tick() {
        if (dirtyState != RefreshState.NONE) {
            boolean compsDirty = dirtyState == RefreshState.HARD_REFRESH;
            Iterator<MultiBlockControllerTileEntity> iter = linkedMbControllers.iterator();
            while (iter.hasNext()) {
                MultiBlockControllerTileEntity mbCtrl = iter.next();
                if (mbCtrl.isInvalid()) {
                    iter.remove();
                } else {
                    mbCtrl.notifyHatchChanged(compsDirty);
                }
            }
            dirtyState = RefreshState.NONE;
        }
        hatchType.getBufferType().tick(buffer);
        exportHandler.tick();
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        hatchType.getBufferType().serializeBufferBytes(data, buffer);
        data.writeBool(exportHandler.isAutoExporting());
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        hatchType.getBufferType().deserializeBufferBytes(data, buffer);
        exportHandler.setAutoExporting(data.readBool());
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        hatchType.getBufferType().serializeBufferNbt(tag, buffer);
        exportHandler.writeToNbt(tag, "AutoExport");
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        hatchType.getBufferType().deserializeBufferNbt(tag, buffer);
        exportHandler.readFromNbt(tag, "AutoExport");
    }

}
