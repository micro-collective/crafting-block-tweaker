package xyz.phanta.cbtweaker.gui.inventory;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xyz.phanta.cbtweaker.gui.ComponentAcceptor;
import xyz.phanta.cbtweaker.gui.ScreenRegion;

import java.util.function.Consumer;

public interface UiElement {

    default void addToContainer(Consumer<Slot> addSlot, int index, ScreenRegion contRegion) {
        // NO-OP
    }

    @SideOnly(Side.CLIENT)
    ScreenRegion addToGui(ComponentAcceptor gui, int index, ScreenRegion contRegion);

    default void onInteraction(byte[] data, EntityPlayerMP player) {
        // NO-OP
    }

    class Indexed {

        private final int index;
        private final UiElement element;

        public Indexed(int index, UiElement element) {
            this.index = index;
            this.element = element;
        }

        public int getIndex() {
            return index;
        }

        public UiElement getElement() {
            return element;
        }

        public void addToContainer(CbtContainer container, Consumer<Slot> addSlot, ScreenRegion region) {
            element.addToContainer(addSlot, index, region);
        }

        public ScreenRegion addToGui(CbtContainerGui<?> screen, ScreenRegion region) {
            return element.addToGui(screen, index, region);
        }

    }

}
