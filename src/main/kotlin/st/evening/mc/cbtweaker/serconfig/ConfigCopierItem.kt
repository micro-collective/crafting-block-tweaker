package st.evening.mc.cbtweaker.serconfig

import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.items.ItemHandlerHelper
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.hatch.HatchBlock
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerBlock
import st.evening.mc.cbtweaker.network.S2CClientEffect
import st.evening.mc.cbtweaker.singleblock.SingleBlockMachineBlock
import st.evening.mc.prelude.api.item.prefab.PrefabItemEnum
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.dataTagOrNull
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.prelude.api.util.world.useTileEntity

class ConfigCopierItem : PrefabItemEnum<ConfigCopierItem.Type>(Type::class.java) {
    fun getTypeByMeta(meta: Int): Type = if (meta >= 0 && meta < variants.size) variants[meta] else Type.BLANK

    fun getType(stack: ItemStack): Type = getTypeByMeta(stack.metadata)

    override fun getSubItems(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        if (isInCreativeTab(tab)) {
            items += newStack(Type.BLANK, 1)
        }
    }

    override fun getItemStackLimit(stack: ItemStack): Int = if (getType(stack) == Type.BLANK) maxStackSize else 1

    override fun onItemUseFirst(
        player: EntityPlayer, world: World, pos: BlockPos,
        side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, hand: EnumHand
    ): EnumActionResult {
        if (player.isSneaking) { // copy
            val type = when (world.getBlockState(pos).block) {
                is HatchBlock -> Type.HATCH
                is MultiBlockControllerBlock -> Type.MB_CTRL
                is SingleBlockMachineBlock -> Type.SB_MACHINE
                else -> return EnumActionResult.PASS
            }
            world.onServer {
                world.useTileEntity<CopiableConfigHost>(pos) {
                    val configDto = NBTTagCompound()
                    it.writeConfig(configDto)
                    if (configDto.isEmpty) {
                        if (player is EntityPlayerMP) {
                            CbTweaker.defns.s2cClientEffect.sendTo(
                                S2CClientEffect(S2CClientEffect.Type.CONFIG_COPY_EMPTY),
                                player
                            )
                        }
                    } else {
                        val configStack = newStack(type, 1)
                        configStack.tagCompound = configDto
                        val heldStack = player.getHeldItem(hand)
                        if (heldStack.count <= 1) {
                            player.setHeldItem(hand, configStack)
                        } else {
                            heldStack.shrink(1)
                            ItemHandlerHelper.giveItemToPlayer(player, configStack)
                        }
                        if (player is EntityPlayerMP) {
                            CbTweaker.defns.s2cClientEffect.sendTo(
                                S2CClientEffect(S2CClientEffect.Type.CONFIG_COPY),
                                player
                            )
                        }
                    }
                }
            }
        } else { // paste
            val heldStack = player.getHeldItem(hand)
            when (getType(heldStack)) {
                Type.BLANK -> return EnumActionResult.PASS
                Type.HATCH -> if (world.getBlockState(pos).block !is HatchBlock) return EnumActionResult.PASS
                Type.MB_CTRL ->
                    if (world.getBlockState(pos).block !is MultiBlockControllerBlock) return EnumActionResult.PASS
                Type.SB_MACHINE ->
                    if (world.getBlockState(pos).block !is SingleBlockMachineBlock) return EnumActionResult.PASS
            }
            world.onServer {
                world.useTileEntity<CopiableConfigHost>(pos) { te ->
                    heldStack.dataTagOrNull?.let {
                        te.readConfig(it)
                        if (player is EntityPlayerMP) {
                            CbTweaker.defns.s2cClientEffect.sendTo(
                                S2CClientEffect(S2CClientEffect.Type.CONFIG_PASTE),
                                player
                            )
                        }
                    }
                }
            }
        }
        return EnumActionResult.SUCCESS
    }

    @ClientSide.Physical
    override fun addInformation(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<String>,
        tooltipFlags: ITooltipFlag
    ) {
        tooltip += "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_CONFIG_COPIER)}"
        tooltip += ""
        if (getType(stack) != Type.BLANK) {
            tooltip += "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_CONFIG_PASTE)}"
        }
        tooltip += "${TextFormatting.GRAY}${I18n.format(CbtLang.TOOLTIP_CONFIG_COPY)}"
    }

    enum class Type {
        BLANK, HATCH, MB_CTRL, SB_MACHINE
    }
}
