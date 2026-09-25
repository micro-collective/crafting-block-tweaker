package st.evening.mc.cbtweaker.structure.block

import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.util.Rotation
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.util.withMirrorX
import st.evening.mc.prelude.api.util.game.ClientSide

interface StructureBlockVisualization {
    val baseBlockState: IBlockState

    fun getBlockState(rotation: Rotation, mirrorX: Boolean): IBlockState =
        baseBlockState.withMirrorX(mirrorX).withRotation(rotation)

    val representative: ItemStack
        get() {
            val state = baseBlockState
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
        tooltip += baseBlockState.block.localizedName
    }

    class State(override val baseBlockState: IBlockState) : StructureBlockVisualization
}
