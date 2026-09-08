package st.evening.mc.cbtweaker.structure.impl

import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.multiblock.MultiBlockType
import st.evening.mc.cbtweaker.structure.MatcherCuboid
import st.evening.mc.cbtweaker.structure.StructureMatch
import st.evening.mc.cbtweaker.structure.StructureMatcher
import st.evening.mc.cbtweaker.structure.StructureMatcherType
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.impl.MultiBlockControllerStructureBlockMatcher
import st.evening.mc.cbtweaker.util.BlockArrayHelper
import st.evening.mc.cbtweaker.util.getRotationFromNorth
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.useArrayValue
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.world.BlockSide

class SimpleStructureMatcher(private val matchRegion: MatcherCuboid, private val allowMirror: Boolean) :
    StructureMatcher {
    override val visualization: Map<Vec3i, StructureBlockMatcher>
        get() = matchRegion.posMatcherTable

    override fun getRegion(world: World, corePos: BlockPos, front: BlockSide): Iterator<BlockPos> {
        val rotation = front.getRotationFromNorth()
        val region = matchRegion.computePositions(corePos, rotation, false)
        return if (allowMirror) {
            region + matchRegion.computePositions(corePos, rotation, true)
        } else {
            region
        }.iterator()
    }

    override fun findMatch(world: World, corePos: BlockPos, front: BlockSide): StructureMatch? {
        val rotation = front.getRotationFromNorth()
        val match = matchRegion.tryMatch(world, corePos, rotation, false)
        if (match != null) return match
        return orNull(allowMirror) { matchRegion.tryMatch(world, corePos, rotation, true) }
    }

    object Type : StructureMatcherType {
        override val id: ResourceLocation = CbTweaker.resource("simple")

        context(_: JsonPath)
        override fun loadMatcher(mbType: MultiBlockType<*>, dto: TJson.Object): StructureMatcher {
            val palette = dto.useObjectValue("palette") { BlockArrayHelper.loadPalette(it) }
            palette.put('@', MultiBlockControllerStructureBlockMatcher(mbType))
            val blueprint = dto.useArrayValue("structure") { BlockArrayHelper.loadBlockArray(it) }
            return SimpleStructureMatcher(
                MatcherCuboid.load(blueprint, BlockArrayHelper.findControllerPosition(blueprint), palette),
                dto.expectBool("allow_mirror") ?: true
            )
        }
    }
}
