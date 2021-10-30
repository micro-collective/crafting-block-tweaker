package xyz.phanta.cbtweaker.util.block;

import io.github.phantamanta44.libnine.util.math.Vec2i;
import io.github.phantamanta44.libnine.util.render.GuiUtils;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import io.github.phantamanta44.libnine.util.world.BlockSide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.CbtContainerGui;
import xyz.phanta.cbtweaker.util.Positioning;

import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.ObjIntConsumer;

public class SideConfigurationUiHandler {

    private static final Map<BlockSide, Pair<TextureRegion, TextureRegion>> SIDE_TEXTURES
            = new EnumMap<>(BlockSide.class);

    static {
        SIDE_TEXTURES.put(BlockSide.UP, Pair.of(
                CbtTextureResources.SIDE_CONFIG_UP_OFF, CbtTextureResources.SIDE_CONFIG_UP_ON));
        SIDE_TEXTURES.put(BlockSide.LEFT, Pair.of(
                CbtTextureResources.SIDE_CONFIG_LEFT_OFF, CbtTextureResources.SIDE_CONFIG_LEFT_ON));
        SIDE_TEXTURES.put(BlockSide.FRONT, Pair.of(
                CbtTextureResources.SIDE_CONFIG_FRONT_OFF, CbtTextureResources.SIDE_CONFIG_FRONT_ON));
        SIDE_TEXTURES.put(BlockSide.RIGHT, Pair.of(
                CbtTextureResources.SIDE_CONFIG_RIGHT_OFF, CbtTextureResources.SIDE_CONFIG_RIGHT_ON));
        SIDE_TEXTURES.put(BlockSide.DOWN, Pair.of(
                CbtTextureResources.SIDE_CONFIG_DOWN_OFF, CbtTextureResources.SIDE_CONFIG_DOWN_ON));
        SIDE_TEXTURES.put(BlockSide.BACK, Pair.of(
                CbtTextureResources.SIDE_CONFIG_BACK_OFF, CbtTextureResources.SIDE_CONFIG_BACK_ON));
    }

    private final CbtContainerGui<?> screen;
    private final int buttonX;
    private final int buttonY;
    private final ObjIntConsumer<BlockSide> sideToggleCb;
    private final IntConsumer exportToggleCb;
    private final List<ConfigComponent> configComps = new ArrayList<>();

    private boolean inConfig = false;

    public SideConfigurationUiHandler(CbtContainerGui<?> screen, int buttonX, int buttonY,
                                      ObjIntConsumer<BlockSide> sideToggleCb, IntConsumer exportToggleCb) {
        this.screen = screen;
        this.buttonX = buttonX;
        this.buttonY = buttonY;
        this.sideToggleCb = sideToggleCb;
        this.exportToggleCb = exportToggleCb;
    }

    public void addConfigRegion(int index, ConfigRegion configRegion) {
        configComps.add(new ConfigComponent(index, configRegion));
    }

    public boolean isInConfig() {
        return inConfig;
    }

    public void render() {
        CbtTextureResources.SIDE_CONFIG_ICON.draw(buttonX, buttonY);
    }

    public void renderOverlay() {
        if (!inConfig) {
            return;
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0F, 0F, 800F);
        for (ConfigComponent configComp : configComps) {
            configComp.renderBackground();
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
        for (ConfigComponent configComp : configComps) {
            configComp.renderForeground();
        }
        GlStateManager.popMatrix();
        RenderHelper.enableGUIStandardItemLighting();
    }

    public void renderTooltip(int mX, int mY) {
        if (GuiUtils.isMouseOver(buttonX, buttonY, 11, 11, mX, mY)) {
            screen.drawHoveringText(I18n.format(CbtLang.TOOLTIP_CONFIGURE_SIDES), mX, mY);
            return;
        }
        if (!inConfig) {
            return;
        }
        for (ConfigComponent configComp : configComps) {
            if (configComp.renderTooltip(mX, mY)) {
                break;
            }
        }
    }

    public boolean handleClick(int mX, int mY) {
        if (GuiUtils.isMouseOver(buttonX, buttonY, 11, 11, mX, mY)) {
            inConfig = !inConfig;
            Minecraft.getMinecraft().getSoundHandler()
                    .playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F));
            return true;
        }
        if (!inConfig) {
            return false;
        }
        for (ConfigComponent configComp : configComps) {
            if (configComp.handleClick(mX, mY)) {
                break;
            }
        }
        return true;
    }

    public static class ConfigRegion {

        private final MachineSideHandler.SideConfig<?> sideConfig;
        private final ScreenRegion region;

        public ConfigRegion(MachineSideHandler.SideConfig<?> sideConfig, ScreenRegion region) {
            this.sideConfig = sideConfig;
            this.region = region;
        }

        public MachineSideHandler.SideConfig<?> getSideConfig() {
            return sideConfig;
        }

        public ScreenRegion getRegion() {
            return region;
        }

    }

    private class ConfigComponent {

        private final int index;
        private final MachineSideHandler.SideConfig<?> sideConfig;
        private final ScreenRegion screenRegion;
        private final int x, y;

        public ConfigComponent(int index, ConfigRegion region) {
            this.index = index;
            this.sideConfig = region.getSideConfig();
            this.screenRegion = region.getRegion();
            Vec2i pos = Positioning.FromCenter.CENTER.computePosition(18, 18, screenRegion);
            this.x = pos.getX();
            this.y = pos.getY();
        }

        public void renderBackground() {
            int rX = screenRegion.getX(), rY = screenRegion.getY();
            Gui.drawRect(rX, rY, rX + screenRegion.getWidth(), rY + screenRegion.getHeight(), 0x7F000000);
        }

        public void renderForeground() {
            for (BlockSide side : BlockSide.VALUES) {
                Pair<TextureRegion, TextureRegion> sideTextures = SIDE_TEXTURES.get(side);
                TextureRegion disabledTexture = sideTextures.getLeft();
                if (sideConfig.isEnabled(side)) {
                    sideTextures.getRight().draw(x + disabledTexture.getX(), y + disabledTexture.getY());
                } else {
                    disabledTexture.draw(x + disabledTexture.getX(), y + disabledTexture.getY());
                }
            }
            AutoExportHandler exportHandler = sideConfig.getExportHandler();
            if (exportHandler.isAutoExportSupported()) {
                if (exportHandler.isAutoExporting()) {
                    CbtTextureResources.SIDE_CONFIG_EXPORT_ON.draw(x, y);
                } else {
                    CbtTextureResources.SIDE_CONFIG_EXPORT_OFF.draw(x, y);
                }
            }
        }

        public boolean renderTooltip(int mX, int mY) {
            if (GuiUtils.isMouseOver(x, y, 18, 18, mX, mY)) {
                for (BlockSide side : BlockSide.VALUES) {
                    Pair<TextureRegion, TextureRegion> sideTextures = SIDE_TEXTURES.get(side);
                    TextureRegion disabledTexture = sideTextures.getLeft();
                    if (GuiUtils.isMouseOver(x + disabledTexture.getX(), y + disabledTexture.getY(),
                            disabledTexture.getWidth(), disabledTexture.getHeight(), mX, mY)) {
                        drawToggleStateTooltip(mX, mY, side.getTranslationKey(), sideConfig.isEnabled(side));
                        GlStateManager.color(1F, 1F, 1F, 1F);
                        return true;
                    }
                }
                AutoExportHandler exportHandler = sideConfig.getExportHandler();
                if (exportHandler.isAutoExportSupported() && GuiUtils.isMouseOver(x, y,
                        CbtTextureResources.AUTO_EXPORT_DISABLED.getWidth(),
                        CbtTextureResources.AUTO_EXPORT_DISABLED.getHeight(),
                        mX, mY)) {
                    drawToggleStateTooltip(mX, mY, CbtLang.TOOLTIP_AUTO_EXPORT, exportHandler.isAutoExporting());
                    return true;
                }
            }
            return false;
        }

        private void drawToggleStateTooltip(int mX, int mY, String nameKey, boolean enabled) {
            screen.drawHoveringText(Arrays.asList(
                            I18n.format(nameKey),
                            TextFormatting.GRAY + I18n.format(
                                    enabled ? CbtLang.TOOLTIP_ENABLED : CbtLang.TOOLTIP_DISABLED)),
                    mX, mY);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }

        public boolean handleClick(int mX, int mY) {
            if (GuiUtils.isMouseOver(x, y, 18, 18, mX, mY)) {
                for (BlockSide side : BlockSide.VALUES) {
                    Pair<TextureRegion, TextureRegion> sideTextures = SIDE_TEXTURES.get(side);
                    TextureRegion disabledTexture = sideTextures.getLeft();
                    if (GuiUtils.isMouseOver(x + disabledTexture.getX(), y + disabledTexture.getY(),
                            disabledTexture.getWidth(), disabledTexture.getHeight(), mX, mY)) {
                        sideToggleCb.accept(side, index);
                        Minecraft.getMinecraft().getSoundHandler()
                                .playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 2F));
                        return true;
                    }
                }
                AutoExportHandler exportHandler = sideConfig.getExportHandler();
                if (exportHandler.isAutoExportSupported() && GuiUtils.isMouseOver(x, y,
                        CbtTextureResources.AUTO_EXPORT_DISABLED.getWidth(),
                        CbtTextureResources.AUTO_EXPORT_DISABLED.getHeight(),
                        mX, mY)) {
                    exportToggleCb.accept(index);
                    Minecraft.getMinecraft().getSoundHandler()
                            .playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.75F));
                    return true;
                }
            }
            return false;
        }

    }

}
