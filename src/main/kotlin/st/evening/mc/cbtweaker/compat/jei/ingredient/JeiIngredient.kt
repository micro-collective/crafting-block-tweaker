package st.evening.mc.cbtweaker.compat.jei.ingredient

import mezz.jei.api.recipe.IIngredientType
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.util.text.TextFormatting
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.cbtweaker.gui.CbtGuiResources
import st.evening.mc.prelude.api.gui.drawable.drawFullSize
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.text.toStringPercentage

interface JeiIngredient<T : Any> {
    val jeiIngredientType: IIngredientType<T>?
        get() = null

    val role: Role

    val annotation: Annotation?

    fun getIngredients(): List<T>

    @ClientSide.Physical
    fun drawIcon(x: Int, y: Int, ingredient: T, partialTicks: Float)

    @ClientSide.Physical
    fun getTooltip(ingredient: T, tooltip: MutableList<String>, tooltipFlags: ITooltipFlag)

    enum class Role {
        INPUT, OUTPUT
    }

    interface Annotation {
        @ClientSide.Physical
        fun drawAnnotation(x: Int, y: Int, partialTicks: Float)

        @ClientSide.Physical
        fun drawAnnotation(region: IntRectangle, partialTicks: Float) {
            drawAnnotation(region.posX + region.width, region.posY, partialTicks)
        }

        @ClientSide.Physical
        fun getAnnotationTooltip(tooltip: MutableList<String>, tooltipFlags: ITooltipFlag)

        companion object {
            fun fromConsume(doConsume: Boolean): Annotation? = if (doConsume) null else Keep

            fun fromChance(chance: Float): Annotation? = if (chance < 1F) Chance(chance) else null
        }

        object Keep : Annotation {
            @ClientSide.Physical
            override fun drawAnnotation(x: Int, y: Int, partialTicks: Float) {
                CbtGuiResources.ICON_ING_KEEP.drawFullSize(partialTicks, x - 6, y - 1)
            }

            @ClientSide.Physical
            override fun getAnnotationTooltip(tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
                tooltip += "${TextFormatting.GREEN}${I18n.format(CbtLang.TOOLTIP_ING_KEEP)}"
            }
        }

        class Damage(val amount: Int) : Annotation {
            @ClientSide.Physical
            override fun drawAnnotation(x: Int, y: Int, partialTicks: Float) {
                CbtGuiResources.ICON_ING_DAMAGE.drawFullSize(partialTicks, x - 6, y - 1)
            }

            @ClientSide.Physical
            override fun getAnnotationTooltip(tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
                tooltip += "${TextFormatting.YELLOW}${I18n.format(CbtLang.TOOLTIP_ING_DAMAGE, amount)}"
            }
        }

        class Chance(val chance: Float) : Annotation {
            @ClientSide.Physical
            override fun drawAnnotation(x: Int, y: Int, partialTicks: Float) {
                CbtGuiResources.ICON_ING_CHANCE.drawFullSize(partialTicks, x - 6, y - 1)
            }

            @ClientSide.Physical
            override fun getAnnotationTooltip(tooltip: MutableList<String>, tooltipFlags: ITooltipFlag) {
                tooltip +=
                    "${TextFormatting.GOLD}${I18n.format(CbtLang.TOOLTIP_ING_CHANCE, chance.toStringPercentage())}"
            }
        }
    }
}
