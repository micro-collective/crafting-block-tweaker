package xyz.phanta.cbtweaker.structure;

import io.github.phantamanta44.libnine.client.gui.L9Gui;
import io.github.phantamanta44.libnine.util.helper.ItemUtils;
import io.github.phantamanta44.libnine.util.render.GuiUtils;
import io.github.phantamanta44.libnine.util.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.tuple.Triple;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;
import xyz.phanta.cbtweaker.network.CPacketVisualizationLevel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VisualizationGui extends L9Gui {

    private static final int VIS_OFF_X = 5, VIS_OFF_Y = 5;

    @Nullable
    private Triple<VisualizationToolItem.VisualizationInfo, ItemStack, VisualizationRenderer> visualization;

    public VisualizationGui() {
        super(CbtTextureResources.GUI_MB_VIS_BG.getTexture().getTexture(),
                CbtTextureResources.GUI_MB_VIS_BG.getWidth(), CbtTextureResources.GUI_MB_VIS_BG.getHeight());
    }

    @Override
    public void initGui() {
        super.initGui();
        updateScreen();
    }

    @Override
    public void drawBackground(float partialTicks, int mX, int mY) {
        super.drawBackground(partialTicks, mX, mY);
        CbtTextureResources.GUI_MB_VIS.draw(getOffsetX() + VIS_OFF_X, getOffsetY() + VIS_OFF_Y);
    }

    @Override
    public void drawForeground(float partialTicks, int mX, int mY) {
        if (visualization == null) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            drawCenteredString(fontRenderer, I18n.format(CbtLang.GUI_VIS_TOOL_NOT_BOUND),
                    sizeX * 2 / 3, (VIS_OFF_Y + 44) * 4 / 3, 0xFF0000);
            GlStateManager.popMatrix();
            return;
        }

        int visMouseX = mX - VIS_OFF_X, visMouseY = mY - VIS_OFF_Y;
        VisualizationRenderer visRenderer = visualization.getRight();
        visRenderer.handleMouseMovement(visMouseX, visMouseY);
        GlStateManager.enableDepth();
        visRenderer.render(VIS_OFF_X, VIS_OFF_Y, visMouseX, visMouseY);
        RenderUtils.renderItemIntoGui(6, 98, visualization.getMiddle());
        GlStateManager.disableDepth();
        if (isOverControllerSlot(mX, mY)) {
            Gui.drawRect(6, 98, 22, 114, 0x80FFFFFF);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.75F, 0.75F, 0.75F);
        drawString(I18n.format(visualization.getLeft().getMultiBlockType().getTranslationKey()), 35, 138, 0x404040);
        GlStateManager.popMatrix();
    }

    @Override
    public void drawOverlay(float partialTicks, int mX, int mY) {
        if (visualization == null) {
            return;
        }
        List<String> tooltip = new ArrayList<>();
        if (isOverControllerSlot(mX, mY)) {
            ItemUtils.getStackTooltip(visualization.getMiddle(), tooltip, RenderUtils.getTooltipFlags());
        } else {
            visualization.getRight().getTooltip(
                    tooltip, mX - VIS_OFF_X, mY - VIS_OFF_Y, RenderUtils.getTooltipFlags());
        }
        if (!tooltip.isEmpty()) {
            drawTooltip(tooltip, mX, mY);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        EntityPlayer player = mc.player;
        VisualizationToolItem.VisualizationInfo newVisInfo = VisualizationToolItem.getVisualizationInfo(player);
        if (newVisInfo == null) {
            visualization = null;
            return;
        }
        MultiBlockType<?, ?, ?> mbType = newVisInfo.getMultiBlockType();
        if (visualization == null || mbType != visualization.getLeft().getMultiBlockType()) {
            VisualizationRenderer visRenderer = new VisualizationRenderer(mbType.getStructureMatcher());
            visRenderer.setLevel(VisualizationToolItem.getLevel(newVisInfo.getStack()));
            visualization = Triple.of(newVisInfo, new ItemStack(mbType.getControllerBlock()), visRenderer);
        } else {
            visualization = Triple.of(newVisInfo, visualization.getMiddle(), visualization.getRight());
        }
    }

    @Override
    protected void mouseClicked(int mX, int mY, int button) {
        if (visualization == null) {
            return;
        }
        VisualizationRenderer visRenderer = visualization.getRight();
        if (visRenderer.handleClick(mX - getOffsetX() - VIS_OFF_X, mY - getOffsetY() - VIS_OFF_Y, button)) {
            VisualizationToolItem.VisualizationInfo visInfo = visualization.getLeft();
            Integer newLevel = visRenderer.getLevel();
            if (!Objects.equals(VisualizationToolItem.getLevel(visInfo.getStack()), newLevel)) {
                CbtMod.INSTANCE.getNetworkHandler()
                        .sendToServer(new CPacketVisualizationLevel(visInfo.getHand(), newLevel));
            }
        }
    }

    @Override
    protected void keyTyped(char typed, int keyCode) {
        if (keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            mc.displayGuiScreen(null);
        }
    }

    private static boolean isOverControllerSlot(int mX, int mY) {
        return GuiUtils.isMouseOver(6, 98, 16, 16, mX, mY);
    }

}
