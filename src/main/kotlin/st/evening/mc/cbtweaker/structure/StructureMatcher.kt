package st.evening.mc.cbtweaker.structure

import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.multiblock.MultiBlockType
import st.evening.mc.cbtweaker.util.Identifiable
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.util.world.BlockSide

interface StructureMatcher<M> {
    fun findMatch(
        world: World,
        corePos: BlockPos,
        front: BlockSide,
        prev: M?,
        changedBlocks: Collection<BlockPos>
    ): StructureMatch<M>

    fun getVisualization(data: M?): StructureVisualization
}

interface StructureMatcherType<M> : Identifiable {
    context(_: JsonPath)
    fun loadMatcher(mbType: MultiBlockType<*>, dto: TJson.Object): StructureMatcher<M>
}
