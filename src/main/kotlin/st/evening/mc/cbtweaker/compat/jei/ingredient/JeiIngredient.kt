package st.evening.mc.cbtweaker.compat.jei.ingredient

import mezz.jei.api.recipe.IIngredientType
import net.minecraft.client.util.ITooltipFlag
import st.evening.mc.prelude.api.util.game.ClientSide

interface JeiIngredient<T : Any> {
    val jeiIngredientType: IIngredientType<T>?
        get() = null

    val role: Role

    fun getIngredients(): List<T>

    @ClientSide.Physical
    fun drawIcon(x: Int, y: Int, ingredient: T, partialTicks: Float)

    @ClientSide.Physical
    fun getTooltip(ingredient: T, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag)

    enum class Role {
        INPUT, OUTPUT
    }
}
