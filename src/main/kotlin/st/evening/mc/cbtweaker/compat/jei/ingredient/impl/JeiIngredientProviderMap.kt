package st.evening.mc.cbtweaker.compat.jei.ingredient.impl

import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.compat.jei.ingredient.JeiIngredientProvider

class JeiIngredientProviderMap {
    private val providerTable: MutableMap<BufferType<*, *, *, *>, List<JeiIngredientProvider<*>>> =
        mutableMapOf()

    val bufferTypes: Collection<BufferType<*, *, *, *>>
        get() = providerTable.keys

    operator fun <JA> set(bufType: BufferType<*, *, *, JA>, providers: List<JeiIngredientProvider<JA>>) {
        providerTable[bufType] = providers
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <JA> get(bufType: BufferType<*, *, *, JA>): List<JeiIngredientProvider<JA>> =
        providerTable[bufType] as? List<JeiIngredientProvider<JA>> ?: emptyList()

    fun forEach(visitor: Visitor): Boolean {
        providerTable.entries.forEach { (bufType, providers) ->
            @Suppress("UNCHECKED_CAST")
            if (!visitor.visit(bufType as BufferType<*, *, *, Any>, providers as List<JeiIngredientProvider<Any>>)) {
                return false
            }
        }
        return true
    }

    interface Visitor {
        fun <JB, JA> visit(bufType: BufferType<*, *, JB, JA>, providers: List<JeiIngredientProvider<JA>>): Boolean
    }
}
