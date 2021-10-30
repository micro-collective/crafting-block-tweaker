package xyz.phanta.cbtweaker.gui.renderable;

import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;

public class RenderableProgressBar implements ScreenRenderable {

    private final int x, y;
    private final TextureRegion texture;
    private final DrawOrientation orientation;
    private final float fraction;

    public RenderableProgressBar(int x, int y, TextureRegion texture, DrawOrientation orientation, float fraction) {
        this.x = x;
        this.y = y;
        this.texture = texture;
        this.orientation = orientation;
        this.fraction = fraction;
    }

    @Override
    public void render(float partialTicks) {
        orientation.draw(texture, x, y, fraction);
    }

}
