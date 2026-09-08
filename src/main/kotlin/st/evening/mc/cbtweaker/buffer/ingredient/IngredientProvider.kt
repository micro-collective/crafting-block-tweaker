package st.evening.mc.cbtweaker.buffer.ingredient

import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredientProvider

interface IngredientProvider<A, JA> : JeiIngredientProvider<JA> {
    fun insertPeriodic(acc: Lazy<A>): Boolean = true

    fun insertFinal(acc: Lazy<A>): Boolean = true
}
