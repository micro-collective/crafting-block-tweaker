package xyz.phanta.cbtweaker.structure;

import gnu.trove.map.hash.TIntObjectHashMap;
import io.github.phantamanta44.libnine.util.helper.InputUtils;
import io.github.phantamanta44.libnine.util.math.LinAlUtils;
import io.github.phantamanta44.libnine.util.math.MathUtils;
import io.github.phantamanta44.libnine.util.render.GuiUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.ForgeHooksClient;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.integration.jei.CbtJeiPlugin;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.StructureBlockVisualization;
import xyz.phanta.cbtweaker.util.DummyBlockAccessor;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

public class VisualizationRenderer {

    // opengl needs a direct buffer to read data into, so this is the one buffer that is reused for various things
    private static final FloatBuffer GL_BUF = ByteBuffer.allocateDirect(16 * Float.BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();

    private final JeiVisualizationWorld visWorld;
    @Nullable
    private StructureBlockVisualization hoveredBlock = null;

    private double focusX = 0.5D, focusY = 0.5D, focusZ = 0.5D, focusDist = 3D;
    private float pitch = -22.5F, yaw = 45F;
    @Nullable
    private MouseAction activeMouseAction;

    public VisualizationRenderer(StructureMatcher structMatcher) {
        this.visWorld = new JeiVisualizationWorld(structMatcher);
    }

    @Nullable
    public Integer getLevel() {
        return visWorld.getLevel();
    }

    public void setLevel(@Nullable Integer level) {
        visWorld.setLevel(level);
    }

    public void getTooltip(List<String> tooltip, int mouseX, int mouseY, ITooltipFlag tooltipFlags) {
        if (isIn3dWindow(mouseX, mouseY)) {
            if (hoveredBlock == null) {
                return;
            }
            hoveredBlock.getTooltip(tooltip, tooltipFlags);
        } else if (isOverLayerUp(mouseX, mouseY)) {
            tooltip.add(I18n.format(CbtLang.TOOLTIP_VIS_LAYER_UP));
        } else if (isOverLayerDown(mouseX, mouseY)) {
            tooltip.add(I18n.format(CbtLang.TOOLTIP_VIS_LAYER_DOWN));
        } else if (isOverHelp(mouseX, mouseY)) {
            tooltip.add(TextFormatting.RED + I18n.format(CbtLang.TOOLTIP_VIS_CONTROLS));
            tooltip.add(I18n.format(CbtLang.TOOLTIP_VIS_HORZ_PAN));
            tooltip.add(I18n.format(CbtLang.TOOLTIP_VIS_VERT_PAN));
            tooltip.add(I18n.format(CbtLang.TOOLTIP_VIS_ORBIT));
            tooltip.add(I18n.format(CbtLang.TOOLTIP_VIS_ZOOM));
            tooltip.add(I18n.format(CbtLang.TOOLTIP_VIS_CENTER));
        }
    }

    public boolean handleClick(int mouseX, int mouseY, int button) {
        switch (button) {
            case 0:
                if (isOverLayerUp(mouseX, mouseY)) {
                    visWorld.incrementLevel();
                    Minecraft.getMinecraft().getSoundHandler()
                            .playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F));
                    return true;
                } else if (isOverLayerDown(mouseX, mouseY)) {
                    visWorld.decrementLevel();
                    Minecraft.getMinecraft().getSoundHandler()
                            .playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F));
                    return true;
                }
                break;
            case 2:
                if (isIn3dWindow(mouseX, mouseY)) {
                    focusX = focusY = focusZ = 0.5D;
                    focusDist = 3D;
                    yaw = 45F;
                    pitch = -22.5F;
                    return true;
                }
                break;
        }
        return false;
    }

    public void handleMouseMovement(int mouseX, int mouseY) {
        if (activeMouseAction != null) {
            if (activeMouseAction.process(mouseX, mouseY)) {
                return;
            } else {
                activeMouseAction = null;
            }
        }
        if (!isIn3dWindow(mouseX, mouseY)) {
            return;
        }
        if (Mouse.isButtonDown(0)) {
            if (InputUtils.ModKey.SHIFT.isActive()) {
                activeMouseAction = new VerticalPanMouseAction(mouseY);
            } else {
                activeMouseAction = new HorizontalPanMouseAction(mouseX, mouseY);
            }
        } else if (Mouse.isButtonDown(1)) {
            if (InputUtils.ModKey.SHIFT.isActive()) {
                activeMouseAction = new ZoomMouseAction(mouseY);
            } else {
                activeMouseAction = new OrbitMouseAction(mouseX, mouseY);
            }
        }
    }

    public void render(int x, int y, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
        ScaledResolution res = new ScaledResolution(mc);
        int scaledWidth = res.getScaledWidth(), scaledHeight = res.getScaledHeight();
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, GL_BUF);
        float kWidth = mc.displayWidth / (float)scaledWidth;
        float kHeight = mc.displayHeight / (float)scaledHeight;
        int vpWidth = (int)(160F * kWidth), vpHeight = (int)(89F * kHeight);
        int vpX = (int)((GL_BUF.get(12) + x + 1F) * kWidth);
        int vpY = mc.displayHeight - (int)((GL_BUF.get(13) + y + 1F) * kHeight) - vpHeight;
        GlStateManager.viewport(vpX, vpY, vpWidth, vpHeight);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(60F, scaledWidth / (float)scaledHeight, 0.05F, 32F);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluLookAt(0F, 0F, -1F, 0F, 0F, 0F, 0F, 1F, 0F);
        GlStateManager.translate(0D, 0D, focusDist);
        GlStateManager.rotate(pitch, 1F, 0F, 0F);
        GlStateManager.rotate(yaw, 0F, 1F, 0F);
        GlStateManager.translate(-focusX, -focusY, -focusZ);

        drawStructure(mc);
        drawFocus();
        drawHoveredBlock(vpX, vpY, vpWidth, vpHeight, mouseX, mouseY);

        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popAttrib();
    }

    private void drawStructure(Minecraft mc) {
        List<Pair<IBlockState, BlockPos>> blocksToRender = new ArrayList<>();
        for (Map.Entry<BlockPos, StructureBlockMatcher> matcher : visWorld.getBlocks().entrySet()) {
            StructureBlockVisualization vis = CbtJeiPlugin.getIngredientByGlobalIndex(
                    matcher.getValue().getVisualization());
            if (vis != null) {
                blocksToRender.add(Pair.of(vis.getBlockState(), matcher.getKey()));
            }
        }

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder vertexBuf = tess.getBuffer();
        BlockRendererDispatcher blockRenderer = mc.getBlockRendererDispatcher();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        for (BlockRenderLayer renderLayer : BlockRenderLayer.values()) {
            ForgeHooksClient.setRenderLayer(renderLayer);
            vertexBuf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            for (Pair<IBlockState, BlockPos> entry : blocksToRender) {
                IBlockState state = entry.getLeft();
                if (!state.getBlock().canRenderInLayer(state, renderLayer)) {
                    continue;
                }
                BlockPos pos = entry.getRight();
                GlStateManager.pushMatrix();
                GlStateManager.translate(pos.getX(), pos.getY(), pos.getZ());
                blockRenderer.renderBlock(state, pos, visWorld, vertexBuf);
                GlStateManager.popMatrix();
            }
            tess.draw();
        }
        ForgeHooksClient.setRenderLayer(null);

        // draw an invisible(ish) cube around each block to populate the depth buffer for hover detection later on
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        for (Pair<IBlockState, BlockPos> entry : blocksToRender) {
            GlStateManager.color(0F, 0F, 0F, 1F / 255F);
            drawCube(entry.getRight());
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
        GlStateManager.enableAlpha();
    }

    private void drawFocus() {
        if (activeMouseAction == null) {
            return;
        }

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder vertexBuf = tess.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(2F);
        vertexBuf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        vertexBuf.pos(focusX - 0.25D, focusY, focusZ).color(1F, 0F, 0F, 1F).endVertex();
        vertexBuf.pos(focusX + 0.25D, focusY, focusZ).color(1F, 0F, 0F, 1F).endVertex();
        vertexBuf.pos(focusX, focusY - 0.25D, focusZ).color(0F, 1F, 0F, 1F).endVertex();
        vertexBuf.pos(focusX, focusY + 0.25D, focusZ).color(0F, 1F, 0F, 1F).endVertex();
        vertexBuf.pos(focusX, focusY, focusZ - 0.25D).color(0F, 0F, 1F, 1F).endVertex();
        vertexBuf.pos(focusX, focusY, focusZ + 0.25D).color(0F, 0F, 1F, 1F).endVertex();
        tess.draw();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }

    private void drawHoveredBlock(int vpX, int vpY, int vpWidth, int vpHeight, int mouseX, int mouseY) {
        if (!isIn3dWindow(mouseX, mouseY)) {
            hoveredBlock = null;
            return;
        }

        Matrix4f projMat = new Matrix4f();
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, GL_BUF);
        projMat.load(GL_BUF);
        GL_BUF.rewind();
        Matrix4f mvMat = new Matrix4f();
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, GL_BUF);
        mvMat.load(GL_BUF);
        GL_BUF.rewind();
        Matrix4f screenToWorldMat = Matrix4f.mul(projMat, mvMat, null);
        screenToWorldMat.invert();
        int mX = Mouse.getX(), mY = Mouse.getY();
        GL11.glReadPixels(mX, mY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, GL_BUF);
        float depthVal = 2F * GL_BUF.get(0) - 0.999F; // offset a bit so we're not on the exact edge of the blocks
        float w = projMat.m32 / (depthVal + projMat.m22);
        Vector4f worldMousePos = Matrix4f.transform(screenToWorldMat, new Vector4f(
                w * (2F * (mX - vpX) / vpWidth - 1F),
                w * (2F * (mY - vpY) / vpHeight - 1F),
                w * depthVal,
                w), null);
        BlockPos hoveredPos = new BlockPos(worldMousePos.x, worldMousePos.y, worldMousePos.z);

        StructureBlockMatcher matcher = visWorld.getBlocks().get(hoveredPos);
        if (matcher == null) {
            hoveredBlock = null;
            return;
        }
        hoveredBlock = CbtJeiPlugin.getIngredientByGlobalIndex(matcher.getVisualization());
        if (hoveredBlock == null) {
            return;
        }

        GlStateManager.color(1F, 1F, 1F, 0.5F);
        drawCube(hoveredPos);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    public static void drawCube(Vec3i pos) {
        double minX = pos.getX(), maxX = minX + 1D;
        double minY = pos.getY(), maxY = minY + 1D;
        double minZ = pos.getZ(), maxZ = minZ + 1D;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder vertexBuf = tess.getBuffer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1F, -10F);
        // draw sides
        vertexBuf.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION);
        vertexBuf.pos(minX, minY, minZ).endVertex();
        vertexBuf.pos(minX, maxY, minZ).endVertex();
        vertexBuf.pos(maxX, minY, minZ).endVertex();
        vertexBuf.pos(maxX, maxY, minZ).endVertex();
        vertexBuf.pos(maxX, minY, maxZ).endVertex();
        vertexBuf.pos(maxX, maxY, maxZ).endVertex();
        vertexBuf.pos(minX, minY, maxZ).endVertex();
        vertexBuf.pos(minX, maxY, maxZ).endVertex();
        vertexBuf.pos(minX, minY, minZ).endVertex();
        vertexBuf.pos(minX, maxY, minZ).endVertex();
        tess.draw();
        // draw top
        vertexBuf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        vertexBuf.pos(minX, maxY, minZ).endVertex();
        vertexBuf.pos(minX, maxY, maxZ).endVertex();
        vertexBuf.pos(maxX, maxY, maxZ).endVertex();
        vertexBuf.pos(maxX, maxY, minZ).endVertex();
        tess.draw();
        // draw bottom
        vertexBuf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        vertexBuf.pos(minX, minY, minZ).endVertex();
        vertexBuf.pos(maxX, minY, minZ).endVertex();
        vertexBuf.pos(maxX, minY, maxZ).endVertex();
        vertexBuf.pos(minX, minY, maxZ).endVertex();
        tess.draw();
        GlStateManager.doPolygonOffset(0F, 0F);
        GlStateManager.disablePolygonOffset();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private static boolean isIn3dWindow(int mouseX, int mouseY) {
        return GuiUtils.isMouseOver(1, 1, 160, 89, mouseX, mouseY);
    }

    private static boolean isOverLayerUp(int mouseX, int mouseY) {
        return GuiUtils.isMouseOver(148, 94, 13, 7, mouseX, mouseY);
    }

    private static boolean isOverLayerDown(int mouseX, int mouseY) {
        return GuiUtils.isMouseOver(148, 101, 13, 7, mouseX, mouseY);
    }

    private static boolean isOverHelp(int mouseX, int mouseY) {
        return GuiUtils.isMouseOver(130, 94, 14, 14, mouseX, mouseY);
    }

    private static class JeiVisualizationWorld extends DummyBlockAccessor {

        private final Map<BlockPos, StructureBlockMatcher> blockTable = new HashMap<>();
        private final TIntObjectHashMap<Map<BlockPos, StructureBlockMatcher>> levelSetTable = new TIntObjectHashMap<>();
        private final int minLevel, maxLevel;
        @Nullable
        private Integer level = null;

        public JeiVisualizationWorld(StructureMatcher structMatcher) {
            int minLevel = Integer.MAX_VALUE, maxLevel = Integer.MIN_VALUE;
            for (Pair<Vec3i, StructureBlockMatcher> entry : structMatcher.getVisualization()) {
                BlockPos pos = new BlockPos(entry.getLeft());
                StructureBlockMatcher matcher = entry.getRight();
                blockTable.put(pos, matcher);
                int level = pos.getY();
                Map<BlockPos, StructureBlockMatcher> levelSet = levelSetTable.get(level);
                if (levelSet == null) {
                    levelSet = new HashMap<>();
                    levelSetTable.put(level, levelSet);
                }
                levelSet.put(pos, matcher);
                if (level < minLevel) {
                    minLevel = level;
                }
                if (level > maxLevel) {
                    maxLevel = level;
                }
            }
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        public Map<BlockPos, StructureBlockMatcher> getBlocks() {
            if (level == null) {
                return blockTable;
            }
            Map<BlockPos, StructureBlockMatcher> levelSet = levelSetTable.get(level);
            return levelSet != null ? levelSet : Collections.emptyMap();
        }

        @Nullable
        public Integer getLevel() {
            return level;
        }

        public void setLevel(@Nullable Integer level) {
            if (level == null || level < minLevel || level > maxLevel) {
                this.level = null;
            } else {
                this.level = level;
            }
        }

        public void incrementLevel() {
            if (level == null) {
                level = minLevel;
            } else if (level >= maxLevel) {
                level = null;
            } else {
                ++level;
            }
        }

        public void decrementLevel() {
            if (level == null) {
                level = maxLevel;
            } else if (level <= minLevel) {
                level = null;
            } else {
                --level;
            }
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            StructureBlockMatcher matcher = getBlocks().get(pos);
            if (matcher == null) {
                return Blocks.AIR.getDefaultState();
            }
            StructureBlockVisualization vis = CbtJeiPlugin.getIngredientByGlobalIndex(matcher.getVisualization());
            return vis != null ? vis.getBlockState() : Blocks.AIR.getDefaultState();
        }

    }

    private interface MouseAction {

        boolean process(int mouseX, int mouseY);

    }

    private class HorizontalPanMouseAction implements MouseAction {

        private static final double SCALE = 0.05D;

        private final int initialX, initialY;
        private final double initialFocusX, initialFocusZ;
        private final Vec3d panBasisX, panBasisZ;

        private HorizontalPanMouseAction(int initialX, int initialY) {
            this.initialX = initialX;
            this.initialY = initialY;
            this.initialFocusX = focusX;
            this.initialFocusZ = focusZ;
            this.panBasisX = LinAlUtils.rotate(LinAlUtils.X_POS, LinAlUtils.Y_POS, yaw * MathUtils.D2R_F);
            this.panBasisZ = panBasisX.crossProduct(LinAlUtils.Y_NEG);
        }

        @Override
        public boolean process(int mouseX, int mouseY) {
            if (!Mouse.isButtonDown(0)) {
                return false;
            }
            if (InputUtils.ModKey.SHIFT.isActive()) {
                activeMouseAction = new VerticalPanMouseAction(mouseY);
                return true;
            }
            double dx = SCALE * (mouseX - initialX), dz = SCALE * (initialY - mouseY);
            focusX = initialFocusX + panBasisX.x * dx + panBasisZ.x * dz;
            focusZ = initialFocusZ + panBasisX.z * dx + panBasisZ.z * dz;
            return true;
        }

    }

    private class VerticalPanMouseAction implements MouseAction {

        private static final double SCALE = 0.05D;

        private final int initialY;
        private final double initialFocusY;

        private VerticalPanMouseAction(int initialY) {
            this.initialY = initialY;
            this.initialFocusY = focusY;
        }

        @Override
        public boolean process(int mouseX, int mouseY) {
            if (!Mouse.isButtonDown(0)) {
                return false;
            }
            if (!InputUtils.ModKey.SHIFT.isActive()) {
                activeMouseAction = new HorizontalPanMouseAction(mouseX, mouseY);
                return true;
            }
            focusY = initialFocusY + SCALE * (mouseY - initialY);
            return true;
        }

    }

    private class OrbitMouseAction implements MouseAction {

        private static final float SCALE = 2F;

        private final int initialX, initialY;
        private final float initialYaw, initialPitch;

        private OrbitMouseAction(int initialX, int initialY) {
            this.initialX = initialX;
            this.initialY = initialY;
            this.initialYaw = yaw;
            this.initialPitch = pitch;
        }

        @Override
        public boolean process(int mouseX, int mouseY) {
            if (!Mouse.isButtonDown(1)) {
                return false;
            }
            yaw = initialYaw + SCALE * (mouseX - initialX);
            pitch = MathUtils.clamp(initialPitch + SCALE * (initialY - mouseY), -90F, 90F);
            return true;
        }

    }

    private class ZoomMouseAction implements MouseAction {

        private static final double SCALE = 0.05D;

        private final int initialY;
        private final double initialFocusDist;

        private ZoomMouseAction(int initialY) {
            this.initialY = initialY;
            this.initialFocusDist = focusDist;
        }

        @Override
        public boolean process(int mouseX, int mouseY) {
            if (!Mouse.isButtonDown(1)) {
                return false;
            }
            focusDist = MathUtils.clamp(initialFocusDist - SCALE * (initialY - mouseY), 1D, 10D);
            return true;
        }

    }

}
