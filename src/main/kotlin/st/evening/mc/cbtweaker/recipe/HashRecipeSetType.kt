package st.evening.mc.cbtweaker.recipe

import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson

open class HashRecipeDatabase<R> {
    val recipeMap: MutableMap<String, R> = mutableMapOf()
}

abstract class HashRecipeSetType<R, D : HashRecipeDatabase<R>> : RecipeSetType<R, D> {
    context(_: JsonPath)
    override fun loadRecipe(database: D, id: String, dto: TJson.Object) {
        database.recipeMap[id] = loadRecipe(id, dto)
    }

    context(_: JsonPath)
    protected abstract fun loadRecipe(id: String, dto: TJson.Object): R

    override fun getRecipeCount(database: D): Int = database.recipeMap.size

    override fun iterateRecipes(database: D): Iterator<R> = database.recipeMap.values.iterator()

    override fun getRecipeById(database: D, recipeId: String): R? = database.recipeMap[recipeId]
}
