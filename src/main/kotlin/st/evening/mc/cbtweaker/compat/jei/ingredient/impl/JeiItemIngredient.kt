package st.evening.mc.cbtweaker.compat.jei.ingredient.impl

import mezz.jei.api.ingredients.VanillaTypes
import mezz.jei.api.recipe.IIngredientType
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.prelude.api.util.data.letIf
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.OreEntry
import st.evening.mc.prelude.api.util.game.copyWithSize
import st.evening.mc.prelude.api.util.game.getColouredTooltip
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

class JeiItemIngredient(
    private val matchingStacks: List<ItemStack>,
    override val role: JeiIngredient.Role,
    override val annotation: JeiIngredient.Annotation?
) : JeiIngredient<ItemStack> {
    constructor(matchingStack: ItemStack, role: JeiIngredient.Role, annotation: JeiIngredient.Annotation?) :
        this(listOf(matchingStack), role, annotation)

    constructor(oreEntry: OreEntry, count: Int, role: JeiIngredient.Role, annotation: JeiIngredient.Annotation?) : this(
        oreEntry.getOreStacks().letIf(count > 1) { stacks -> stacks.map { it.copyWithSize(count) } },
        role,
        annotation
    )

    override val jeiIngredientType: IIngredientType<ItemStack>?
        get() = VanillaTypes.ITEM

    override fun getIngredients(): List<ItemStack> = matchingStacks

    @ClientSide.Physical
    override fun drawIcon(x: Int, y: Int, ingredient: ItemStack, partialTicks: Float) {
        if (ingredient.isEmpty) return
        GuiRenderHelper.withGuiItemSetup {
            val mc = Minecraft.getMinecraft()
            val renderItem = mc.renderItem
            renderItem.renderItemAndEffectIntoGUI(null, ingredient, x, y)
            annotation?.let {
                GlStateManager.disableLighting()
                GlStateManager.disableDepth()
                it.drawAnnotation(x + 16, y, partialTicks)
            }
            renderItem.renderItemOverlayIntoGUI(mc.fontRenderer, ingredient, x, y, null)
        }
    }

    @ClientSide.Physical
    override fun getTooltip(ingredient: ItemStack, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
        ingredient.getColouredTooltip(tooltip, tooltipFlags)
        annotation?.getAnnotationTooltip(tooltip, tooltipFlags)
    }
}
