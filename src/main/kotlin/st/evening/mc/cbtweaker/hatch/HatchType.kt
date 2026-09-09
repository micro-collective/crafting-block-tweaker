package st.evening.mc.cbtweaker.hatch

import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import st.evening.mc.cbtweaker.buffer.BufferFactory
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.common.CustomBlockType
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.block

class HatchType<B>(
    reg: ModRegistrar,
    val id: String,
    override val blockConfig: BlockConfig,
    val bufferType: BufferType<B, *, *, *>,
    private val tierData: List<TierData<B>>
) : CustomBlockType {
    val hatchBlock: HatchBlock by reg.block("hatch_$id") { HatchBlock.construct(this) }

    val tierCount: Int
        get() = tierData.size

    fun getTier(tier: Int): TierData<B> = tierData[tier]

    fun getHatchBlock(tier: Int): IBlockState {
        val block = hatchBlock
        return block.defaultState.withProperty(block.tierProperty, tier)
    }

    fun getHatchStack(count: Int, tier: Int): ItemStack = ItemStack(hatchBlock, count, tier)

    class TierData<B>(val bufferFactory: BufferFactory<B, *>, val windowConfig: WindowConfig)
}
