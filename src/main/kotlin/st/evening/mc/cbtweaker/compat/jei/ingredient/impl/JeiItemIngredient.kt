package st.evening.mc.cbtweaker.compat.jei.ingredient.impl

import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IIngredientType
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.getColouredTooltip
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

class JeiItemIngredient(
    private val matchingStacks: List<ItemStack>,
    override val role: JeiIngredient.Role
) : JeiIngredient<ItemStack> {
    constructor(matchingStack: ItemStack, role: JeiIngredient.Role) : this(listOf(matchingStack), role)

    override val jeiIngredientType: IIngredientType<ItemStack>?
        get() = VanillaTypes.ITEM

    override fun getIngredients(): List<ItemStack> = matchingStacks

    @ClientSide.Physical
    override fun drawIcon(x: Int, y: Int, ingredient: ItemStack, partialTicks: Float) {
        GuiRenderHelper.drawItemAndOverlay(x, y, ingredient)
    }

    @ClientSide.Physical
    override fun getTooltip(ingredient: ItemStack, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        ingredient.getColouredTooltip(tooltip, tooltipFlags)
    }
}
