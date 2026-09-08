package st.evening.mc.cbtweaker.structure.block.impl

import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatch
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcherType
import st.evening.mc.cbtweaker.structure.block.StructureBlockVisualization
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.resource

object AirStructureBlockMatcher : StructureBlockMatcher {
    override val visualization: List<StructureBlockVisualization>
        get() = emptyList()

    override fun matchBlock(world: World, pos: BlockPos, rotation: Rotation): StructureBlockMatch? =
        if (world.isAirBlock(pos)) StructureBlockMatch.Normal else null

    object Type : StructureBlockMatcherType {
        override val id: ResourceLocation = CbTweaker.resource("air")

        context(_: JsonPath)
        override fun loadMatcher(dto: TJson.Object): StructureBlockMatcher = AirStructureBlockMatcher
    }
}
