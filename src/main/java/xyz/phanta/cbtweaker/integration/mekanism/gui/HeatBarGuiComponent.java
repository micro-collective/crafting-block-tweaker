package xyz.phanta.cbtweaker.integration.mekanism.gui;

import io.github.phantamanta44.libnine.util.format.FormatUtils;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import mekanism.common.util.UnitDisplayUtils;
import xyz.phanta.cbtweaker.gui.component.TexturedBarGuiComponent;

import java.util.function.DoubleSupplier;

public class HeatBarGuiComponent extends TexturedBarGuiComponent {

    private final DoubleSupplier temperatureGetter;
    private final double maxTemp;
    private final String unitSymbol;

    public HeatBarGuiComponent(int x, int y, TextureRegion bgTexture, TextureRegion fgTexture,
                               int fgOffsetX, int fgOffsetY, DrawOrientation orientation,
                               DoubleSupplier temperatureGetter, double maxTemp, String unitSymbol) {
        super(x, y, bgTexture, fgTexture, fgOffsetX, fgOffsetY, orientation);
        this.temperatureGetter = temperatureGetter;
        this.maxTemp = maxTemp;
        this.unitSymbol = unitSymbol;
    }

    @Override
    public float getBarProgress() {
        return (float)Math.min(temperatureGetter.getAsDouble() / maxTemp, 1D);
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        drawTooltip(FormatUtils.formatSI(
                        UnitDisplayUtils.TemperatureUnit.AMBIENT.convertToK(temperatureGetter.getAsDouble(), true),
                        unitSymbol),
                mX, mY);
    }

}
