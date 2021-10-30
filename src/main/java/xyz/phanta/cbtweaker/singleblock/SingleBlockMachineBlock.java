package xyz.phanta.cbtweaker.singleblock;

import io.github.phantamanta44.libnine.util.world.WorldBlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.common.OrientableMachineBlock;
import xyz.phanta.cbtweaker.gui.CbtGuiIds;

import javax.annotation.Nullable;

public class SingleBlockMachineBlock extends OrientableMachineBlock implements ITileEntityProvider {

    private final SingleBlockType<?, ?, ?> sbType;

    public SingleBlockMachineBlock(SingleBlockType<?, ?, ?> sbType) {
        super(sbType.getBlockMaterial());
        this.sbType = sbType;
    }

    public SingleBlockType<?, ?, ?> getSingleBlockType() {
        return sbType;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new SingleBlockMachineTileEntity();
    }

    @Override
    public String getTranslationKey() {
        return CbtMod.MOD_ID + ".singleblock." + sbType.getId();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof SingleBlockMachineTileEntity) {
            ((SingleBlockMachineTileEntity)tile).getRedstoneHandler().setRedstoneState(world.isBlockPowered(pos));
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        SingleBlockMachineTileEntity sbMachine = (SingleBlockMachineTileEntity)world.getTileEntity(pos);
        if (sbMachine == null) {
            return false;
        }
        if (sbMachine.handleInteraction(player, hand, facing)) {
            return true;
        }
        CbtMod.INSTANCE.getGuiHandler().openGui(player, CbtGuiIds.SB_MACHINE, new WorldBlockPos(world, pos));
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        SingleBlockMachineTileEntity sbMachine = (SingleBlockMachineTileEntity)world.getTileEntity(pos);
        if (sbMachine != null) {
            sbMachine.dropContents();
        }
        super.breakBlock(world, pos, state);
    }

}
