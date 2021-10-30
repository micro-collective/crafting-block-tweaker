package xyz.phanta.cbtweaker.gui;

import io.github.phantamanta44.libnine.client.gui.component.GuiComponent;
import xyz.phanta.cbtweaker.gui.renderable.ScreenRenderable;

public interface ComponentAcceptor {

    void addBackgroundRender(ScreenRenderable render);

    void addComponent(GuiComponent component);

}
