package st.evening.mc.cbtweaker.util.world

import net.minecraft.tileentity.TileEntity
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.util.world.BlockSide

class FrontGetter(private val te: TileEntity) : () -> BlockSide {
    private var front: BlockSide? = null
    private var lastCheck: Long = -1

    private fun refreshFront(): BlockSide {
        val newFront = te.world.getBlockState(te.pos).getValue(BlockSidedIfc.PROP_FACING)
        front = newFront
        lastCheck = te.world.totalWorldTime
        return newFront
    }

    override fun invoke(): BlockSide =
        if (lastCheck < te.world.totalWorldTime) refreshFront() else (front ?: refreshFront())
}
