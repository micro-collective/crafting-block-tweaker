package st.evening.mc.cbtweaker.recipe

import st.evening.mc.cbtweaker.compat.jei.recipe.JeiRecipeSetAdaptor
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson

interface RecipeSetType<R, D> {
    val debugName: String

    context(_: JsonPath)
    fun loadDatabase(id: String, dto: TJson.Object): D

    context(_: JsonPath)
    fun loadRecipe(database: D, id: String, dto: TJson.Object)

    fun getRecipeCount(database: D): Int

    fun iterateRecipes(database: D): Iterator<R>

    fun getRecipeId(database: D, recipe: R): String

    fun getRecipeById(database: D, recipeId: String): R?

    fun getJeiRecipeAdaptor(database: D): JeiRecipeSetAdaptor<R>?
}
