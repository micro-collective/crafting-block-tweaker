package st.evening.mc.cbtweaker.structure.block.impl

import net.minecraft.block.state.IBlockState
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.hatch.HatchBlock
import st.evening.mc.cbtweaker.hatch.HatchTileEntity
import st.evening.mc.cbtweaker.hatch.HatchType
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatch
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcherType
import st.evening.mc.cbtweaker.structure.block.StructureBlockVisualization
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.useInt
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.getColouredTooltip
import st.evening.mc.prelude.api.util.world.findTileEntity

class HatchStructureBlockMatcher(
    private val hatchType: HatchType<*>,
    private val groupId: String,
    private val tierRange: IntRange
) : StructureBlockMatcher {
    constructor(hatchType: HatchType<*>, groupId: String, tierMin: Int, tierMax: Int) :
        this(hatchType, groupId, tierMin..tierMax)

    override val visualization: List<StructureBlockVisualization> by lazy {
        tierRange.map { tier ->
            object : StructureBlockVisualization {
                override val blockState: IBlockState = hatchType.getHatchBlock(tier)

                override val representative: ItemStack = hatchType.getHatchStack(1, tier)

                @ClientSide.Physical
                override fun getTooltip(tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
                    representative.getColouredTooltip(tooltip, tooltipFlags)
                    val groupIdString = "${TextFormatting.WHITE}$groupId"
                    if (tooltipFlags.isAdvanced) {
                        tooltip += "${TextFormatting.AQUA}${I18n.format(CbtLang.TOOLTIP_BUFFER_GROUP, groupIdString)}"
                    }
                }
            }
        }
    }

    override fun matchBlock(world: World, pos: BlockPos, rotation: Rotation): StructureBlockMatch? {
        val state = world.getBlockState(pos).withRotation(rotation)
        val block = state.getBlock()
        if (block !is HatchBlock || block.hatchType != hatchType || state.getValue(block.tierProperty) !in tierRange) {
            return null
        }
        val hatch = world.findTileEntity<HatchTileEntity>(pos) ?: return null // uh oh
        return StructureBlockMatch.Hatch(groupId, hatch)
    }

    object Type : StructureBlockMatcherType {
        override val id: ResourceLocation = CbTweaker.resource("hatch")

        context(_: JsonPath)
        override fun loadMatcher(dto: TJson.Object): StructureBlockMatcher {
            val hatchType = dto.useStringValue("hatch") {
                CbTweaker.defns.hatches[it] ?: throw SerializationException.withPath("Unknown hatch type: $it")
            }
            val tierMin = dto.useInt("tier_min") {
                if (it < 0 || it >= hatchType.tierCount) {
                    throw SerializationException.withPath("Hatch tier out of bounds!")
                }
                return@useInt it
            } ?: 0
            return HatchStructureBlockMatcher(
                hatchType,
                dto.getStringValue("group"),
                tierMin,
                dto.useInt("tier_max") {
                    if (it < tierMin) throw SerializationException.withPath("Maximum hatch tier below minimum!")
                    if (it >= hatchType.tierCount) throw SerializationException.withPath("Hatch tier out of bounds!")
                    return@useInt it
                } ?: (hatchType.tierCount - 1)
            )
        }
    }
}
