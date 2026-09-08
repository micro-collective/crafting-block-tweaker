package st.evening.mc.cbtweaker.buffer.ingredient

import st.evening.mc.cbtweaker.util.Identifiable
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson

interface IngredientMatcherType<A, JA> : Identifiable {
    context(_: JsonPath)
    fun loadMatcher(dto: TJson.Object): IngredientMatcher<A, JA>
}
