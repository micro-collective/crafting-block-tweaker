package xyz.phanta.cbtweaker.integration.jei.render;

import io.github.phantamanta44.libnine.util.render.TextureRegion;
import mezz.jei.api.gui.IDrawable;
import net.minecraft.client.Minecraft;

public class TextureRegionDrawable implements IDrawable {

    private final TextureRegion texture;

    public TextureRegionDrawable(TextureRegion texture) {
        this.texture = texture;
    }

    @Override
    public int getWidth() {
        return texture.getWidth();
    }

    @Override
    public int getHeight() {
        return texture.getHeight();
    }

    @Override
    public void draw(Minecraft minecraft, int xOffset, int yOffset) {
        texture.draw(xOffset, yOffset);
    }

}
