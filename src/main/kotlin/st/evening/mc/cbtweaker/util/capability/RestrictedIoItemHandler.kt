package st.evening.mc.cbtweaker.util.capability

import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler

class RestrictedIoItemHandler(
    private val delegate: IItemHandler,
    private val allowInsert: Boolean,
    private val allowExtract: Boolean
) : IItemHandler by delegate {
    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack =
        if (allowInsert) delegate.insertItem(slot, stack, simulate) else stack

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack =
        if (allowExtract) delegate.extractItem(slot, amount, simulate) else ItemStack.EMPTY

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean =
        allowInsert && delegate.isItemValid(slot, stack)
}
