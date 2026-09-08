package st.evening.mc.cbtweaker.compat.jei.render

import mezz.jei.api.ingredients.IIngredientRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
object NoopIngredientRenderer : IIngredientRenderer<Any> {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> invoke(): IIngredientRenderer<T> = this as IIngredientRenderer<T>

    override fun render(minecraft: Minecraft, xPosition: Int, yPosition: Int, ingredient: Any?) {}

    override fun getTooltip(minecraft: Minecraft, ingredient: Any, tooltipFlag: ITooltipFlag): List<String> = listOf()
}
