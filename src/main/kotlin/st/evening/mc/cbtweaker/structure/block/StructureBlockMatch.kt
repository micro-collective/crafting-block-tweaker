package st.evening.mc.cbtweaker.structure.block

import st.evening.mc.cbtweaker.hatch.HatchTileEntity

sealed interface StructureBlockMatch {
    object Normal : StructureBlockMatch

    data class Hatch(val groupId: String, val hatch: HatchTileEntity) : StructureBlockMatch

    data class Component(val componentId: String) : StructureBlockMatch

    companion object {
        fun maybeComponent(componentId: String?): StructureBlockMatch = componentId?.let { Component(it) } ?: Normal
    }
}
