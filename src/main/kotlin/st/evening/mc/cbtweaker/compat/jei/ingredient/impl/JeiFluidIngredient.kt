package st.evening.mc.cbtweaker.compat.jei.ingredient.impl

import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IIngredientType
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fluids.FluidStack
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.getStillSprite
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.TextureResource
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

class JeiFluidIngredient(
    val fluidStack: FluidStack,
    val unitName: String,
    override val role: JeiIngredient.Role,
    override val annotation: JeiIngredient.Annotation?
) : JeiIngredient<FluidStack> {
    constructor(
        fluidStack: FluidStack,
        isRate: Boolean,
        role: JeiIngredient.Role,
        annotation: JeiIngredient.Annotation?
    ) : this(fluidStack, if (isRate) "mB/t" else "mB", role, annotation)

    override val jeiIngredientType: IIngredientType<FluidStack>
        get() = VanillaTypes.FLUID

    override fun getIngredients(): List<FluidStack> = listOf(fluidStack)

    @ClientSide.Physical
    override fun drawIcon(x: Int, y: Int, ingredient: FluidStack, partialTicks: Float) {
        TextureResource.ITEM_BLOCK_ATLAS.bind()
        RenderingHelper.setColourArgb(ingredient.fluid.getColor(ingredient))
        GuiRenderHelper.drawAtlasSprite(x, y, x + 16, y + 16, ingredient.getStillSprite())
        RenderingHelper.resetColour()
        annotation?.drawAnnotation(x + 16, y, partialTicks)
    }

    @ClientSide.Physical
    override fun getTooltip(ingredient: FluidStack, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        tooltip += "${ingredient.fluid.getRarity(ingredient).color}${ingredient.localizedName}"
        tooltip += "${TextFormatting.GRAY}%,d %s".format(ingredient.amount, unitName)
        annotation?.getAnnotationTooltip(tooltip, tooltipFlags)
    }
}
