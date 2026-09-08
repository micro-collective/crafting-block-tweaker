package st.evening.mc.cbtweaker.compat.jei.ingredient

import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcher
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientProvider
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiAccumulatorMap
import st.evening.mc.cbtweaker.util.recipe.IngredientMatcherMap
import st.evening.mc.cbtweaker.util.recipe.IngredientProviderMap

interface JeiIngredientProvider<JA> {
    fun getJeiIngredients(): Collection<JeiIngredient<*>>

    fun populateJei(acc: JA): Boolean
}

class MutableJeiIngredientCollectVisitor : IngredientMatcherMap.Visitor, IngredientProviderMap.Visitor {
    lateinit var jeiIngredients: MutableMap<BufferType<*, *, *, *>, MutableList<JeiIngredient<*>>>

    override fun <B, A, JB, JA> visitMatchers(
        bufType: BufferType<B, A, JB, JA>,
        matchers: List<IngredientMatcher<A, JA>>
    ): Boolean {
        matchers.flatMapTo(jeiIngredients.getOrPut(bufType) { mutableListOf() }) { it.getJeiIngredients() }
        return true
    }

    override fun <B, A, JB, JA> visitProviders(
        bufType: BufferType<B, A, JB, JA>,
        providers: List<IngredientProvider<A, JA>>
    ): Boolean {
        providers.flatMapTo(jeiIngredients.getOrPut(bufType) { mutableListOf() }) { it.getJeiIngredients() }
        return true
    }
}

class MutableJeiIngredientAccumulateVisitor : IngredientMatcherMap.Visitor, IngredientProviderMap.Visitor {
    lateinit var jeiAccumulators: JeiAccumulatorMap.Group

    override fun <B, A, JB, JA> visitMatchers(
        bufType: BufferType<B, A, JB, JA>,
        matchers: List<IngredientMatcher<A, JA>>
    ): Boolean {
        val acc = jeiAccumulators.getAccumulator(bufType) ?: return true // recipe will be missing inputs!
        matchers.forEach { it.populateJei(acc) }
        return true
    }

    override fun <B, A, JB, JA> visitProviders(
        bufType: BufferType<B, A, JB, JA>,
        providers: List<IngredientProvider<A, JA>>
    ): Boolean {
        val acc = jeiAccumulators.getAccumulator(bufType) ?: return true // recipe will be missing outputs!
        providers.forEach { it.populateJei(acc) }
        return true
    }
}
