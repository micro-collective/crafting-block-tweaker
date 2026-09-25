package st.evening.mc.cbtweaker.structure.block.impl

import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerBlock
import st.evening.mc.cbtweaker.multiblock.MultiBlockType
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatch
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.StructureBlockVisualization
import st.evening.mc.prelude.api.util.data.orNull

// these can't be manually used; rather, they're automatically generated during multi-block loading
class MultiBlockControllerStructureBlockMatcher(private val mbType: MultiBlockType<*>) : StructureBlockMatcher {
    override val visualization: List<StructureBlockVisualization> =
        listOf(StructureBlockVisualization.State(mbType.controllerBlock.defaultState))

    override fun matchBlock(world: World, pos: BlockPos, rotation: Rotation, mirrorX: Boolean): StructureBlockMatch? {
        val block = world.getBlockState(pos).getBlock() // could check rotation, but we probably don't need to
        return orNull(block is MultiBlockControllerBlock && block.mbType == mbType) { StructureBlockMatch.Normal }
    }
}
