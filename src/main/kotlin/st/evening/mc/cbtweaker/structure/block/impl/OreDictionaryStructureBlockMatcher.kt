package st.evening.mc.cbtweaker.structure.block.impl

import net.minecraft.block.state.IBlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatch
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcherType
import st.evening.mc.cbtweaker.structure.block.StructureBlockVisualization
import st.evening.mc.cbtweaker.util.withMirrorX
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectStringValue
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.OreEntry
import st.evening.mc.prelude.api.util.game.getColouredTooltip
import st.evening.mc.prelude.api.util.world.unaryMinus

class OreDictionaryStructureBlockMatcher(
    private val oreEntry: OreEntry,
    private val match: StructureBlockMatch,
    private val visualize: Boolean
) : StructureBlockMatcher {
    override val visualization: List<StructureBlockVisualization> by lazy {
        if (!visualize) return@lazy emptyList()
        oreEntry.getOreStacks().mapNotNull { stack ->
            val item = stack.item
            if (item !is ItemBlock) return@mapNotNull null
            return@mapNotNull object : StructureBlockVisualization {
                @Suppress("DEPRECATION")
                override val baseBlockState: IBlockState = item.block.getStateFromMeta(stack.metadata) // questionable

                override val representative: ItemStack
                    get() = stack

                @ClientSide.Physical
                override fun getTooltip(tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
                    stack.getColouredTooltip(tooltip, tooltipFlags)
                    tooltip += "${TextFormatting.DARK_GRAY}($oreEntry)"
                }
            }
        }
    }

    override fun matchBlock(world: World, pos: BlockPos, rotation: Rotation, mirrorX: Boolean): StructureBlockMatch? {
        val state = world.getBlockState(pos).withRotation(-rotation).withMirrorX(mirrorX)
        val block = state.block
        // questionable
        return orNull(oreEntry.matches(Item.getItemFromBlock(block), block.damageDropped(state))) { match }
    }

    object Type : StructureBlockMatcherType {
        override val id: ResourceLocation = CbTweaker.resource("ore_dict")

        context(_: JsonPath)
        override fun loadMatcher(dto: TJson.Object): StructureBlockMatcher = OreDictionaryStructureBlockMatcher(
            OreEntry(dto.expectStringValue("ore")),
            dto.useAny("component") {
                StructureBlockMatch.Component.load(it)
            } ?: StructureBlockMatch.Normal,
            dto.expectBool("visualize") ?: true
        )
    }
}
