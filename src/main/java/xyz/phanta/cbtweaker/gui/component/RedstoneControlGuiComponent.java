package xyz.phanta.cbtweaker.gui.component;

import io.github.phantamanta44.libnine.client.gui.component.GuiComponent;
import io.github.phantamanta44.libnine.util.world.RedstoneBehaviour;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RedstoneControlGuiComponent extends GuiComponent {

    private final Supplier<RedstoneBehaviour> stateGetter;
    private final Consumer<RedstoneBehaviour> callback;

    public RedstoneControlGuiComponent(int x, int y,
                                       Supplier<RedstoneBehaviour> stateGetter, Consumer<RedstoneBehaviour> callback) {
        super(x, y,
                CbtTextureResources.REDSTONE_BEHAVIOUR_IGNORED.getWidth(),
                CbtTextureResources.REDSTONE_BEHAVIOUR_IGNORED.getHeight());
        this.stateGetter = stateGetter;
        this.callback = callback;
    }

    @Override
    public void render(float partialTicks, int mX, int mY, boolean mouseOver) {
        switch (stateGetter.get()) {
            case IGNORED:
                CbtTextureResources.REDSTONE_BEHAVIOUR_IGNORED.draw(x, y);
                break;
            case DIRECT:
                CbtTextureResources.REDSTONE_BEHAVIOUR_DIRECT.draw(x, y);
                break;
            case INVERTED:
                CbtTextureResources.REDSTONE_BEHAVIOUR_INVERTED.draw(x, y);
                break;
        }
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        drawTooltip(Arrays.asList(
                        I18n.format(CbtLang.TOOLTIP_REDSTONE_BEHAVIOUR),
                        TextFormatting.GRAY + I18n.format(stateGetter.get().getTranslationKey())),
                mX, mY);
    }

    @Override
    public boolean onClick(int mX, int mY, int button, boolean mouseOver) {
        if (!mouseOver) {
            return false;
        }
        RedstoneBehaviour newState;
        switch (button) {
            case 0:
                newState = RedstoneBehaviour.VALUES.get(
                        (stateGetter.get().ordinal() + 1) % RedstoneBehaviour.VALUES.size());
                break;
            case 1:
                int stateCount = RedstoneBehaviour.VALUES.size();
                newState = RedstoneBehaviour.VALUES.get(
                        (stateGetter.get().ordinal() + stateCount - 1) % stateCount);
                break;
            default:
                return false;
        }
        callback.accept(newState);
        playClickSound();
        return true;
    }

}
