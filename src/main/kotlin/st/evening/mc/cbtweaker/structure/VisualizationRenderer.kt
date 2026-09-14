package st.evening.mc.cbtweaker.structure

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.init.Blocks
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.client.ForgeHooksClient
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import org.lwjgl.util.glu.Project
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector4f
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.StructureBlockVisualization
import st.evening.mc.cbtweaker.util.CbtClientHelper
import st.evening.mc.cbtweaker.util.getRotationFromNorth
import st.evening.mc.cbtweaker.util.rotate
import st.evening.mc.cbtweaker.util.world.DummyBlockAccessor
import st.evening.mc.prelude.api.util.collection.getOrPut
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.playUiClick
import st.evening.mc.prelude.api.util.math.MathsHelper
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.TextureResource
import st.evening.mc.prelude.api.util.render.tessellate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

@ClientSide.Physical
class VisualizationRenderer(structMatcher: StructureMatcher) {
    private val visWorld: JeiVisualizationWorld = JeiVisualizationWorld(structMatcher)
    private var hoveredBlock: StructureBlockVisualization? = null

    private var focusX: Double = DEFAULT_FOCUS_X
    private var focusY: Double = DEFAULT_FOCUS_Y
    private var focusZ: Double = DEFAULT_FOCUS_Z
    private var focusDist: Double = DEFAULT_FOCUS_DIST
    private var pitch: Float = DEFAULT_PITCH
    private var yaw: Float = DEFAULT_YAW
    private var activeMouseAction: MouseAction? = null

    var level: Int?
        get() = visWorld.level
        set(level) {
            visWorld.updateLevel(level)
        }

    fun getTooltip(tooltip: MutableList<String>, mouseX: Int, mouseY: Int, tooltipFlags: ITooltipFlag) {
        if (isIn3dWindow(mouseX, mouseY)) {
            hoveredBlock?.getTooltip(tooltip, tooltipFlags)
        } else {
            getUiTooltip(tooltip, mouseX, mouseY)
        }
    }

    fun handleClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        when (button) {
            0 -> {
                if (isOverLayerUp(mouseX, mouseY)) {
                    visWorld.incrementLevel()
                    Minecraft.getMinecraft().soundHandler.playUiClick()
                    return true
                } else if (isOverLayerDown(mouseX, mouseY)) {
                    visWorld.decrementLevel()
                    Minecraft.getMinecraft().soundHandler.playUiClick()
                    return true
                }
            }
            2 -> {
                if (isIn3dWindow(mouseX, mouseY)) {
                    focusX = DEFAULT_FOCUS_X
                    focusY = DEFAULT_FOCUS_Y
                    focusZ = DEFAULT_FOCUS_Z
                    focusDist = DEFAULT_FOCUS_DIST
                    pitch = DEFAULT_PITCH
                    yaw = DEFAULT_YAW
                    return true
                }
            }
        }
        return false
    }

    fun handleMouseMovement(mouseX: Int, mouseY: Int) {
        activeMouseAction?.let {
            if (it.process(mouseX, mouseY)) return
            activeMouseAction = null
        }
        if (!isIn3dWindow(mouseX, mouseY)) return
        if (Mouse.isButtonDown(0)) {
            activeMouseAction = if (GuiScreen.isShiftKeyDown()) {
                VerticalPanMouseAction(mouseY)
            } else {
                HorizontalPanMouseAction(mouseX, mouseY)
            }
        } else if (Mouse.isButtonDown(1)) {
            activeMouseAction = if (GuiScreen.isShiftKeyDown()) {
                ZoomMouseAction(mouseY)
            } else {
                OrbitMouseAction(mouseX, mouseY)
            }
        }
    }

    fun render(x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val mc = Minecraft.getMinecraft()
        GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT)
        val res = ScaledResolution(mc)
        val scaledWidth = res.scaledWidth
        val scaledHeight = res.scaledHeight
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, GL_BUF)
        val kWidth = mc.displayWidth / scaledWidth.toFloat()
        val kHeight = mc.displayHeight / scaledHeight.toFloat()
        val vpWidth = (160F * kWidth).toInt()
        val vpHeight = (89F * kHeight).toInt()
        val vpX = ((GL_BUF[12] + x + 1F) * kWidth).toInt()
        val vpY = mc.displayHeight - ((GL_BUF[13] + y + 1F) * kHeight).toInt() - vpHeight
        GlStateManager.viewport(vpX, vpY, vpWidth, vpHeight)
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT)

        GlStateManager.matrixMode(GL11.GL_PROJECTION)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()
        Project.gluPerspective(60F, scaledWidth / scaledHeight.toFloat(), 0.05F, 32F)

        GlStateManager.matrixMode(GL11.GL_MODELVIEW)
        GlStateManager.pushMatrix()
        GlStateManager.loadIdentity()
        Project.gluLookAt(0F, 0F, -1F, 0F, 0F, 0F, 0F, 1F, 0F)

        GlStateManager.translate(0.0, 0.0, focusDist)
        GlStateManager.rotate(pitch, 1F, 0F, 0F)
        GlStateManager.rotate(yaw, 0F, 1F, 0F)
        GlStateManager.translate(-focusX, -focusY, -focusZ)

        drawStructure(mc)
        drawFocus()
        drawHoveredBlock(vpX, vpY, vpWidth, vpHeight, mouseX, mouseY)

        GlStateManager.popMatrix()
        GlStateManager.matrixMode(GL11.GL_PROJECTION)
        GlStateManager.popMatrix()
        GlStateManager.matrixMode(GL11.GL_MODELVIEW)
        GlStateManager.popAttrib()
    }

    private fun drawStructure(mc: Minecraft) {
        val blocksToRender = mutableListOf<Pair<BlockPos, IBlockState>>()
        visWorld.blocks.forEach { (pos, matcher) ->
            CbtClientHelper.indexByGlobalTimer(matcher.visualization)?.let {
                blocksToRender += pos to it.blockState
            }
        }
        if (blocksToRender.isEmpty()) return

        val tess = Tessellator.getInstance()
        val blockRenderer = mc.blockRendererDispatcher
        TextureResource.ITEM_BLOCK_ATLAS.bind()
        BlockRenderLayer.entries.forEach { renderLayer ->
            ForgeHooksClient.setRenderLayer(renderLayer)
            tess.tessellate(GL11.GL_QUADS, DefaultVertexFormats.BLOCK) {
                blocksToRender.forEach { (pos, state) ->
                    if (!state.getBlock().canRenderInLayer(state, renderLayer)) return@forEach
                    RenderingHelper.pushMatrix {
                        GlStateManager.translate(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                        blockRenderer.renderBlock(state, pos, visWorld, this)
                    }
                }
            }
        }
        ForgeHooksClient.setRenderLayer(null)

        // draw an invisible cube around each block to populate the depth buffer for hover detection later on
        GlStateManager.disableAlpha()
        GlStateManager.colorMask(false, false, false, false)
        blocksToRender.forEach { (pos, _) -> // TODO should probably do this all in one draw call
            drawCube(pos)
        }
        GlStateManager.colorMask(true, true, true, true)
        GlStateManager.enableAlpha()
    }

    private fun drawFocus() {
        if (activeMouseAction == null) return
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE)
        GlStateManager.disableTexture2D()
        GlStateManager.disableDepth()
        GlStateManager.glLineWidth(2F)
        Tessellator.getInstance().tessellate(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR) {
            pos(focusX - 0.25, focusY, focusZ).color(1F, 0F, 0F, 1F).endVertex()
            pos(focusX + 0.25, focusY, focusZ).color(1F, 0F, 0F, 1F).endVertex()
            pos(focusX, focusY - 0.25, focusZ).color(0F, 1F, 0F, 1F).endVertex()
            pos(focusX, focusY + 0.25, focusZ).color(0F, 1F, 0F, 1F).endVertex()
            pos(focusX, focusY, focusZ - 0.25).color(0F, 0F, 1F, 1F).endVertex()
            pos(focusX, focusY, focusZ + 0.25).color(0F, 0F, 1F, 1F).endVertex()
        }
        GlStateManager.enableDepth()
        GlStateManager.enableTexture2D()
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableBlend()
    }

    private fun drawHoveredBlock(vpX: Int, vpY: Int, vpWidth: Int, vpHeight: Int, mouseX: Int, mouseY: Int) {
        if (!isIn3dWindow(mouseX, mouseY)) {
            hoveredBlock = null
            return
        }

        val projMat = Matrix4f()
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, GL_BUF)
        projMat.load(GL_BUF)
        GL_BUF.rewind()
        val screenToWorldMat = Matrix4f()
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, GL_BUF)
        screenToWorldMat.load(GL_BUF)
        GL_BUF.rewind()
        Matrix4f.mul(projMat, screenToWorldMat, screenToWorldMat)
        screenToWorldMat.invert()

        val screenMouseX = Mouse.getX()
        val screenMouseY = Mouse.getY()
        GL11.glReadPixels(screenMouseX, screenMouseY, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, GL_BUF)
        val depthVal = 2F * GL_BUF.get(0) - 0.999F // offset a bit so we're not on the exact edge of the blocks
        val w = projMat.m32 / (depthVal + projMat.m22)
        val worldMousePos = Vector4f(
            w * (2F * (screenMouseX - vpX) / vpWidth - 1F),
            w * (2F * (screenMouseY - vpY) / vpHeight - 1F),
            w * depthVal,
            w
        )
        Matrix4f.transform(screenToWorldMat, worldMousePos, worldMousePos)
        val hoveredPos = BlockPos(
            MathHelper.floor(worldMousePos.x),
            MathHelper.floor(worldMousePos.y),
            MathHelper.floor(worldMousePos.z)
        )

        hoveredBlock = visWorld.blocks[hoveredPos]?.let { CbtClientHelper.indexByGlobalTimer(it.visualization) }
            ?: return
        GlStateManager.color(1F, 1F, 1F, 0.5F)
        drawCube(hoveredPos)
        RenderingHelper.resetColour()
    }

    private class JeiVisualizationWorld(structMatcher: StructureMatcher) : DummyBlockAccessor() {
        private val blockTable: MutableMap<BlockPos, StructureBlockMatcher> = mutableMapOf()
        private val levelSetTable: Int2ObjectMap<MutableMap<BlockPos, StructureBlockMatcher>> = Int2ObjectOpenHashMap()

        private val minLevel: Int
        private val maxLevel: Int
        var level: Int? = null
            private set

        init {
            var minLevel = Int.MAX_VALUE
            var maxLevel = Int.MIN_VALUE
            structMatcher.visualization.forEach { (pos, matcher) ->
                val blockPos = BlockPos(pos)
                blockTable[blockPos] = matcher
                val level = pos.y
                levelSetTable.getOrPut(level) { mutableMapOf() }[blockPos] = matcher
                if (level < minLevel) {
                    minLevel = level
                }
                if (level > maxLevel) {
                    maxLevel = level
                }
            }
            this.minLevel = minLevel
            this.maxLevel = maxLevel
        }

        val blocks: Map<BlockPos, StructureBlockMatcher>
            get() = level?.let { levelSetTable[it] ?: emptyMap() } ?: blockTable

        fun updateLevel(level: Int?) {
            this.level = if (level == null || level < minLevel || level > maxLevel) null else level
        }

        fun incrementLevel() {
            val currentLevel = level
            level = when {
                currentLevel == null -> minLevel
                currentLevel >= maxLevel -> null
                else -> currentLevel + 1
            }
        }

        fun decrementLevel() {
            val currentLevel = level
            level = when {
                currentLevel == null -> maxLevel
                currentLevel <= minLevel -> null
                else -> currentLevel - 1
            }
        }

        override fun getBlockState(pos: BlockPos): IBlockState =
            blocks[pos]?.let { CbtClientHelper.indexByGlobalTimer(it.visualization) }?.blockState
                ?: Blocks.AIR.defaultState
    }

    private interface MouseAction {
        fun process(mouseX: Int, mouseY: Int): Boolean
    }

    private inner class HorizontalPanMouseAction(private val initialX: Int, private val initialY: Int) : MouseAction {
        private val initialFocusX: Double = focusX
        private val initialFocusZ: Double = focusZ
        private val panBasisX: Vec3d
        private val panBasisZ: Vec3d

        init {
            val yawR = yaw * MathsHelper.D2R_F
            this.panBasisX = Vec3d(MathHelper.cos(yawR).toDouble(), 0.0, MathHelper.sin(yawR).toDouble())
            this.panBasisZ = Vec3d(panBasisX.z, 0.0, -panBasisX.x)
        }

        override fun process(mouseX: Int, mouseY: Int): Boolean {
            if (!Mouse.isButtonDown(0)) return false
            if (GuiScreen.isShiftKeyDown()) {
                activeMouseAction = VerticalPanMouseAction(mouseY)
                return true
            }
            val dx = PAN_H_SCALE * (mouseX - initialX)
            val dz = PAN_H_SCALE * (initialY - mouseY)
            focusX = initialFocusX + panBasisX.x * dx + panBasisZ.x * dz
            focusZ = initialFocusZ + panBasisX.z * dx + panBasisZ.z * dz
            return true
        }
    }

    private inner class VerticalPanMouseAction(private val initialY: Int) : MouseAction {
        private val initialFocusY: Double = focusY

        override fun process(mouseX: Int, mouseY: Int): Boolean {
            if (!Mouse.isButtonDown(0)) return false
            if (!GuiScreen.isShiftKeyDown()) {
                activeMouseAction = HorizontalPanMouseAction(mouseX, mouseY)
                return true
            }
            focusY = initialFocusY + PAN_V_SCALE * (mouseY - initialY)
            return true
        }
    }

    private inner class OrbitMouseAction(private val initialX: Int, private val initialY: Int) : MouseAction {
        private val initialYaw: Float = yaw
        private val initialPitch: Float = pitch

        override fun process(mouseX: Int, mouseY: Int): Boolean {
            if (!Mouse.isButtonDown(1)) return false
            yaw = initialYaw + ORBIT_SCALE * (mouseX - initialX)
            pitch = (initialPitch + ORBIT_SCALE * (initialY - mouseY)).coerceIn(-90F, 90F)
            return true
        }
    }

    private inner class ZoomMouseAction(private val initialY: Int) : MouseAction {
        private val initialFocusDist: Double = focusDist

        override fun process(mouseX: Int, mouseY: Int): Boolean {
            if (!Mouse.isButtonDown(1)) return false
            focusDist = (initialFocusDist - ZOOM_SCALE * (initialY - mouseY)).coerceIn(1.0, 10.0)
            return true
        }
    }

    companion object {
        private const val DEFAULT_FOCUS_X: Double = 0.5
        private const val DEFAULT_FOCUS_Y: Double = 0.5
        private const val DEFAULT_FOCUS_Z: Double = 0.5
        private const val DEFAULT_FOCUS_DIST: Double = 3.0
        private const val DEFAULT_PITCH: Float = -22.5F
        private const val DEFAULT_YAW: Float = 45F

        private const val PAN_H_SCALE: Double = 0.05
        private const val PAN_V_SCALE: Double = 0.05
        private const val ORBIT_SCALE: Float = 2F
        private const val ZOOM_SCALE: Double = 0.05

        // opengl needs a direct buffer to read data into, so this is the one buffer that is reused for various things
        private val GL_BUF: FloatBuffer =
            ByteBuffer.allocateDirect(16 * Float.SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()

        private fun isIn3dWindow(mouseX: Int, mouseY: Int): Boolean =
            mouseX >= 1 && mouseY >= 1 && mouseX < 161 && mouseY < 90

        private fun isOverLayerUp(mouseX: Int, mouseY: Int): Boolean =
            mouseX >= 148 && mouseY >= 94 && mouseX < 161 && mouseY < 101

        private fun isOverLayerDown(mouseX: Int, mouseY: Int): Boolean =
            mouseX >= 148 && mouseY >= 101 && mouseX < 161 && mouseY < 108

        private fun isOverHelp(mouseX: Int, mouseY: Int): Boolean =
            mouseX >= 130 && mouseY >= 94 && mouseX < 144 && mouseY < 108

        fun getUiTooltip(tooltip: MutableList<String>, mouseX: Int, mouseY: Int) {
            if (isOverLayerUp(mouseX, mouseY)) {
                tooltip += I18n.format(CbtLang.TOOLTIP_VIS_LAYER_UP)
            } else if (isOverLayerDown(mouseX, mouseY)) {
                tooltip += I18n.format(CbtLang.TOOLTIP_VIS_LAYER_DOWN)
            } else if (isOverHelp(mouseX, mouseY)) {
                tooltip += "${TextFormatting.RED}${I18n.format(CbtLang.TOOLTIP_VIS_CONTROLS)}"
                tooltip += I18n.format(CbtLang.TOOLTIP_VIS_HORZ_PAN)
                tooltip += I18n.format(CbtLang.TOOLTIP_VIS_VERT_PAN)
                tooltip += I18n.format(CbtLang.TOOLTIP_VIS_ORBIT)
                tooltip += I18n.format(CbtLang.TOOLTIP_VIS_ZOOM)
                tooltip += I18n.format(CbtLang.TOOLTIP_VIS_CENTER)
            }
        }

        private fun drawCube(pos: Vec3i) {
            val minX = pos.x.toDouble()
            val maxX = minX + 1.0
            val minY = pos.y.toDouble()
            val maxY = minY + 1.0
            val minZ = pos.z.toDouble()
            val maxZ = minZ + 1.0
            val tess = Tessellator.getInstance()
            GlStateManager.disableTexture2D()
            GlStateManager.enableBlend()
            GlStateManager.enablePolygonOffset()
            GlStateManager.doPolygonOffset(-1F, -10F)

            // draw sides
            tess.tessellate(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION) {
                pos(minX, minY, minZ).endVertex()
                pos(minX, maxY, minZ).endVertex()
                pos(maxX, minY, minZ).endVertex()
                pos(maxX, maxY, minZ).endVertex()
                pos(maxX, minY, maxZ).endVertex()
                pos(maxX, maxY, maxZ).endVertex()
                pos(minX, minY, maxZ).endVertex()
                pos(minX, maxY, maxZ).endVertex()
                pos(minX, minY, minZ).endVertex()
                pos(minX, maxY, minZ).endVertex()
            }

            // draw top
            tess.tessellate(GL11.GL_QUADS, DefaultVertexFormats.POSITION) {
                pos(minX, maxY, minZ).endVertex()
                pos(minX, maxY, maxZ).endVertex()
                pos(maxX, maxY, maxZ).endVertex()
                pos(maxX, maxY, minZ).endVertex()
            }

            // draw bottom
            tess.tessellate(GL11.GL_QUADS, DefaultVertexFormats.POSITION) {
                pos(minX, minY, minZ).endVertex()
                pos(maxX, minY, minZ).endVertex()
                pos(maxX, minY, maxZ).endVertex()
                pos(minX, minY, maxZ).endVertex()
            }

            GlStateManager.doPolygonOffset(0F, 0F)
            GlStateManager.disablePolygonOffset()
            GlStateManager.disableBlend()
            GlStateManager.enableTexture2D()
        }

        fun renderInWorldVisualization(partialTicks: Float) {
            val mc = Minecraft.getMinecraft()
            val player = mc.player
            val state = ClientVisualizationState.getState() ?: return
            val rotation = state.ctrlFront.getRotationFromNorth()
            val level = VisualizationToolItem.getLevel(state.visToolStack)

            GlStateManager.enableBlend()
            GlStateManager.depthMask(false)
            RenderingHelper.pushMatrix {
                GlStateManager.translate(
                    -(player.prevPosX + (player.posX - player.prevPosX) * partialTicks),
                    -(player.prevPosY + (player.posY - player.prevPosY) * partialTicks),
                    -(player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks)
                )

                GlStateManager.color(1F, 0F, 0F, 0.3F)
                val visWorld = DummyBlockAccessor.MapBacked()
                state.mbType.structureMatcher.visualization.forEach { (offset, matcher) ->
                    if (level != null && offset.y != level) return@forEach
                    // TODO mirroring
                    val pos = state.ctrlPos.add(rotation.rotate(offset, false))
                    if (!player.world.isAirBlock(pos)) {
                        if (matcher.matchBlock(player.world, pos, rotation) == null) {
                            drawCube(pos)
                        }
                    } else {
                        CbtClientHelper.indexByGlobalTimer(matcher.visualization)?.let {
                            visWorld.setBlockState(pos, it.blockState)
                        }
                    }
                }
                RenderingHelper.resetColour()

                GlStateManager.enableBlend()
                GlStateManager.blendFunc(
                    GlStateManager.SourceFactor.CONSTANT_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA
                )
                GL14.glBlendColor(1F, 1F, 1F, 0.3F)
                GlStateManager.depthMask(false)

                val tess = Tessellator.getInstance()
                val blockRenderer = mc.blockRendererDispatcher
                TextureResource.ITEM_BLOCK_ATLAS.bind()
                BlockRenderLayer.entries.forEach { renderLayer ->
                    ForgeHooksClient.setRenderLayer(renderLayer)
                    tess.tessellate(GL11.GL_QUADS, DefaultVertexFormats.BLOCK) {
                        visWorld.entries.forEach { (pos, state) ->
                            if (!state.getBlock().canRenderInLayer(state, renderLayer)) return@forEach
                            RenderingHelper.pushMatrix {
                                GlStateManager.translate(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                                blockRenderer.renderBlock(state, pos, visWorld, this)
                            }
                        }
                    }
                }
                ForgeHooksClient.setRenderLayer(null)

                GlStateManager.depthMask(true)
                GL14.glBlendColor(1F, 1F, 1F, 1F)
                GlStateManager.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                )
            }
        }
    }
}
