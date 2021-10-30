package xyz.phanta.cbtweaker.gui.renderable;

import io.github.phantamanta44.libnine.util.render.RenderUtils;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class RenderableItem implements ScreenRenderable {

    private final int x, y;
    private final ItemStack stack;
    @Nullable
    private final String countText;

    public RenderableItem(int x, int y, ItemStack stack, @Nullable String countText) {
        this.x = x;
        this.y = y;
        this.stack = stack;
        this.countText = countText;
    }

    public RenderableItem(int x, int y, ItemStack stack) {
        this(x, y, stack, null);
    }

    @Override
    public void render(float partialTicks) {
        RenderUtils.renderItemIntoGui(null, x, y, stack, countText);
    }

}
