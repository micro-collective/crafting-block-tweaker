package st.evening.mc.cbtweaker.recipe.impl

import mezz.jei.api.IJeiHelpers
import st.evening.mc.cbtweaker.compat.jei.CbtJeiPlugin
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.compat.jei.ingredient.MutableJeiIngredientAccumulateVisitor
import st.evening.mc.cbtweaker.compat.jei.ingredient.MutableJeiIngredientPartitionVisitor
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiAccumulatorMap
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiBufferGroup
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiRecipeSetAdaptor
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiUi
import st.evening.mc.cbtweaker.compat.jei.ui.impl.JeiBackgroundBoxElement
import st.evening.mc.cbtweaker.compat.jei.ui.impl.JeiIconElement
import st.evening.mc.cbtweaker.compat.jei.ui.impl.JeiProgressBarElement
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.recipe.HashRecipeDatabase
import st.evening.mc.cbtweaker.recipe.HashRecipeSetType
import st.evening.mc.cbtweaker.util.gui.BarDrawData
import st.evening.mc.cbtweaker.util.recipe.IngredientLoader
import st.evening.mc.cbtweaker.util.recipe.IngredientMatcherMap
import st.evening.mc.cbtweaker.util.recipe.IngredientProviderMap
import st.evening.mc.cbtweaker.util.recipe.RecipeExecutor
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.useIntValue
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.prefab.DrawableBlank
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntArithmetic
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation

class SimpleCraftingRecipe(
    val id: String,
    val inputTable: Map<String, IngredientMatcherMap>,
    val outputTable: Map<String, IngredientProviderMap>,
    val duration: Int
) {
    fun populateJei(recipe: SimpleCraftingRecipe, bufGroups: Map<String, JeiBufferGroup>) {
        val accs = JeiAccumulatorMap(bufGroups)
        val visitor = MutableJeiIngredientAccumulateVisitor()
        recipe.inputTable.forEach { (bufGroupId, inputMap) ->
            visitor.jeiAccumulators = accs[bufGroupId]!!
            inputMap.forEach(visitor)
        }
        recipe.outputTable.forEach { (bufGroupId, outputMap) ->
            visitor.jeiAccumulators = accs[bufGroupId]!!
            outputMap.forEach(visitor)
        }
    }

    class JeiConfig(val visible: Boolean, val progressBar: BarDrawData) {
        companion object {
            val DEFAULT: JeiConfig = JeiConfig(
                true,
                BarDrawData(CbtGuiData.PROGRESS_BAR_BG, CbtGuiData.PROGRESS_BAR_FG, 0, 0, DrawOrientation.LEFT_TO_RIGHT)
            )
        }
    }

    class Database(val id: String, private val jeiConfig: JeiConfig) : HashRecipeDatabase<SimpleCraftingRecipe>() {
        fun getJeiRecipeAdaptor(): JeiRecipeSetAdaptor<SimpleCraftingRecipe>? =
            if (jeiConfig.visible) JeiIconAdaptor(id, null, jeiConfig) else null
    }

    class JeiIconAdaptor(
        private val id: String,
        override val jeiDiscriminator: String?,
        private val config: JeiConfig
    ) : JeiRecipeSetAdaptor<SimpleCraftingRecipe> {
        @ClientSide.Physical
        override fun getJeiCategoryName(): String = CbtJeiPlugin.getRecipeSetCategoryName(id, jeiDiscriminator)

        @ClientSide.Physical
        override fun getJeiBackground(): GuiDrawable = DrawableBlank(162, 55)

        @ClientSide.Physical
        override fun addJeiUiElements(
            recipe: SimpleCraftingRecipe,
            container: JeiUi,
            region: IntRectangle,
            jeiHelpers: IJeiHelpers
        ) {
            val progressBar = config.progressBar
            val barBg = progressBar.bgTexture.drawable
            val barElem = JeiProgressBarElement(
                region.posX + DrawAlignment.CENTER.computeOffset(IntArithmetic, barBg.width, region.width),
                region.posY + DrawAlignment.CENTER.computeOffset(IntArithmetic, barBg.height, region.height),
                progressBar,
                recipe.duration,
                jeiHelpers.guiHelper
            )
            container.addJeiUiElement(barElem)

            val inputIngs = mutableListOf<Pair<JeiIngredient<*>, String?>>()
            val outputIngs = mutableListOf<Pair<JeiIngredient<*>, String?>>()
            val visitor = MutableJeiIngredientPartitionVisitor(inputIngs, outputIngs)
            recipe.inputTable.forEach { (bufGroupId, matchers) ->
                visitor.bufGroupId = bufGroupId
                matchers.forEach(visitor)
            }
            recipe.outputTable.forEach { (bufGroupId, providers) ->
                visitor.bufGroupId = bufGroupId
                providers.forEach(visitor)
            }

            val barRegion = barElem.ingredientRegion
            val inRegion = Rect2i(region.posX, region.posY, barRegion.posX - region.posX - 4, region.height)
            container.addJeiUiElement(JeiBackgroundBoxElement(inRegion, 0))
            val outputsX = barRegion.posX + barRegion.width + 4
            val outRegion = Rect2i(outputsX, region.posY, region.posX + region.width - outputsX, region.height)
            container.addJeiUiElement(JeiBackgroundBoxElement(outRegion, 0))

            JeiIconElement.layOutIconGroup(container, inRegion, DrawAlignment.CENTER, DrawAlignment.CENTER, inputIngs)
            JeiIconElement.layOutIconGroup(container, outRegion, DrawAlignment.CENTER, DrawAlignment.CENTER, outputIngs)
        }
    }

    object Type : HashRecipeSetType<SimpleCraftingRecipe, Database>() {
        override val debugName: String
            get() = "simple_crafting_recipe"

        context(_: JsonPath)
        override fun loadDatabase(id: String, dto: TJson.Object): Database = Database(
            id,
            dto.useObject("jei") { jeiDto ->
                JeiConfig(
                    jeiDto.expectBool("visible") ?: JeiConfig.DEFAULT.visible,
                    jeiDto.useObject("progress_bar") {
                        BarDrawData.load(it, JeiConfig.DEFAULT.progressBar)
                    } ?: JeiConfig.DEFAULT.progressBar
                )
            } ?: JeiConfig.DEFAULT
        )

        context(_: JsonPath)
        override fun loadRecipe(id: String, dto: TJson.Object): SimpleCraftingRecipe = SimpleCraftingRecipe(
            id,
            dto.useObjectValue("inputs") { IngredientLoader.loadMatcherGroups(it) },
            dto.useObjectValue("outputs") { IngredientLoader.loadProviderGroups(it) },
            dto.useIntValue("duration") {
                if (it <= 0) throw SerializationException.withPath("Duration must be positive!")
                return@useIntValue it
            }
        )

        override fun getRecipeId(database: Database, recipe: SimpleCraftingRecipe): String = recipe.id

        override fun getJeiRecipeAdaptor(database: Database): JeiRecipeSetAdaptor<SimpleCraftingRecipe>? =
            database.getJeiRecipeAdaptor()
    }

    abstract class Executor : RecipeExecutor<SimpleCraftingRecipe>() {
        override fun getRecipeId(recipe: SimpleCraftingRecipe): String = recipe.id

        override fun getRecipeDuration(recipe: SimpleCraftingRecipe): Int = recipe.duration

        override fun getRecipeInputs(recipe: SimpleCraftingRecipe): Map<String, IngredientMatcherMap> =
            recipe.inputTable

        override fun getRecipeOutputs(recipe: SimpleCraftingRecipe): Map<String, IngredientProviderMap> =
            recipe.outputTable
    }
}
