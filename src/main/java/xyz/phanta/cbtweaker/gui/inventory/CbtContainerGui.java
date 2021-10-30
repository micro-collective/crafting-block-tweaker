package xyz.phanta.cbtweaker.gui.inventory;

import io.github.phantamanta44.libnine.client.gui.L9GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.gui.ComponentAcceptor;
import xyz.phanta.cbtweaker.gui.renderable.ScreenRenderable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CbtContainerGui<C extends CbtContainer> extends L9GuiContainer implements ComponentAcceptor {

    private final C container;
    private final List<ScreenRenderable> bgRenders = new ArrayList<>();

    public CbtContainerGui(C container, @Nullable ResourceLocation bg, int sizeX, int sizeY) {
        super(container, bg, sizeX, sizeY);
        this.container = container;
    }

    public C getContainer() {
        return container;
    }

    @Override
    public void addBackgroundRender(ScreenRenderable render) {
        bgRenders.add(render);
    }

    @Override
    public void drawBackground(float partialTicks, int mX, int mY) {
        super.drawBackground(partialTicks, mX, mY);
        GlStateManager.pushMatrix();
        GlStateManager.translate(guiLeft, guiTop, 0F);
        for (ScreenRenderable render : bgRenders) {
            render.render(partialTicks);
        }
        GlStateManager.popMatrix();
    }

}
