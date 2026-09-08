package st.evening.mc.cbtweaker.structure.block

import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextFormatting
import st.evening.mc.prelude.api.util.game.ClientSide

interface StructureBlockVisualization {
    val blockState: IBlockState

    val representative: ItemStack
        get() {
            val state = blockState
            val block = state.block
            return ItemStack(block, 1, block.damageDropped(state))
        }

    @ClientSide.Physical
    fun getTooltip(tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        val stack = representative
        if (!stack.isEmpty) {
            val iter = stack.getTooltip(Minecraft.getMinecraft().player, tooltipFlags).iterator()
            if (iter.hasNext()) {
                tooltip += "${stack.item.getForgeRarity(stack).color}${iter.next()}"
                iter.forEach {
                    tooltip += "${TextFormatting.GRAY}$it"
                }
                return
            }
        }
        tooltip.add(blockState.block.localizedName)
    }

    class State(override val blockState: IBlockState) : StructureBlockVisualization
}
