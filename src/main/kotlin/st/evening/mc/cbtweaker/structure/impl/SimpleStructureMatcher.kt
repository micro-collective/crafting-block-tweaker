package st.evening.mc.cbtweaker.structure.impl

import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.multiblock.MultiBlockType
import st.evening.mc.cbtweaker.structure.MatcherCuboid
import st.evening.mc.cbtweaker.structure.StructureMatch
import st.evening.mc.cbtweaker.structure.StructureMatcher
import st.evening.mc.cbtweaker.structure.StructureMatcherType
import st.evening.mc.cbtweaker.structure.StructureParts
import st.evening.mc.cbtweaker.structure.StructureVisualization
import st.evening.mc.cbtweaker.structure.block.impl.MultiBlockControllerStructureBlockMatcher
import st.evening.mc.cbtweaker.util.config.BlockArrayHelper
import st.evening.mc.cbtweaker.util.getRotationFromNorth
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.useArrayValue
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.world.BlockSide

class SimpleStructureMatcher(private val matchRegion: MatcherCuboid, private val allowMirror: Boolean) :
    StructureMatcher<SimpleStructureMatcher.MatchData> {

    override fun findMatch(
        world: World,
        corePos: BlockPos,
        front: BlockSide,
        prev: MatchData?,
        changedBlocks: Collection<BlockPos>
    ): StructureMatch<MatchData> {
        prev?.markDirty(changedBlocks)
        val rotation = front.getRotationFromNorth()
        return StructureMatch.mirrorMatch(
            rotation,
            prev,
            allowMirror,
            MatchData::forward,
            MatchData::mirror,
            { MatcherCuboid.IncrementalMatcher(matchRegion, world, corePos, rotation, it) },
            ::MatchData,
            StructureParts::fromMatches,
            { _, match, _ -> match.keys },
            { matchRegion.computePositions(corePos, rotation, false).asIterable() },
            { _, _ ->
                (matchRegion.computePositions(corePos, rotation, false) +
                    matchRegion.computePositions(corePos, rotation, true)).asIterable()
            },
            MatcherCuboid.IncrementalMatcher::tryMatch
        )
    }

    override fun getVisualization(data: MatchData?): StructureVisualization = StructureVisualization(
        matchRegion.posMatcherTable,
        data != null && data.mirror?.dirtyCount?.let { it < data.forward.dirtyCount } == true
    )

    class MatchData(val forward: MatcherCuboid.IncrementalMatcher, val mirror: MatcherCuboid.IncrementalMatcher?) {
        fun markDirty(changedBlocks: Collection<BlockPos>) {
            forward.markDirty(changedBlocks)
            mirror?.markDirty(changedBlocks)
        }
    }

    object Type : StructureMatcherType<MatchData> {
        override val id: ResourceLocation = CbTweaker.resource("simple")

        context(_: JsonPath)
        override fun loadMatcher(mbType: MultiBlockType<*>, dto: TJson.Object): SimpleStructureMatcher {
            val palette = dto.useObjectValue("palette") { BlockArrayHelper.loadPalette(it) }
            palette.put('@', MultiBlockControllerStructureBlockMatcher(mbType))
            val blueprint = dto.useArrayValue("structure") { BlockArrayHelper.loadBlockArray(it) }
            return SimpleStructureMatcher(
                MatcherCuboid.load(blueprint, BlockArrayHelper.findControllerPosition(blueprint), palette),
                dto.expectBool("allow_mirror") ?: false
            )
        }
    }
}
