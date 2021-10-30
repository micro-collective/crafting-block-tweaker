package xyz.phanta.cbtweaker.common;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.util.Direction;

import javax.annotation.Nullable;

public class OrientableMachineBlock extends CbtCustomBlock {

    public static final IProperty<Direction> PROP_DIRECTION = PropertyEnum.create("dir", Direction.class);
    public static final IProperty<Boolean> PROP_ACTIVE = PropertyBool.create("active");

    public OrientableMachineBlock(BlockMaterial material) {
        super(material);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PROP_DIRECTION, PROP_ACTIVE);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos,
                                            EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState()
                .withProperty(PROP_DIRECTION, Direction.getFromFace(placer.getHorizontalFacing()).getOpposite());
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(PROP_DIRECTION).ordinal();
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getStateFromMeta(int meta) {
        if (meta < 0 || meta >= Direction.VALUES.size()) {
            return getDefaultState();
        }
        return getDefaultState().withProperty(PROP_DIRECTION, Direction.VALUES.get(meta));
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof ActivatableMachine
                ? state.withProperty(PROP_ACTIVE, ((ActivatableMachine)tile).isActive()) : state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(PROP_DIRECTION, state.getValue(PROP_DIRECTION).rotate(rot));
    }

    @Nullable
    @Override
    public EnumFacing[] getValidRotations(World world, BlockPos pos) {
        return EnumFacing.HORIZONTALS;
    }

}
