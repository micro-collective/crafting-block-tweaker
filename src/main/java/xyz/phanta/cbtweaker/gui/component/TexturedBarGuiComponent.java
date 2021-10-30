package xyz.phanta.cbtweaker.gui.component;

import io.github.phantamanta44.libnine.client.gui.component.GuiComponent;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;

public abstract class TexturedBarGuiComponent extends GuiComponent {

    private final TextureRegion bgTexture, fgTexture;
    private final int fgOffsetX, fgOffsetY;
    private final DrawOrientation orientation;

    public TexturedBarGuiComponent(int x, int y, TextureRegion bgTexture, TextureRegion fgTexture,
                                   int fgOffsetX, int fgOffsetY, DrawOrientation orientation) {
        super(x, y, bgTexture.getWidth(), bgTexture.getHeight());
        this.bgTexture = bgTexture;
        this.fgTexture = fgTexture;
        this.fgOffsetX = fgOffsetX;
        this.fgOffsetY = fgOffsetY;
        this.orientation = orientation;
    }

    public abstract float getBarProgress();

    @Override
    public void render(float partialTicks, int mX, int mY, boolean mouseOver) {
        bgTexture.draw(x, y);
        float progress = getBarProgress();
        if (progress > 0F) {
            orientation.draw(fgTexture, x + fgOffsetX, y + fgOffsetY, Math.min(progress, 1F));
        }
    }

}
