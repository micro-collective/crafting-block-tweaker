package st.evening.mc.cbtweaker.common

import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler

interface MachineTileEntity {
    val isActive: Boolean

    val rsHandler: RedstoneControlHandler?
}
