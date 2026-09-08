package st.evening.mc.cbtweaker.structure.block

import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.util.Identifiable
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson

interface StructureBlockMatcher {
    val visualization: List<StructureBlockVisualization>

    fun matchBlock(world: World, pos: BlockPos, rotation: Rotation): StructureBlockMatch?

    object NothingMatcher : StructureBlockMatcher {
        override val visualization: List<StructureBlockVisualization>
            get() = emptyList()

        override fun matchBlock(world: World, pos: BlockPos, rotation: Rotation): StructureBlockMatch? = null
    }

    class MatcherSet(private val matchers: List<StructureBlockMatcher>) : StructureBlockMatcher {
        constructor(vararg matchers: StructureBlockMatcher) : this(matchers.asList())

        override val visualization: List<StructureBlockVisualization> by lazy {
            matchers.flatMap { it.visualization }
        }

        override fun matchBlock(world: World, pos: BlockPos, rotation: Rotation): StructureBlockMatch? =
            matchers.firstNotNullOfOrNull { it.matchBlock(world, pos, rotation) }
    }
}

fun List<StructureBlockMatcher>.joinMatchers(): StructureBlockMatcher = when (size) {
    0 -> StructureBlockMatcher.NothingMatcher
    1 -> this[0]
    else -> StructureBlockMatcher.MatcherSet(this)
}

interface StructureBlockMatcherType : Identifiable {
    context(_: JsonPath)
    fun loadMatcher(dto: TJson.Object): StructureBlockMatcher
}
