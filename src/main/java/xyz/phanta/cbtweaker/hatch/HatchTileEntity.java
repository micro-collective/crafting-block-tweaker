package xyz.phanta.cbtweaker.hatch;

import io.github.phantamanta44.libnine.capability.provider.CapabilityBroker;
import io.github.phantamanta44.libnine.tile.L9TileEntityTicking;
import io.github.phantamanta44.libnine.tile.RegisterTile;
import io.github.phantamanta44.libnine.util.data.serialization.AutoSerialize;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.common.MachineDataHolder;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerTileEntity;
import xyz.phanta.cbtweaker.util.block.AutoExportHandler;

@RegisterTile(CbtMod.MOD_ID)
public class HatchTileEntity extends L9TileEntityTicking {

    @AutoSerialize
    private final MachineDataHolder<HatchData<?>> dataHolder = new MachineDataHolder<>(() -> {
        IBlockState state = getWorld().getBlockState(getPos());
        HatchBlock block = (HatchBlock)state.getBlock();
        return new HatchData<>(
                this, block.getHatchType(), state.getValue(block.getTierProperty()));
    });

    public HatchTileEntity() {
        markRequiresSync();
        setInitialized();
    }

    @Override
    protected ICapabilityProvider initCapabilities() {
        CapabilityBroker caps = new CapabilityBroker();
        dataHolder.getData().attachCapabilities(caps::with);
        return caps;
    }

    public HatchType<?> getHatchType() {
        return dataHolder.getData().getHatchType();
    }

    public int getTier() {
        return dataHolder.getData().getHatchTier();
    }

    public AutoExportHandler getExportHandler() {
        return dataHolder.getData().getExportHandler();
    }

    public UiElement createUiElement() {
        return dataHolder.getData().createUiElement();
    }

    @Override
    public void setDirty() {
        super.setDirty();
    }

    public void associate(MultiBlockControllerTileEntity mbCtrl) {
        dataHolder.getData().associate(mbCtrl);
    }

    public void disassociate(MultiBlockControllerTileEntity mbCtrl) {
        dataHolder.getData().disassociate(mbCtrl);
    }

    public void addToGroup(BufferGroup group) {
        dataHolder.getData().addToGroup(group);
    }

    public boolean handleInteraction(EntityPlayer player, EnumHand hand, EnumFacing face) {
        World world = getWorld();
        BlockPos pos = getPos();
        return dataHolder.getData().handleInteraction(world, pos, world.getBlockState(pos), player, hand, face);
    }

    public void dropContents() {
        dataHolder.getData().dropContents(getWorld(), getPos());
    }

    @Override
    protected void tick() {
        dataHolder.getData().tick();
    }

}
