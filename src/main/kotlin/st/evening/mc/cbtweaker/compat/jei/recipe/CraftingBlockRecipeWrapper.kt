package st.evening.mc.cbtweaker.compat.jei.recipe

import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe.IIngredientType
import mezz.jei.api.recipe.IRecipeWrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.util.ITooltipFlag
import org.apache.commons.lang3.mutable.MutableInt
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.compat.jei.ui.JeiUiElement
import st.evening.mc.cbtweaker.util.CbtClientHelper
import st.evening.mc.prelude.api.util.game.ClientHelper
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.containsPoint
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.gui.GuiRenderHelper

@ClientSide.Physical
class CraftingBlockRecipeWrapper : IRecipeWrapper, JeiUi {
    companion object {
        private fun <T : Any> layOutJeiIngredients(
            layout: IRecipeLayout,
            index: MutableInt,
            ingType: IIngredientType<T>,
            uiElems: List<JeiUiElement<T>>
        ) {
            val ingGroup = layout.getIngredientsGroup(ingType)
            uiElems.forEach { uiElem ->
                val indexHere = index.getAndIncrement()
                val region = uiElem.ingredientRegion
                val ing = uiElem.jeiIngredient
                if (ing != null) {
                    ingGroup.init(
                        indexHere, ing.role == JeiIngredient.Role.INPUT, JeiUiElement.JeiRenderer(uiElem),
                        region.posX, region.posY, region.width, region.height, 0, 0
                    )
                    ingGroup.set(indexHere, ing.getIngredients())
                } else {
                    ingGroup.init(
                        indexHere, false, JeiUiElement.JeiRenderer(uiElem),
                        region.posX, region.posY, region.width, region.height, 0, 0
                    )
                }
            }
        }
    }

    private val inputLists: MutableMap<IIngredientType<*>, MutableList<out List<*>>> = mutableMapOf()
    private val outputLists: MutableMap<IIngredientType<*>, MutableList<out List<*>>> = mutableMapOf()
    private val jeiElements: MutableMap<IIngredientType<*>, MutableList<JeiUiElement<*>>> = mutableMapOf()
    private val nonJeiElements: MutableList<NonJeiElement<*>> = mutableListOf()

    override fun addJeiUiElement(uiElem: JeiUiElement<*>) {
        addUiElement(uiElem)
    }

    private fun <T : Any> addUiElement(uiElem: JeiUiElement<T>) {
        val jeiIng = uiElem.jeiIngredient
        if (jeiIng != null) {
            val jeiIngType = jeiIng.jeiIngredientType
            if (jeiIngType != null) {
                jeiElements.getOrPut(jeiIngType) { mutableListOf() } += uiElem
                @Suppress("UNCHECKED_CAST")
                when (jeiIng.role) {
                    JeiIngredient.Role.INPUT -> inputLists
                    JeiIngredient.Role.OUTPUT -> outputLists
                }.getOrPut(jeiIngType) { mutableListOf() } as MutableList<List<T>> += jeiIng.getIngredients()
                return
            }
        }
        nonJeiElements += NonJeiElement(uiElem)
    }

    override fun getIngredients(ingredients: IIngredients) {
        inputLists.forEach { (ingType, ingLists) ->
            @Suppress("UNCHECKED_CAST")
            ingredients.setInputLists(ingType as IIngredientType<Any>, ingLists as List<List<Any>>)
        }
        outputLists.forEach { (ingType, ingLists) ->
            @Suppress("UNCHECKED_CAST")
            ingredients.setOutputLists(ingType as IIngredientType<Any>, ingLists as List<List<Any>>)
        }
    }

    fun layOutRecipe(layout: IRecipeLayout) {
        val index = MutableInt(0)
        jeiElements.forEach { (ingType, uiElems) ->
            @Suppress("UNCHECKED_CAST")
            layOutJeiIngredients(layout, index, ingType as IIngredientType<Any>, uiElems as List<JeiUiElement<Any>>)
        }
    }

    override fun drawInfo(minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int) {
        if (nonJeiElements.isEmpty()) return
        GlStateManager.enableBlend()
        val partialTicks = minecraft.renderPartialTicks
        nonJeiElements.forEach {
            it.draw(mouseX, mouseY, partialTicks)
        }
    }

    override fun getTooltipStrings(mouseX: Int, mouseY: Int): List<String> {
        val tooltipFlags = ClientHelper.tooltipFlags
        val tooltip = mutableListOf<String>()
        nonJeiElements.forEach {
            it.getTooltip(mouseX, mouseY, tooltip, tooltipFlags)
            if (tooltip.isNotEmpty()) return tooltip
        }
        return listOf()
    }

    private class NonJeiElement<T : Any>(private val element: JeiUiElement<T>) {
        private val ingredients: List<T>? = element.jeiIngredient?.getIngredients()

        fun draw(mouseX: Int, mouseY: Int, partialTicks: Float) {
            GlStateManager.enableBlend()
            ingredients?.let { ings ->
                element.drawElement(CbtClientHelper.indexByGlobalTimer(ings), partialTicks)
                val ingRegion = element.ingredientRegion
                if (ingRegion.containsPoint(mouseX, mouseY)) {
                    GlStateManager.disableTexture2D()
                    GlStateManager.color(1F, 1F, 1F, 0.5F)
                    val x = ingRegion.posX
                    val y = ingRegion.posY
                    GuiRenderHelper.drawUntexturedQuad(x, y, x + ingRegion.width, y + ingRegion.height)
                    RenderingHelper.resetColour()
                    GlStateManager.enableTexture2D()
                }
                return
            }
            element.drawElement(null, partialTicks)
        }

        fun getTooltip(mouseX: Int, mouseY: Int, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
            if (!element.ingredientRegion.containsPoint(mouseX, mouseY)) return
            element.getTooltip(ingredients?.let { CbtClientHelper.indexByGlobalTimer(it) }, tooltip, tooltipFlags)
        }
    }
}
