package st.evening.mc.cbtweaker.compat.mekanism.gui

import mekanism.api.gas.GasStack
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.cbtweaker.util.gui.SpriteBarRenderer
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.prefab.asDrawable
import st.evening.mc.prelude.api.util.data.BitwiseHelper
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.RequireMod

@RequireMod(MekanismCompat.MOD_ID)
@ClientSide.Physical
class GasBarRenderer : SpriteBarRenderer<GasStack>() {
    override fun getAmount(contents: GasStack): Int = contents.amount

    override fun getSprite(contents: GasStack): TextureAtlasSprite = contents.gas.sprite

    override fun wrapSprite(contents: GasStack, sprite: TextureAtlasSprite): GuiSamplable =
        sprite.asDrawable(tint = contents.gas.tint or BitwiseHelper.FF000000)
}
