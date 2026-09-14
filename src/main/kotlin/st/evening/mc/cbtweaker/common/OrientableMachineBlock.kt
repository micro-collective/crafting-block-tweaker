package st.evening.mc.cbtweaker.common

import net.minecraft.block.properties.PropertyBool
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.util.system.Magic
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.findTileEntity

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
@Magic.Use
abstract class OrientableMachineBlock(blockConfig: BlockConfig) :
    CbtCustomBlock(blockConfig), BlockSidedIfc by BlockSidedIfc.Delegate(Magic.getThis()) {
    companion object {
        val PROP_ACTIVE: PropertyBool = PropertyBool.create("active")
    }

    init {
        defaultState = blockState.baseState
            .withProperty(BlockSidedIfc.PROP_FACING, BlockSide.NORTH)
            .withProperty(PROP_ACTIVE, false)
    }

    override fun createBlockState(): BlockStateContainer =
        BlockStateContainer(this, BlockSidedIfc.PROP_FACING, PROP_ACTIVE)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getActualState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState =
        world.findTileEntity<MachineTileEntity>(pos)?.let { state.withProperty(PROP_ACTIVE, it.isActive) } ?: state
}
