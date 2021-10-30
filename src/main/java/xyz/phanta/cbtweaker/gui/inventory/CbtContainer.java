package xyz.phanta.cbtweaker.gui.inventory;

import io.github.phantamanta44.libnine.gui.L9Container;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import xyz.phanta.cbtweaker.gui.ScreenRegion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CbtContainer extends L9Container {

    private final List<UiElement> uiElements = new ArrayList<>();

    protected void addPlayerInventory(int x, int y, InventoryPlayer playerInv) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                super.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            super.addSlotToContainer(new Slot(playerInv, col, x + col * 18, y + 58));
        }
    }

    public UiElement.Indexed addUiElement(UiElement element, ScreenRegion region) {
        int index = uiElements.size();
        uiElements.add(element);
        element.addToContainer(this::addSlotToContainer, index, region);
        return new UiElement.Indexed(index, element);
    }

    @Nullable
    public UiElement getUiElement(int index) {
        return (index >= 0 && index < uiElements.size()) ? uiElements.get(index) : null;
    }

}
