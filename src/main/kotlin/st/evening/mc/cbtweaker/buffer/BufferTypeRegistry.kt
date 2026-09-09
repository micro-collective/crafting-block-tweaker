package st.evening.mc.cbtweaker.buffer

import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcherType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientProviderType
import st.evening.mc.cbtweaker.event.CbtIngredientHandlerRegistrationEvent
import st.evening.mc.cbtweaker.util.EventRegistry
import st.evening.mc.cbtweaker.util.Identifiable

class BufferTypeRegistry :
    EventRegistry<BufferType<*, *, *, *>, BufferTypeRegistry.Entry<*, *, *, *>>(BufferType::class.java, "buffer type") {

    override fun init() {
        super.init()
        CbTweaker.logger.info("Loading ingredient matchers...")
        entries.values.forEach {
            it.initMatcherTypes()
        }
        CbTweaker.logger.info("Loaded ingredient matchers")
    }

    override fun createEntry(obj: BufferType<*, *, *, *>): Entry<*, *, *, *> = Entry(obj)

    class Entry<B, A, JB, JA>(val bufferType: BufferType<B, A, JB, JA>) : Identifiable {
        private val matcherTypeTable: MutableMap<ResourceLocation, IngredientMatcherType<A, JA>> = mutableMapOf()
        private val providerTypeTable: MutableMap<ResourceLocation, IngredientProviderType<A, JA>> = mutableMapOf()

        override val id: ResourceLocation
            get() = bufferType.id

        internal fun initMatcherTypes() {
            MinecraftForge.EVENT_BUS.post(
                CbtIngredientHandlerRegistrationEvent(
                    bufferType.accumulatorClass,
                    bufferType,
                    ::registerMatcherType,
                    ::registerProviderType
                )
            )
        }

        private fun registerMatcherType(type: IngredientMatcherType<A, JA>) {
            val typeId = type.id
            matcherTypeTable[typeId]?.let {
                throw IllegalStateException(
                    "Ingredient matcher type ID clash! Buffer type: ${bufferType.id}, ID: $typeId, " +
                        "existing: ${it.javaClass.canonicalName}, new: ${type.javaClass.canonicalName}"
                )
            }
            matcherTypeTable[typeId] = type
            CbTweaker.logger.debug(
                "Registered matcher type {}/{} ({})",
                bufferType.id, typeId, type.javaClass.canonicalName
            )
        }

        private fun registerProviderType(type: IngredientProviderType<A, JA>) {
            val typeId = type.id
            providerTypeTable[typeId]?.let {
                throw IllegalStateException(
                    "Ingredient provider type ID clash! Buffer type: ${bufferType.id}, ID: $typeId, " +
                        "existing: ${it.javaClass.canonicalName}, new: ${type.javaClass.canonicalName}"
                )
            }
            providerTypeTable[typeId] = type
            CbTweaker.logger.debug(
                "Registered provider type {}/{} ({})",
                bufferType.id, typeId, type.javaClass.canonicalName
            )
        }

        fun getMatcherType(id: ResourceLocation): IngredientMatcherType<A, JA>? = matcherTypeTable[id]

        fun getProviderType(id: ResourceLocation): IngredientProviderType<A, JA>? = providerTypeTable[id]
    }
}
