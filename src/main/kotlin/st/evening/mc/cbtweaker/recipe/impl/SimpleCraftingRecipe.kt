package st.evening.mc.cbtweaker.recipe.impl

import mezz.jei.api.IJeiHelpers
import net.minecraft.util.math.MathHelper
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcher
import st.evening.mc.cbtweaker.compat.jei.CbtJeiPlugin
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredient
import st.evening.mc.cbtweaker.compat.jei.ingredient.MutableJeiIngredientAccumulateVisitor
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiAccumulatorMap
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiBufferGroup
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiRecipeSetAdaptor
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiUi
import st.evening.mc.cbtweaker.compat.jei.ui.impl.JeiIconElement
import st.evening.mc.cbtweaker.compat.jei.ui.impl.JeiProgressBarElement
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.recipe.RecipeSetType
import st.evening.mc.cbtweaker.util.gui.DrawableData
import st.evening.mc.cbtweaker.util.gui.SamplableData
import st.evening.mc.cbtweaker.util.machine.NumberModifier
import st.evening.mc.cbtweaker.util.recipe.IngredientLoader
import st.evening.mc.cbtweaker.util.recipe.IngredientMatcherMap
import st.evening.mc.cbtweaker.util.recipe.IngredientProviderMap
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useIntValue
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.prefab.DrawableBlank
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntArithmetic
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation

class SimpleCraftingRecipe(
    val id: String,
    val inputTable: Map<String, IngredientMatcherMap>,
    val outputTable: Map<String, IngredientProviderMap>,
    val duration: Int
) {
    fun computeModifiedDuration(modTable: Map<String, NumberModifier>): Int =
        modTable["duration"]?.let { MathHelper.ceil(it.modify(duration.toDouble())) } ?: duration

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

    class JeiConfig(
        val visible: Boolean,
        val progressBarBg: DrawableData,
        val progressBarFg: SamplableData,
        val barOffsetX: Int,
        val barOffsetY: Int,
        val barOrientation: DrawOrientation
    ) {
        companion object {
            val DEFAULT: JeiConfig = JeiConfig(
                true, CbtGuiData.PROGRESS_BAR_BG, CbtGuiData.PROGRESS_BAR_FG, 0, 0, DrawOrientation.LEFT_TO_RIGHT
            )
        }
    }

    class Database(val id: String, private val jeiConfig: JeiConfig) {
        val recipes: MutableMap<String, SimpleCraftingRecipe> = mutableMapOf()

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
            val barBg = config.progressBarBg.drawable
            val barElem = JeiProgressBarElement(
                region.posX + DrawAlignment.CENTER.computeOffset(IntArithmetic, barBg.width, region.width),
                region.posY + DrawAlignment.CENTER.computeOffset(IntArithmetic, barBg.height, region.height),
                recipe.duration,
                barBg,
                config.progressBarFg.drawable,
                config.barOffsetX,
                config.barOffsetY,
                config.barOrientation,
                jeiHelpers.guiHelper
            )
            container.addJeiUiElement(barElem)

            val inputIngs = mutableListOf<Pair<JeiIngredient<*>, String?>>()
            val outputIngs = mutableListOf<Pair<JeiIngredient<*>, String?>>()
            recipe.inputTable.forEach { (bufGroupId, matchers) ->
                matchers.forEach(object : IngredientMatcherMap.Visitor {
                    override fun <B, A, JB, JA> visitMatchers(
                        bufType: BufferType<B, A, JB, JA>,
                        matchers: List<IngredientMatcher<A, JA>>
                    ): Boolean {
                        matchers.forEach { matcher ->
                            matcher.getJeiIngredients().forEach {
                                when (it.role) {
                                    JeiIngredient.Role.INPUT -> inputIngs
                                    JeiIngredient.Role.OUTPUT -> outputIngs
                                } += it to bufGroupId
                            }
                        }
                        return true
                    }
                })
            }

            val barRegion = barElem.ingredientRegion
            JeiIconElement.layOutIconGroup(
                container,
                region.posX,
                region.posY,
                barRegion.posX - region.posX,
                region.height,
                DrawAlignment.CENTER,
                DrawAlignment.CENTER,
                inputIngs
            )
            val outputsX = barRegion.posX + barRegion.width
            JeiIconElement.layOutIconGroup(
                container,
                outputsX,
                region.posY,
                region.posX + region.width - outputsX,
                region.height,
                DrawAlignment.CENTER,
                DrawAlignment.CENTER,
                outputIngs
            )
        }
    }

    object Type : RecipeSetType<SimpleCraftingRecipe, Database> {
        override val debugName: String
            get() = "simple_crafting_recipe"

        context(_: JsonPath)
        override fun loadDatabase(id: String, dto: TJson.Object): Database = Database(
            id,
            dto.useObject("jei") { jeiDto ->
                JeiConfig(
                    jeiDto.expectBool("visible") ?: JeiConfig.DEFAULT.visible,
                    jeiDto.useAny("bar_bg") { DrawableData.loadSlice(it) } ?: JeiConfig.DEFAULT.progressBarBg,
                    jeiDto.useAny("bar_fg") { DrawableData.loadSlice(it) } ?: JeiConfig.DEFAULT.progressBarFg,
                    jeiDto.expectInt("bar_offset_x") ?: JeiConfig.DEFAULT.barOffsetX,
                    jeiDto.expectInt("bar_offset_y") ?: JeiConfig.DEFAULT.barOffsetY,
                    jeiDto.useString("bar_orientation") { DrawOrientation.serializer.deserializeFromJson(it) }
                        ?: JeiConfig.DEFAULT.barOrientation
                )
            } ?: JeiConfig.DEFAULT
        )

        context(_: JsonPath)
        override fun loadRecipe(database: Database, id: String, dto: TJson.Object) {
            database.recipes[id] = SimpleCraftingRecipe(
                id,
                dto.useObjectValue("inputs") { IngredientLoader.loadMatcherGroups(it) },
                dto.useObjectValue("outputs") { IngredientLoader.loadProviderGroups(it) },
                dto.useIntValue("duration") {
                    if (it <= 0) throw SerializationException.withPath("Duration must be positive!")
                    return@useIntValue it
                }
            )
        }

        override fun getRecipeCount(database: Database): Int = database.recipes.size

        override fun iterateRecipes(database: Database): Iterator<SimpleCraftingRecipe> =
            database.recipes.values.iterator()

        override fun getRecipeId(database: Database, recipe: SimpleCraftingRecipe): String = recipe.id

        override fun getRecipeById(database: Database, recipeId: String): SimpleCraftingRecipe? =
            database.recipes[recipeId]

        override fun getJeiRecipeAdaptor(database: Database): JeiRecipeSetAdaptor<SimpleCraftingRecipe>? =
            database.getJeiRecipeAdaptor()
    }
}
