package st.evening.mc.cbtweaker.structure

import net.minecraft.client.particle.Particle
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.RenderingHelper


@ClientSide.Physical
class StructureHighlightParticle(world: World, private val blockPos: BlockPos, ttl: Int) :
    Particle(world, blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble()) {

    init {
        particleMaxAge = ttl
    }

    override fun onUpdate() {
        prevPosX = posX
        prevPosY = posY
        prevPosZ = posZ
        if (++particleAge > particleMaxAge) {
            setExpired()
        }
    }

    override fun getFXLayer(): Int = 3

    override fun getBrightnessForRender(partialTick: Float): Int = 3

    override fun renderParticle(
        buffer: BufferBuilder,
        entity: Entity,
        partialTicks: Float,
        rotationX: Float,
        rotationZ: Float,
        rotationYZ: Float,
        rotationXY: Float,
        rotationXZ: Float
    ) {
        if (particleAge > particleMaxAge) return
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
        GlStateManager.disableAlpha()
        val t = (1F - (particleAge + partialTicks) / particleMaxAge.toFloat()).coerceAtLeast(0F)
        GlStateManager.color(1F, 1F, 1F, t * t / 3F)
        RenderingHelper.pushMatrix {
            RenderingHelper.translateInverseMotionInterpolation(entity, partialTicks)
            VisualizationRenderer.drawCube(blockPos)
        }
        GlStateManager.enableAlpha()
        RenderingHelper.resetColour()
    }
}
