package st.evening.mc.cbtweaker.buffer.ingredient

import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredientProvider

interface IngredientMatcher<A, JA> : JeiIngredientProvider<JA> {
    fun consumeInitial(acc: Lazy<A>, consumeFactor: Float, determMode: Boolean): Boolean = true

    fun consumePeriodic(acc: Lazy<A>, consumeFactor: Float): Boolean = true
}
