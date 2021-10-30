package xyz.phanta.cbtweaker.singleblock;

import io.github.phantamanta44.libnine.tile.L9TileEntityTicking;
import io.github.phantamanta44.libnine.tile.RegisterTile;
import io.github.phantamanta44.libnine.util.data.serialization.AutoSerialize;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.common.ActivatableMachine;
import xyz.phanta.cbtweaker.common.MachineDataHolder;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.util.block.MachineSideHandler;
import xyz.phanta.cbtweaker.util.block.RedstoneControlHandler;

import java.util.List;

@RegisterTile(CbtMod.MOD_ID)
public class SingleBlockMachineTileEntity extends L9TileEntityTicking implements ActivatableMachine {

    @AutoSerialize
    private final MachineDataHolder<SingleBlockData<?, ?, ?, ?, ?>> dataHolder = new MachineDataHolder<>(
            () -> SingleBlockData.construct(this,
                    ((SingleBlockMachineBlock)getWorld().getBlockState(getPos()).getBlock()).getSingleBlockType()));

    public SingleBlockMachineTileEntity() {
        markRequiresSync();
        setInitialized();
    }

    @Override
    protected ICapabilityProvider initCapabilities() {
        return getSideHandler();
    }

    public SingleBlockType<?, ?, ?> getSingleBlockType() {
        return dataHolder.getData().getSingleBlockType();
    }

    @Override
    public boolean isActive() {
        return dataHolder.getData().isActive();
    }

    public MachineSideHandler getSideHandler() {
        return dataHolder.getData().getSideHandler();
    }

    public RedstoneControlHandler getRedstoneHandler() {
        return dataHolder.getData().getRedstoneHandler();
    }

    public List<Pair<Object, UiElement>> createBufferUiElements() {
        return dataHolder.getData().createBufferUiElements();
    }

    public UiElement createRecipeLogicUiElement() {
        return dataHolder.getData().createRecipeLogicUiElement();
    }

    @Override
    public void setDirty() {
        super.setDirty();
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
