package st.evening.mc.cbtweaker.structure

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumRarity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.common.IRarity
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.data.getIntOrNull
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.dataTagOrNull
import st.evening.mc.prelude.api.util.game.getOrCreateDataTag
import st.evening.mc.prelude.api.util.game.onStrongClient
import st.evening.mc.prelude.api.util.math.BlockPosSerializer

class VisualizationToolItem : Item() {
    init {
        setMaxStackSize(1)
    }

    override fun onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult<ItemStack> {
        onStrongClient {
            Minecraft.getMinecraft().displayGuiScreen(VisualizationGui())
        }
        return ActionResult(EnumActionResult.SUCCESS, player.getHeldItem(hand))
    }

    override fun onItemUse(
        player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): EnumActionResult {
        if (!player.isSneaking) return EnumActionResult.PASS
        val stack = player.getHeldItem(hand)
        setBoundDim(stack, world.provider.dimension)
        setBoundPos(stack, pos)
        setLevel(stack, null)
        return EnumActionResult.SUCCESS
    }

    override fun getForgeRarity(stack: ItemStack): IRarity = EnumRarity.RARE

    @ClientSide.Physical
    override fun addInformation(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<String>,
        tooltipFlags: ITooltipFlag
    ) {
        tooltip += "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_VIS_TOOL)}"
        tooltip += ""
        tooltip += getBoundPos(stack)?.let { "${TextFormatting.GOLD}(${it.x}, ${it.y}, ${it.z})" }
            ?: "${TextFormatting.RED}${I18n.format(CbtLang.TOOLTIP_BIND_TO_BLOCK)}"
    }

    companion object {
        private const val SER_DIM: String = "dim"
        private const val SER_POS: String = "pos"
        private const val SER_LEVEL: String = "level"

        fun setBoundDim(stack: ItemStack, dimId: Int) {
            stack.getOrCreateDataTag().setInteger(SER_DIM, dimId)
        }

        fun getBoundDim(stack: ItemStack): Int = stack.dataTagOrNull?.getInteger(SER_DIM) ?: 0

        fun setBoundPos(stack: ItemStack, pos: BlockPos?) {
            if (pos != null) {
                stack.getOrCreateDataTag().setTag(SER_POS, BlockPosSerializer.serializeToNbt(pos))
            } else {
                stack.dataTagOrNull?.removeTag(SER_POS)
            }
        }

        fun getBoundPos(stack: ItemStack): BlockPos? = stack.dataTagOrNull?.getCompoundOrNull(SER_POS)?.let {
            BlockPosSerializer.deserializeFromNbt(it)
        }

        fun setLevel(stack: ItemStack, level: Int?) {
            if (level != null) {
                stack.getOrCreateDataTag().setInteger(SER_LEVEL, level)
            } else {
                stack.dataTagOrNull?.removeTag(SER_LEVEL)
            }
        }

        fun getLevel(stack: ItemStack): Int? = stack.dataTagOrNull?.getIntOrNull(SER_LEVEL)
    }
}
