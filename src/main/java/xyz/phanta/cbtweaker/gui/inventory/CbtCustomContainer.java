package xyz.phanta.cbtweaker.gui.inventory;

import io.github.phantamanta44.libnine.util.math.Vec2i;
import net.minecraft.entity.player.InventoryPlayer;

public abstract class CbtCustomContainer extends CbtContainer {

    private final UiLayout layout;

    public CbtCustomContainer(UiLayout layout, InventoryPlayer playerInv) {
        this.layout = layout;
        Vec2i playerInvPos = layout.getPlayerInventoryPosition();
        addPlayerInventory(playerInvPos.getX() + 1, playerInvPos.getY() + 1, playerInv);
    }

    public UiLayout getLayout() {
        return layout;
    }

    public abstract String getTranslationKey();

}
