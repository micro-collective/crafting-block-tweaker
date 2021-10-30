package xyz.phanta.cbtweaker.gui.component;

import io.github.phantamanta44.libnine.client.gui.component.GuiComponent;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

public class AutoExportGuiComponent extends GuiComponent {

    private final BooleanSupplier stateGetter;
    private final Runnable callback;

    public AutoExportGuiComponent(int x, int y, BooleanSupplier stateGetter, Runnable callback) {
        super(x, y,
                CbtTextureResources.AUTO_EXPORT_DISABLED.getWidth(),
                CbtTextureResources.AUTO_EXPORT_DISABLED.getHeight());
        this.stateGetter = stateGetter;
        this.callback = callback;
    }

    @Override
    public void render(float partialTicks, int mX, int mY, boolean mouseOver) {
        if (stateGetter.getAsBoolean()) {
            CbtTextureResources.AUTO_EXPORT_ENABLED.draw(x, y);
        } else {
            CbtTextureResources.AUTO_EXPORT_DISABLED.draw(x, y);
        }
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        drawTooltip(Arrays.asList(
                        I18n.format(CbtLang.TOOLTIP_AUTO_EXPORT),
                        TextFormatting.GRAY + I18n.format(stateGetter.getAsBoolean()
                                ? CbtLang.TOOLTIP_ENABLED : CbtLang.TOOLTIP_DISABLED)),
                mX, mY);
    }

    @Override
    public boolean onClick(int mX, int mY, int button, boolean mouseOver) {
        if (!mouseOver) {
            return false;
        }
        callback.run();
        playClickSound();
        return true;
    }

}
