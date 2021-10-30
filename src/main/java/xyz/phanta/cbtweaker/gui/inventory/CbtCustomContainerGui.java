package xyz.phanta.cbtweaker.gui.inventory;

import io.github.phantamanta44.libnine.util.render.TextureRegion;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

public class CbtCustomContainerGui<C extends CbtCustomContainer> extends CbtContainerGui<C> {

    private CbtCustomContainerGui(C container, TextureRegion bg) {
        super(container, bg.getTexture().getTexture(), bg.getWidth(), bg.getHeight());
    }

    public CbtCustomContainerGui(C container) {
        this(container, container.getLayout().getBackgroundTexture());
    }

    @Override
    public void drawForeground(float partialTicks, int mX, int mY) {
        C cont = getContainer();
        UiLayout layout = cont.getLayout();
        if (layout.shouldRenderMachineName()) {
            fontRenderer.drawString(I18n.format(cont.getTranslationKey()),
                    layout.getMachineInventoryRegion().getX(),
                    layout.getMachineInventoryRegion().getY() - 10,
                    DEF_TEXT_COL);
        }
        if (layout.shouldRenderPlayerInvName()) {
            fontRenderer.drawString(I18n.format("container.inventory"),
                    layout.getPlayerInventoryPosition().getX(),
                    layout.getPlayerInventoryPosition().getY() - 10,
                    DEF_TEXT_COL);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

}
