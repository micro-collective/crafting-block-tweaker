package st.evening.mc.cbtweaker.buffer.ingredient

import st.evening.mc.cbtweaker.util.Identifiable
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson

interface IngredientProviderType<A, JA> : Identifiable {
    context(_: JsonPath)
    fun loadProvider(dto: TJson.Object): IngredientProvider<A, JA>
}
