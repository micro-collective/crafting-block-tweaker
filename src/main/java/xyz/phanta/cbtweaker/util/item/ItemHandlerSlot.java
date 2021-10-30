package xyz.phanta.cbtweaker.util.item;

import io.github.phantamanta44.libnine.util.gameobject.ItemIdentity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public class ItemHandlerSlot implements ItemStore {

    private final IItemHandler backing;
    private final int slotIndex;

    public ItemHandlerSlot(IItemHandler backing, int slotIndex) {
        this.backing = backing;
        this.slotIndex = slotIndex;
    }

    @Override
    public ItemIdentity getStoredItem() {
        return ItemIdentity.getForStack(getContentsAsStack());
    }

    @Override
    public boolean setStoredItem(ItemIdentity ident) {
        if (!(backing instanceof IItemHandlerModifiable)) {
            return false;
        }
        ((IItemHandlerModifiable)backing).setStackInSlot(slotIndex, ident.createStack(getCount()));
        return true;
    }

    @Override
    public int getCount() {
        return getContentsAsStack().getCount();
    }

    @Override
    public boolean isEmpty() {
        return getContentsAsStack().isEmpty();
    }

    @Override
    public ItemStack getContentsAsStack() {
        return backing.getStackInSlot(slotIndex);
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        return backing.insertItem(slotIndex, stack, simulate);
    }

    @Override
    public ItemStack extract(int amount, boolean simulate) {
        return backing.extractItem(slotIndex, amount, simulate);
    }

}
