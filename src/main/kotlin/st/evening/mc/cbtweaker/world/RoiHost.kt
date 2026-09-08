package st.evening.mc.cbtweaker.world

import net.minecraft.util.math.BlockPos

interface RoiTicket {
    fun invalidateRoi()
}

interface RoiHost {
    val isValidRoiHost: Boolean

    fun onRegionChanged(ticket: RoiTicket, pos: BlockPos)
}
