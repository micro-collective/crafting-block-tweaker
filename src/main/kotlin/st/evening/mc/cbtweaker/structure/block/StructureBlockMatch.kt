package st.evening.mc.cbtweaker.structure.block

import st.evening.mc.cbtweaker.hatch.HatchTileEntity
import st.evening.mc.cbtweaker.util.machine.ComponentSet

sealed interface StructureBlockMatch {
    object Normal : StructureBlockMatch

    data class Hatch(val groupId: String, val hatch: HatchTileEntity) : StructureBlockMatch

    class Components(val components: ComponentSet) : StructureBlockMatch
}
