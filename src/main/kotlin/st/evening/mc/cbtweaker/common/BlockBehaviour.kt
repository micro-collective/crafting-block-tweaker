package st.evening.mc.cbtweaker.common

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import java.util.Random

interface BlockBehaviour<T> {
    fun handlePlacement(state: T, blockState: IBlockState, placer: EntityLivingBase, stack: ItemStack) {}

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

    @ClientSide
    fun handleDisplayTick(state: T, blockState: IBlockState, rand: Random) {
    }

    interface Handler {
        fun handlePlacement(blockState: IBlockState, placer: EntityLivingBase, stack: ItemStack)

        fun handleInteraction(
            blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
            face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
        ): Boolean

        @ServerSide
        fun handleBlockUpdate(blockState: IBlockState, fromBlock: Block, fromPos: BlockPos)

        @ServerSide
        fun handleDestruction(blockState: IBlockState)

        @ClientSide.Strong
        fun handleDisplayTick(blockState: IBlockState, rand: Random)
    }
}
