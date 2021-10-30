package xyz.phanta.cbtweaker.multiblock;

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

public class MultiBlockControllerBlock extends OrientableMachineBlock implements ITileEntityProvider {

    private final MultiBlockType<?, ?, ?> mbType;

    public MultiBlockControllerBlock(MultiBlockType<?, ?, ?> mbType) {
        super(mbType.getBlockMaterial());
        this.mbType = mbType;
    }

    public MultiBlockType<?, ?, ?> getMultiBlockType() {
        return mbType;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new MultiBlockControllerTileEntity();
    }

    @Override
    public String getTranslationKey() {
        return CbtMod.MOD_ID + ".multiblock." + mbType.getId() + ".controller";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof MultiBlockControllerTileEntity) {
            ((MultiBlockControllerTileEntity)tile).getRedstoneHandler().setRedstoneState(world.isBlockPowered(pos));
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        CbtMod.INSTANCE.getGuiHandler().openGui(player, CbtGuiIds.MB_CONTROLLER, new WorldBlockPos(world, pos));
        return true;
    }

}
