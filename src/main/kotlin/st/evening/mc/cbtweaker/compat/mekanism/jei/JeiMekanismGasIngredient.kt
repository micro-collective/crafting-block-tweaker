package st.evening.mc.cbtweaker.compat.mekanism.jei

import mekanism.api.gas.GasStack
import mekanism.client.jei.MekanismJEI
import mezz.jei.api.recipe.IIngredientType
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.RequireMod
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.TextureResource
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

@RequireMod(MekanismCompat.MOD_ID)
class JeiMekanismGasIngredient(
    val gasStack: GasStack,
    val unitName: String,
    override val role: JeiIngredient.Role,
    override val annotation: JeiIngredient.Annotation?
) : JeiIngredient<GasStack> {
    constructor(gasStack: GasStack, isRate: Boolean, role: JeiIngredient.Role, annotation: JeiIngredient.Annotation?) :
        this(gasStack, if (isRate) "mB/t" else "mB", role, annotation)

    override val jeiIngredientType: IIngredientType<GasStack>?
        get() = MekanismJEI.TYPE_GAS

    override fun getIngredients(): List<GasStack> = listOf(gasStack)

    @ClientSide.Physical
    override fun drawIcon(x: Int, y: Int, ingredient: GasStack, partialTicks: Float) {
        TextureResource.ITEM_BLOCK_ATLAS.bind()
        RenderingHelper.setColourRgb(ingredient.gas.tint)
        GuiRenderHelper.drawAtlasSprite(x, y, x + 16, y + 16, ingredient.gas.sprite)
        RenderingHelper.resetColour()
        annotation?.drawAnnotation(x + 16, y, partialTicks)
    }

    @ClientSide.Physical
    override fun getTooltip(ingredient: GasStack, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += ingredient.gas.localizedName
        tooltip += "${TextFormatting.GRAY}${"%,d %s".format(ingredient.amount, unitName)}"
        annotation?.getAnnotationTooltip(tooltip, tooltipFlags)
    }
}
