package st.evening.mc.cbtweaker.hatch

import com.google.common.base.Optional
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.common.CbtCustomBlock
import st.evening.mc.cbtweaker.common.CustomBlockType
import st.evening.mc.cbtweaker.compat.cofh.CoFHCoreCompat
import st.evening.mc.prelude.api.block.CustomItemBlock
import st.evening.mc.prelude.api.block.TileEntityBlock
import st.evening.mc.prelude.api.registration.TileEntityType
import st.evening.mc.prelude.api.registration.openContainer
import st.evening.mc.prelude.api.util.collection.IntRangeSet
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.assertLogicalServer
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.prelude.api.util.world.useTileEntity

class HatchBlock private constructor(val hatchType: HatchType<*>) :
    CbtCustomBlock(hatchType.blockConfig), TileEntityBlock, CustomItemBlock {
    companion object {
        private var ctorTierProp: IProperty<Int>? = null

        fun construct(hatchType: HatchType<*>): HatchBlock {
            // createBlockState gets called *at construction time, in the superconstructor*
            // so we have to do this dumb hack to be able to pass the property to createBlockState
            ctorTierProp = TierProperty(hatchType.tierCount)
            return try {
                HatchBlock(hatchType)
            } finally {
                ctorTierProp = null
            }.also { it.init() }
        }
    }

    val tierProperty: IProperty<Int> = ctorTierProp!!

    override val blockType: CustomBlockType
        get() = hatchType

    override fun createBlockState(): BlockStateContainer = BlockStateContainer(this, ctorTierProp!!)

    override fun createBlockItem(): ItemBlock = HatchBlockItem(this)

    override fun getSubBlocks(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        for (i in 0..<hatchType.tierCount) {
            items += ItemStack(this, 1, i)
        }
    }

    override fun damageDropped(state: IBlockState): Int = getMetaFromState(state)

    override fun getMetaFromState(state: IBlockState): Int = state.getValue(tierProperty)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getStateFromMeta(meta: Int): IBlockState = defaultState.withProperty(tierProperty, meta)

    override fun getTileEntityType(world: World, meta: Int): TileEntityType<*> = CbTweaker.defns.tileEntityHatch

    override fun onBlockActivated(
        world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (CoFHCoreCompat.tryWrenchDismantle(world, pos, state, player, hand)) return true
        world.useTileEntity<HatchTileEntity>(pos) {
            if (it.handleInteraction(state, player, hand, facing, hitX, hitY, hitZ)) return true
            world.onServer {
                player.openContainer(CbTweaker.defns.containerHatch, world, pos)
            }
            return true
        }
        return false
    }

    @OptIn(SidednessAssertion::class)
    @Suppress("OVERRIDE_DEPRECATION")
    override fun neighborChanged(state: IBlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos) {
        assertLogicalServer {
            world.useTileEntity<HatchTileEntity>(pos) {
                it.handleBlockUpdate(state, block, fromPos)
            }
        }
    }

    @OptIn(SidednessAssertion::class)
    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        assertLogicalServer {
            world.useTileEntity<HatchTileEntity>(pos) {
                it.handleDestruction(state)
            }
        }
        super.breakBlock(world, pos, state)
    }

    override fun getTranslationKey(): String = "${CbtConsts.MOD_ID}.hatch.${hatchType.id}"

    private class TierProperty(tierCount: Int) : IProperty<Int> {
        private val tierRange: IntRangeSet = IntRangeSet(0..<tierCount)

        override fun getName(): String = "tier"

        override fun getName(value: Int): String = value.toString()

        override fun getAllowedValues(): Collection<Int> = tierRange

        override fun getValueClass(): Class<Int> = Int::class.javaObjectType

        override fun parseValue(value: String): Optional<Int?> {
            val tier = try {
                value.toInt(10)
            } catch (_: NumberFormatException) {
                return Optional.absent()
            }
            return if (tier in tierRange) Optional.of(tier) else Optional.absent()
        }
    }
}
