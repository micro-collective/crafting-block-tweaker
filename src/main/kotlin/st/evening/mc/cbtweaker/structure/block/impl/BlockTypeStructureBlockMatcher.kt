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
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectString
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.game.BlockKey

class BlockTypeStructureBlockMatcher(
    private val block: BlockKey,
    private val componentId: String?,
    visualize: Boolean
) : StructureBlockMatcher {
    override val visualization: List<StructureBlockVisualization> =
        if (visualize) listOf(StructureBlockVisualization.State(block.asState())) else emptyList()

    override fun matchBlock(world: World, pos: BlockPos, rotation: Rotation): StructureBlockMatch? =
        orNull(block.matches(world.getBlockState(pos).withRotation(rotation))) {
            StructureBlockMatch.maybeComponent(componentId)
        }

    object Type : StructureBlockMatcherType {
        override val id: ResourceLocation
            get() = CbTweaker.resource("block")

        context(_: JsonPath)
        override fun loadMatcher(dto: TJson.Object): StructureBlockMatcher = BlockTypeStructureBlockMatcher(
            BlockKey.Serializer.deserializeFromJson(dto),
            dto.expectString("component"),
            dto.expectBool("visualize") ?: true
        )
    }
}
