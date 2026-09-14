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

class MutableJeiIngredientPartitionVisitor(
    private val inputIngs: MutableList<Pair<JeiIngredient<*>, String?>>?,
    private val outputIngs: MutableList<Pair<JeiIngredient<*>, String?>>?,
) : IngredientMatcherMap.Visitor, IngredientProviderMap.Visitor {
    lateinit var bufGroupId: String

    override fun <B, A, JB, JA> visitMatchers(
        bufType: BufferType<B, A, JB, JA>,
        matchers: List<IngredientMatcher<A, JA>>
    ): Boolean {
        addIngredients(matchers)
        return true
    }

    override fun <B, A, JB, JA> visitProviders(
        bufType: BufferType<B, A, JB, JA>,
        providers: List<IngredientProvider<A, JA>>
    ): Boolean {
        addIngredients(providers)
        return true
    }

    private fun addIngredients(providers: List<JeiIngredientProvider<*>>) {
        providers.forEach { provider ->
            provider.getJeiIngredients().forEach { ingredient ->
                when (ingredient.role) {
                    JeiIngredient.Role.INPUT -> inputIngs
                    JeiIngredient.Role.OUTPUT -> outputIngs
                }?.let {
                    it += ingredient to bufGroupId
                }
            }
        }
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
