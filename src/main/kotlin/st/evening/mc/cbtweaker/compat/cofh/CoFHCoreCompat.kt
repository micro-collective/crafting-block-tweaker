package st.evening.mc.cbtweaker.compat.cofh

import cofh.api.block.IDismantleable
import cofh.api.item.IToolHammer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.prelude.api.util.game.RequireMod
import st.evening.mc.prelude.api.util.game.ifModLoaded
import st.evening.mc.prelude.api.util.world.onServer

object CoFHCoreCompat {
    const val MOD_ID: String = "cofhcore"

    private val wrenchHandler: WrenchHandler = ifModLoaded(MOD_ID) { WrenchHandler.Impl() } ?: WrenchHandler.Noop

    fun tryWrenchDismantle(
        world: World,
        pos: BlockPos,
        state: IBlockState,
        player: EntityPlayer,
        hand: EnumHand
    ): Boolean = wrenchHandler.tryWrenchDismantle(world, pos, state, player, hand)

    private interface WrenchHandler {
        fun tryWrenchDismantle(
            world: World,
            pos: BlockPos,
            state: IBlockState,
            player: EntityPlayer,
            hand: EnumHand
        ): Boolean

        object Noop : WrenchHandler {
            override fun tryWrenchDismantle(
                world: World,
                pos: BlockPos,
                state: IBlockState,
                player: EntityPlayer,
                hand: EnumHand
            ): Boolean = false
        }

        @RequireMod(MOD_ID)
        class Impl : WrenchHandler {
            override fun tryWrenchDismantle(
                world: World,
                pos: BlockPos,
                state: IBlockState,
                player: EntityPlayer,
                hand: EnumHand
            ): Boolean {
                val stack = player.getHeldItem(hand)
                if (stack.isEmpty || !player.isSneaking) return false
                val block = state.block
                if (block !is IDismantleable || !block.canDismantle(world, pos, state, player)) return false
                val item = stack.item
                if (item !is IToolHammer || !item.isUsable(stack, player, pos)) return false
                world.onServer {
                    block.dismantleBlock(world, pos, state, player, false)
                    item.toolUsed(stack, player, pos)
                }
                return true
            }
        }
    }
}
