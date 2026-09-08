package st.evening.mc.cbtweaker.util.gui

import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.util.math.MathHelper
import net.minecraftforge.fluids.FluidStack
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.drawTiledColumn
import st.evening.mc.prelude.api.gui.drawable.prefab.asDrawable
import st.evening.mc.prelude.api.gui.drawable.sliceSized
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.getStillSprite
import st.evening.mc.prelude.api.util.math.IntRectangle

@ClientSide.Physical
abstract class SpriteBarRenderer<T> {
    private var cachedDrawable: Pair<TextureAtlasSprite, GuiSamplable>? = null

    protected abstract fun getAmount(contents: T): Int

    protected abstract fun getSprite(contents: T): TextureAtlasSprite

    protected abstract fun wrapSprite(contents: T, sprite: TextureAtlasSprite): GuiSamplable

    fun drawBar(contents: T?, capacity: Int, region: IntRectangle, partialTicks: Float) {
        drawBar(contents, capacity, region.posX, region.posY, region.width, region.height, partialTicks)
    }

    fun drawBar(contents: T, amount: Int, capacity: Int, region: IntRectangle, partialTicks: Float) {
        drawBar(contents, amount, capacity, region.posX, region.posY, region.width, region.height, partialTicks)
    }

    fun drawBar(contents: T?, capacity: Int, x: Int, y: Int, width: Int, height: Int, partialTicks: Float) {
        if (contents == null) return
        drawBar(contents, getAmount(contents), capacity, x, y, width, height, partialTicks)
    }

    fun drawBar(
        contents: T,
        amount: Int,
        capacity: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        partialTicks: Float
    ) {
        if (amount <= 0) return
        val sprite = getSprite(contents)
        cachedDrawable?.let { (cachedSprite, drawable) ->
            if (cachedSprite === sprite) {
                drawBarSprite(drawable, x, y, height, amount / capacity.toFloat(), partialTicks)
                return
            }
        }
        val drawable = wrapSprite(contents, sprite).sliceSized(width, sprite.iconHeight, 0, 0)
        cachedDrawable = sprite to drawable
        drawBarSprite(drawable, x, y, height, amount / capacity.toFloat(), partialTicks)
    }

    private fun drawBarSprite(sprite: GuiSamplable, x: Int, y: Int, height: Int, fill: Float, partialTicks: Float) {
        val dy = if (fill < 1F) MathHelper.ceil(height * (1 - fill)) else 0
        sprite.drawTiledColumn(partialTicks, x, y + dy, height - dy)
    }
}

@ClientSide.Physical
class FluidBarRenderer : SpriteBarRenderer<FluidStack>() {
    override fun getAmount(contents: FluidStack): Int = contents.amount

    override fun getSprite(contents: FluidStack): TextureAtlasSprite = contents.getStillSprite()

    override fun wrapSprite(contents: FluidStack, sprite: TextureAtlasSprite): GuiSamplable =
        sprite.asDrawable(tint = contents.fluid.getColor(contents))
}
