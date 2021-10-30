package xyz.phanta.cbtweaker.gui.component;

import io.github.phantamanta44.libnine.util.format.FormatUtils;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyBarGuiComponent extends TexturedBarGuiComponent {

    private final IEnergyStorage energyStorage;
    private final String unitSymbol;

    public EnergyBarGuiComponent(int x, int y, TextureRegion bgTexture, TextureRegion fgTexture,
                                 int fgOffsetX, int fgOffsetY, DrawOrientation orientation,
                                 IEnergyStorage energyStorage, String unitSymbol) {
        super(x, y, bgTexture, fgTexture, fgOffsetX, fgOffsetY, orientation);
        this.energyStorage = energyStorage;
        this.unitSymbol = unitSymbol;
    }

    @Override
    public float getBarProgress() {
        return energyStorage.getEnergyStored() / (float)energyStorage.getMaxEnergyStored();
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        drawTooltip(String.format("%s / %s",
                        FormatUtils.formatSI(energyStorage.getEnergyStored(), unitSymbol),
                        FormatUtils.formatSI(energyStorage.getMaxEnergyStored(), unitSymbol)),
                mX, mY);
    }

}
