package xyz.phanta.cbtweaker.util.item;

import io.github.phantamanta44.libnine.util.gameobject.ItemIdentity;
import net.minecraft.item.ItemStack;

public interface ItemStore {

    ItemIdentity getStoredItem();

    boolean setStoredItem(ItemIdentity ident);

    int getCount();

    default boolean isEmpty() {
        return getCount() == 0;
    }

    default ItemStack getContentsAsStack() {
        return getStoredItem().createStack(getCount());
    }

    ItemStack insert(ItemStack stack, boolean simulate);

    ItemStack extract(int amount, boolean simulate);

    ItemStore EMPTY = new ItemStore() {
        @Override
        public ItemIdentity getStoredItem() {
            return ItemIdentity.AIR;
        }

        @Override
        public boolean setStoredItem(ItemIdentity ident) {
            return false;
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extract(int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    };

}
