package xyz.phanta.cbtweaker.integration.mekanism.gui;

import io.github.phantamanta44.libnine.client.gui.component.GuiComponent;
import io.github.phantamanta44.libnine.constant.NameConst;
import io.github.phantamanta44.libnine.util.format.TextFormatUtils;
import io.github.phantamanta44.libnine.util.render.FluidRenderUtils;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.integration.mekanism.SingleGasTank;
import xyz.phanta.cbtweaker.network.CPacketUiElementInteraction;

import java.util.Arrays;

public class InteractiveGasTankGuiComponent extends GuiComponent {

    private final int uiElemIndex;
    private final TextureRegion bgTexture;
    private final int fgOffsetX, fgOffsetY;
    private final SingleGasTank tank;

    public InteractiveGasTankGuiComponent(int uiElemIndex, int x, int y, TextureRegion bgTexture,
                                          int fgOffsetX, int fgOffsetY, SingleGasTank tank) {
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
        GasStack gasStack = tank.getGas();
        if (gasStack != null && gasStack.amount > 0) {
            renderGas(gasStack, tank.getMaxGas(),
                    x + fgOffsetX, y + fgOffsetY, width - fgOffsetX * 2, height - fgOffsetY * 2);
        }
    }

    public static void renderGas(GasStack gasStack, int capacity, int x, int y, int width, int height) {
        Gas gas = gasStack.getGas();
        int tint = gas.getTint();
        TextFormatUtils.setGlColour(tint, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        Tessellator tess = Tessellator.getInstance();
        FluidRenderUtils.renderFluidIntoGui(
                tess, tess.getBuffer(), x, y, width, height, gas.getSprite(), gasStack.amount / (double)capacity);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        GasStack fluid = tank.getGas();
        if (fluid != null && fluid.amount > 0) {
            drawTooltip(Arrays.asList(
                            fluid.getGas().getLocalizedName(),
                            TextFormatting.GRAY + String.format("%,d / %,d mB", fluid.amount, tank.getMaxGas()),
                            "",
                            TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_INSERT),
                            TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_TANK_INTERACT_EXTRACT)),
                    mX, mY);
        } else {
            drawTooltip(Arrays.asList(
                            I18n.format(NameConst.INFO_EMPTY),
                            TextFormatting.GRAY + String.format("0 / %,d mB", tank.getMaxGas()),
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
