package st.evening.mc.cbtweaker.structure

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import st.evening.mc.cbtweaker.multiblock.MultiBlockType
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.util.Identifiable
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.util.world.BlockSide

interface StructureMatcher {
    val visualization: Map<Vec3i, StructureBlockMatcher>

    fun getRegion(world: World, corePos: BlockPos, front: BlockSide): Iterator<BlockPos>

    fun findMatch(world: World, corePos: BlockPos, front: BlockSide): StructureMatch?
}

interface StructureMatcherType : Identifiable {
    context(_: JsonPath)
    fun loadMatcher(mbType: MultiBlockType<*>, dto: TJson.Object): StructureMatcher
}
