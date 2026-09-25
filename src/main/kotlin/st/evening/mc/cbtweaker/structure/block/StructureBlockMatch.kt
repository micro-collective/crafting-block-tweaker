package st.evening.mc.cbtweaker.structure.block

import st.evening.mc.cbtweaker.hatch.HatchTileEntity
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectStringValue

sealed interface StructureBlockMatch {
    object Normal : StructureBlockMatch

    data class Hatch(val groupId: String, val hatch: HatchTileEntity) : StructureBlockMatch

    data class Component(val componentId: String, val count: Int) : StructureBlockMatch {
        companion object {
            context(_: JsonPath)
            fun load(dto: TJson): Component = when (dto) {
                is TJson.String -> Component(dto.value, 1)
                is TJson.Object -> Component(
                    dto.expectStringValue("id"),
                    dto.expectInt("count") ?: 1
                )
                else -> throw SerializationException.withPath("Expected a component ID or an object!")
            }
        }
    }
}
