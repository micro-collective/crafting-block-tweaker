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

class TimedFuelRecipe(
    val id: String,
    val inputTable: Map<String, IngredientMatcherMap>,
    val duration: Int
) {
    fun populateJei(recipe: TimedFuelRecipe, bufGroups: Map<String, JeiBufferGroup>) {
        val accs = JeiAccumulatorMap(bufGroups)
        val visitor = MutableJeiIngredientAccumulateVisitor()
        recipe.inputTable.forEach { (bufGroupId, inputMap) ->
            visitor.jeiAccumulators = accs[bufGroupId]!!
            inputMap.forEach(visitor)
        }
    }

    class JeiConfig(val visible: Boolean, val fuelBar: BarDrawData) {
        companion object {
            val DEFAULT: JeiConfig = JeiConfig(
                true,
                BarDrawData(CbtGuiData.FUEL_BAR_BG, CbtGuiData.FUEL_BAR_FG, 0, 0, DrawOrientation.BOTTOM_TO_TOP)
            )
        }
    }

    class Database(val id: String, private val jeiConfig: JeiConfig) : HashRecipeDatabase<TimedFuelRecipe>() {
        fun getJeiRecipeAdaptor(): JeiRecipeSetAdaptor<TimedFuelRecipe>? =
            if (jeiConfig.visible) JeiIconAdaptor(id, null, jeiConfig) else null
    }

    class JeiIconAdaptor(
        private val id: String,
        override val jeiDiscriminator: String?,
        private val config: JeiConfig
    ) : JeiRecipeSetAdaptor<TimedFuelRecipe> {
        @ClientSide.Physical
        override fun getJeiCategoryName(): String = CbtJeiPlugin.getRecipeSetCategoryName(id, jeiDiscriminator)

        @ClientSide.Physical
        override fun getJeiBackground(): GuiDrawable = DrawableBlank(61 + config.fuelBar.bgTexture.drawable.width, 55)

        @ClientSide.Physical
        override fun addJeiUiElements(
            recipe: TimedFuelRecipe,
            container: JeiUi,
            region: IntRectangle,
            jeiHelpers: IJeiHelpers
        ) {
            val fuelBar = config.fuelBar
            val barBg = fuelBar.bgTexture.drawable
            val barElem = JeiProgressBarElement(
                region.posX + DrawAlignment.END.computeOffset(IntArithmetic, barBg.width, region.width) - 3,
                region.posY + DrawAlignment.CENTER.computeOffset(IntArithmetic, barBg.height, region.height),
                fuelBar,
                recipe.duration,
                jeiHelpers.guiHelper,
                reverse = true
            )
            container.addJeiUiElement(barElem)

            val ings = mutableListOf<Pair<JeiIngredient<*>, String?>>()
            val visitor = MutableJeiIngredientPartitionVisitor(ings, null)
            recipe.inputTable.forEach { (bufGroupId, matchers) ->
                visitor.bufGroupId = bufGroupId
                matchers.forEach(visitor)
            }

            val ingRegion = Rect2i(
                region.posX,
                region.posY,
                region.width - barElem.ingredientRegion.width - 6,
                region.height
            )
            container.addJeiUiElement(JeiBackgroundBoxElement(ingRegion, 0))
            JeiIconElement.layOutIconGroup(container, ingRegion, DrawAlignment.CENTER, DrawAlignment.CENTER, ings)
        }
    }

    object Type : HashRecipeSetType<TimedFuelRecipe, Database>() {
        override val debugName: String
            get() = "timed_fuel_recipe"

        context(_: JsonPath)
        override fun loadDatabase(id: String, dto: TJson.Object): Database = Database(
            id,
            dto.useObject("jei") { jeiDto ->
                JeiConfig(
                    jeiDto.expectBool("visible") ?: JeiConfig.DEFAULT.visible,
                    jeiDto.useObject("fuel_bar") {
                        BarDrawData.load(it, JeiConfig.DEFAULT.fuelBar)
                    } ?: JeiConfig.DEFAULT.fuelBar
                )
            } ?: JeiConfig.DEFAULT
        )

        context(_: JsonPath)
        override fun loadRecipe(id: String, dto: TJson.Object): TimedFuelRecipe = TimedFuelRecipe(
            id,
            dto.useObjectValue("inputs") { IngredientLoader.loadMatcherGroups(it) },
            dto.useIntValue("duration") {
                if (it <= 0) throw SerializationException.withPath("Duration must be positive!")
                return@useIntValue it
            }
        )

        override fun getRecipeId(database: Database, recipe: TimedFuelRecipe): String = recipe.id

        override fun getJeiRecipeAdaptor(database: Database): JeiRecipeSetAdaptor<TimedFuelRecipe>? =
            database.getJeiRecipeAdaptor()
    }
}
