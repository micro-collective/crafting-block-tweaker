package st.evening.mc.cbtweaker.common

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import st.evening.mc.prelude.api.util.game.ServerSide

interface BlockBehaviour<T> {
    fun handleInteraction(
        state: T, blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
        face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean = false

    @ServerSide
    fun handleBlockUpdate(state: T, blockState: IBlockState, fromBlock: Block, fromPos: BlockPos) {
    }

    @ServerSide
    fun handleDestruction(state: T, blockState: IBlockState) {
    }
}
