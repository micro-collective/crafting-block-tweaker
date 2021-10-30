package xyz.phanta.cbtweaker.gui.component;

import io.github.phantamanta44.libnine.util.format.FormatUtils;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;

import java.util.function.DoubleSupplier;

public class DoubleEnergyBarGuiComponent extends TexturedBarGuiComponent {

    private final DoubleSupplier quantityGetter;
    private final double capacity;
    private final String unitSymbol;
    private final String capacityStr;

    public DoubleEnergyBarGuiComponent(int x, int y, TextureRegion bgTexture, TextureRegion fgTexture,
                                       int fgOffsetX, int fgOffsetY, DrawOrientation orientation,
                                       DoubleSupplier quantityGetter, double capacity, String unitSymbol) {
        super(x, y, bgTexture, fgTexture, fgOffsetX, fgOffsetY, orientation);
        this.quantityGetter = quantityGetter;
        this.capacity = capacity;
        this.unitSymbol = unitSymbol;
        this.capacityStr = FormatUtils.formatSI(capacity, unitSymbol);
    }

    @Override
    public float getBarProgress() {
        return Double.valueOf(quantityGetter.getAsDouble() / capacity).floatValue();
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        drawTooltip(String.format("%s / %s",
                        FormatUtils.formatSI(quantityGetter.getAsDouble(), unitSymbol),
                        capacityStr),
                mX, mY);
    }

}
