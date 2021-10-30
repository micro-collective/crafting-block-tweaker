package xyz.phanta.cbtweaker.gui.renderable;

import java.util.Arrays;
import java.util.Collection;

public interface ScreenRenderable {

    void render(float partialTicks);

    static ScreenRenderable conjoin(Collection<ScreenRenderable> parts) {
        return partialTicks -> {
            for (ScreenRenderable part : parts) {
                part.render(partialTicks);
            }
        };
    }

    static ScreenRenderable conjoin(ScreenRenderable... parts) {
        return conjoin(Arrays.asList(parts));
    }

}
