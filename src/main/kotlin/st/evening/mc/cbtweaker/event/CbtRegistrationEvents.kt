package st.evening.mc.cbtweaker.event

import net.minecraftforge.fml.common.eventhandler.GenericEvent
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcherType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientProviderType

class CbtRegistrationEvent<T>(type: Class<T>, private val handleRegister: (T) -> Unit) : GenericEvent<T>(type) {
    companion object {
        inline operator fun <reified T> invoke(noinline handleRegister: (T) -> Unit): CbtRegistrationEvent<T> =
            CbtRegistrationEvent(T::class.java, handleRegister)
    }

    fun register(obj: T) {
        handleRegister(obj)
    }
}

// only one type parameter is actually matched on for generic events
// but both parameters here are fixed by the buffer type anyways, so it doesn't really matter
class CbtIngredientHandlerRegistrationEvent<A, JA>(
    accClass: Class<A>,
    val bufferType: BufferType<*, A, *, JA>,
    private val handleRegisterMatcher: (IngredientMatcherType<A, JA>) -> Unit,
    private val handleRegisterProvider: (IngredientProviderType<A, JA>) -> Unit
) : GenericEvent<A>(accClass) {
    fun registerMatcherType(type: IngredientMatcherType<A, JA>) {
        handleRegisterMatcher(type)
    }

    fun registerProviderType(type: IngredientProviderType<A, JA>) {
        handleRegisterProvider(type)
    }
}
