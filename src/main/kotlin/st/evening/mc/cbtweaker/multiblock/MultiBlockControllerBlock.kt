package st.evening.mc.cbtweaker.multiblock

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.common.CustomBlockType
import st.evening.mc.cbtweaker.common.OrientableMachineBlock
import st.evening.mc.prelude.api.block.TileEntityBlock
import st.evening.mc.prelude.api.registration.TileEntityType
import st.evening.mc.prelude.api.registration.openContainer
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.assertLogicalServer
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.prelude.api.util.world.useTileEntity

class MultiBlockControllerBlock private constructor(val mbType: MultiBlockType<*>) :
    OrientableMachineBlock(mbType.blockConfig.material), TileEntityBlock {
    companion object {
        fun construct(mbType: MultiBlockType<*>): MultiBlockControllerBlock =
            MultiBlockControllerBlock(mbType).also { it.init() }
    }

    override val blockType: CustomBlockType
        get() = mbType

    override fun getTileEntityType(world: World, meta: Int): TileEntityType<*> =
        CbTweaker.defns.tileEntityMultiBlockController

    override fun onBlockActivated(
        world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        world.useTileEntity<MultiBlockControllerTileEntity>(pos) {
            if (it.handleInteraction(state, player, hand, facing, hitX, hitY, hitZ)) return true
            world.onServer {
                player.openContainer(CbTweaker.defns.containerMultiBlockController, world, pos)
            }
            return true
        }
        return false
    }

    @OptIn(SidednessAssertion::class)
    @Suppress("OVERRIDE_DEPRECATION")
    override fun neighborChanged(state: IBlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos) {
        assertLogicalServer {
            world.useTileEntity<MultiBlockControllerTileEntity>(pos) {
                it.handleBlockUpdate(state, block, fromPos)
            }
        }
    }

    @OptIn(SidednessAssertion::class)
    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        assertLogicalServer {
            world.useTileEntity<MultiBlockControllerTileEntity>(pos) {
                it.handleDestruction(state)
            }
        }
        super.breakBlock(world, pos, state)
    }

    override fun getTranslationKey(): String = "${CbtConsts.MOD_ID}.multiblock.${mbType.id}.controller"
}
