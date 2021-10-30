package xyz.phanta.cbtweaker.multiblock;

import io.github.phantamanta44.libnine.tile.L9TileEntityTicking;
import io.github.phantamanta44.libnine.tile.RegisterTile;
import io.github.phantamanta44.libnine.util.data.serialization.AutoSerialize;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.common.ActivatableMachine;
import xyz.phanta.cbtweaker.common.MachineDataHolder;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.util.block.RedstoneControlHandler;

@RegisterTile(CbtMod.MOD_ID)
public class MultiBlockControllerTileEntity extends L9TileEntityTicking implements ActivatableMachine {

    @AutoSerialize
    private final MachineDataHolder<MultiBlockData<?, ?, ?, ?, ?>> dataHolder = new MachineDataHolder<>(
            () -> MultiBlockData.construct(this,
                    ((MultiBlockControllerBlock)getWorld().getBlockState(getPos()).getBlock()).getMultiBlockType()));

    public MultiBlockControllerTileEntity() {
        markRequiresSync();
        setInitialized();
    }

    public MultiBlockType<?, ?, ?> getMultiBlockType() {
        return dataHolder.getData().getMultiBlockType();
    }

    public boolean isAssembled() {
        return dataHolder.getData().isAssembled();
    }

    @Override
    public boolean isActive() {
        return dataHolder.getData().isActive();
    }

    public RedstoneControlHandler getRedstoneHandler() {
        return dataHolder.getData().getRedstoneHandler();
    }

    public UiElement createRecipeLogicUiElement() {
        return dataHolder.getData().createRecipeLogicUiElement();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        dataHolder.getData().onInvalidated();
    }

    public void notifyHatchChanged(boolean compsDirty) {
        dataHolder.getData().notifyHatchChanged(compsDirty);
    }

    @Override
    public void setDirty() {
        super.setDirty();
    }

    @Override
    protected void tick() {
        dataHolder.getData().tick();
    }

}
