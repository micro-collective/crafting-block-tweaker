package xyz.phanta.cbtweaker.gui.component;

import io.github.phantamanta44.libnine.util.function.IFloatSupplier;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import net.minecraft.client.resources.I18n;
import xyz.phanta.cbtweaker.CbtLang;

public class ProgressBarGuiComponent extends TexturedBarGuiComponent {

    private final IFloatSupplier progressSupplier;

    public ProgressBarGuiComponent(int x, int y, TextureRegion bgTexture, TextureRegion fgTexture,
                                   int fgOffsetX, int fgOffsetY, DrawOrientation orientation,
                                   IFloatSupplier progressSupplier) {
        super(x, y, bgTexture, fgTexture, fgOffsetX, fgOffsetY, orientation);
        this.progressSupplier = progressSupplier;
    }

    @Override
    public float getBarProgress() {
        return progressSupplier.get();
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        float progress = getBarProgress();
        if (progress <= 0F) {
            drawTooltip(I18n.format(CbtLang.TOOLTIP_IDLE), mX, mY);
        } else {
            drawTooltip(String.format("%d%%", (int)Math.ceil(progress * 100F)), mX, mY);
        }
    }

}
