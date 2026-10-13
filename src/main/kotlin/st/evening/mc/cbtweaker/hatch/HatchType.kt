package st.evening.mc.cbtweaker.hatch

import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.BufferFactory
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.common.CustomBlockType
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.block

class HatchType(
    reg: ModRegistrar,
    val id: String,
    override val blockConfig: BlockConfig,
    internal val fixedTierCount: Int
) : CustomBlockType {
    val hatchBlock: HatchBlock by reg.block("hatch_$id") { HatchBlock.construct(this, fixedTierCount) }

    lateinit var data: Data<*>
        private set

    internal fun <B> initData(bufferType: BufferType<B, *, *, *>, tierData: List<TierData<B>>) {
        if (tierData.size != fixedTierCount) {
            CbTweaker.logger.warn(
                "Hatch {} initialized with {} tiers, which is not the expected count {}; this may cause issues!",
                id, tierData.size, fixedTierCount
            )
        }
        data = Data(bufferType, tierData)
    }

    fun getHatchBlock(tier: Int): IBlockState {
        val block = hatchBlock
        return block.defaultState.withProperty(block.tierProperty, tier)
    }

    fun getHatchStack(count: Int, tier: Int): ItemStack = ItemStack(hatchBlock, count, tier)

    class Data<B>(
        val bufferType: BufferType<B, *, *, *>,
        private val tierData: List<TierData<B>>
    ) {
        val tierCount: Int
            get() = tierData.size

        fun getTier(tier: Int): TierData<B> = tierData[tier]
    }

    class TierData<B>(val bufferFactory: BufferFactory<B, *>, val windowConfig: WindowConfig)
}
