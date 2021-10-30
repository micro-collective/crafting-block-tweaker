package xyz.phanta.cbtweaker.gui.renderable;

import io.github.phantamanta44.libnine.util.render.FluidRenderUtils;
import net.minecraftforge.fluids.FluidStack;
import xyz.phanta.cbtweaker.gui.ScreenRegion;

public class RenderableFluid implements ScreenRenderable {

    private final int x, y, width, height;
    private final FluidStack fluid;
    private final int capacity;

    public RenderableFluid(int x, int y, int width, int height, FluidStack fluid, int capacity) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fluid = fluid;
        this.capacity = capacity;
    }

    public RenderableFluid(ScreenRegion region, FluidStack fluid, int capacity) {
        this(region.getX(), region.getY(), region.getWidth(), region.getHeight(), fluid, capacity);
    }

    public RenderableFluid(int x, int y, int width, int height, FluidStack fluid) {
        this(x, y, width, height, fluid, fluid.amount);
    }

    public RenderableFluid(ScreenRegion region, FluidStack fluid) {
        this(region.getX(), region.getY(), region.getWidth(), region.getHeight(), fluid);
    }

    @Override
    public void render(float partialTicks) {
        FluidRenderUtils.renderFluidIntoGuiCleanly(x, y, width, height, fluid, capacity);
    }

}
