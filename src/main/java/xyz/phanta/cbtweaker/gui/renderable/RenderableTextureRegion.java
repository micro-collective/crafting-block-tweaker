package xyz.phanta.cbtweaker.gui.renderable;

import io.github.phantamanta44.libnine.util.render.TextureRegion;
import io.github.phantamanta44.libnine.util.render.TextureResource;
import net.minecraft.util.ResourceLocation;

public class RenderableTextureRegion implements ScreenRenderable {

    private final int x, y;
    private final TextureRegion texture;
    private final float xStart, yStart, xEnd, yEnd;

    public RenderableTextureRegion(int x, int y, TextureRegion texture,
                                   float xStart, float yStart, float xEnd, float yEnd) {
        this.x = x;
        this.y = y;
        this.texture = texture;
        this.xStart = xStart;
        this.yStart = yStart;
        this.xEnd = xEnd;
        this.yEnd = yEnd;
    }

    public RenderableTextureRegion(int x, int y, TextureRegion texture) {
        this(x, y, texture, 0F, 0F, 1F, 1F);
    }

    public RenderableTextureRegion(int x, int y, ResourceLocation textureLoc, int width, int height) {
        this(x, y, new TextureResource(textureLoc, width, height).asRegion());
    }

    @Override
    public void render(float partialTicks) {
        texture.drawPartial(x, y, xStart, yStart, xEnd, yEnd);
    }

}
