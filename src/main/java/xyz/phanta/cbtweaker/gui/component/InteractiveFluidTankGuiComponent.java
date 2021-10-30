package xyz.phanta.cbtweaker.gui.component;

import io.github.phantamanta44.libnine.client.gui.component.GuiComponent;
import io.github.phantamanta44.libnine.constant.NameConst;
import io.github.phantamanta44.libnine.util.render.FluidRenderUtils;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.network.CPacketUiElementInteraction;

import java.util.Arrays;

public class InteractiveFluidTankGuiComponent extends GuiComponent {

    private final int uiElemIndex;
    private final TextureRegion bgTexture;
    private final int fgOffsetX, fgOffsetY;
    private final IFluidTank tank;

    public InteractiveFluidTankGuiComponent(int uiElemIndex, int x, int y, TextureRegion bgTexture,
                                            int fgOffsetX, int fgOffsetY, IFluidTank tank) {
        super(x, y, bgTexture.getWidth(), bgTexture.getHeight());
        this.uiElemIndex = uiElemIndex;
        this.bgTexture = bgTexture;
        this.fgOffsetX = fgOffsetX;
        this.fgOffsetY = fgOffsetY;
        this.tank = tank;
    }

    @Override
    public void render(float partialTicks, int mX, int mY, boolean mouseOver) {
        bgTexture.draw(x, y);
        FluidStack fluid = tank.getFluid();
        if (fluid != null && fluid.amount > 0) {
            FluidRenderUtils.renderFluidIntoGuiCleanly(
                    x + fgOffsetX, y + fgOffsetY, width - fgOffsetX * 2, height - fgOffsetY * 2,
                    fluid, tank.getCapacity());
        }
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        FluidStack fluid = tank.getFluid();
        if (fluid != null && fluid.amount > 0) {
            drawTooltip(Arrays.asList(
                            fluid.getLocalizedName(),
                            TextFormatting.GRAY + String.format("%,d / %,d mB", fluid.amount, tank.getCapacity()),
                            "",
                            TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_INSERT),
                            TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_EXTRACT)),
                    mX, mY);
        } else {
            drawTooltip(Arrays.asList(
                            I18n.format(NameConst.INFO_EMPTY),
                            TextFormatting.GRAY + String.format("0 / %,d mB", tank.getCapacity()),
                            "",
                            TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_INSERT),
                            TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_EXTRACT)),
                    mX, mY);
        }
    }

    @Override
    public boolean onClick(int mX, int mY, int button, boolean mouseOver) {
        if (mouseOver && (button == 0 || button == 1)) {
            CbtMod.INSTANCE.getNetworkHandler().sendToServer(
                    new CPacketUiElementInteraction(uiElemIndex, new byte[] { (byte)button }));
            return true;
        }
        return false;
    }

}
