package st.evening.mc.cbtweaker.util.recipe

import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.BufferTypeRegistry
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcher
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientProvider
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.forEachArray
import st.evening.mc.prelude.api.data.tjson.forEachObject
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.util.game.ItemKey
import st.evening.mc.prelude.api.util.game.OreEntry
import java.util.function.Predicate

object IngredientLoader {
    context(_: JsonPath)
    fun loadMatcherGroups(dto: TJson.Object): Map<String, IngredientMatcherMap> =
        loadIngredientGroups(dto, ::IngredientMatcherMap) { matcherMap, dto -> loadMatcherMap(matcherMap, dto) }

    context(_: JsonPath)
    fun loadProviderGroups(dto: TJson.Object): Map<String, IngredientProviderMap> =
        loadIngredientGroups(dto, ::IngredientProviderMap) { providerMap, dto -> loadProviderMap(providerMap, dto) }

    context(_: JsonPath)
    inline fun <T> loadIngredientGroups(
        dto: TJson.Object,
        newIngredientMap: () -> T,
        loadMap: context(JsonPath) (T, TJson.Object) -> Unit
    ): Map<String, T> {
        val result = mutableMapOf<String, T>()
        dto.forEachObject { bufGroupId, bufTypesDto ->
            val ingredientMap = newIngredientMap()
            loadMap(ingredientMap, bufTypesDto)
            result[bufGroupId] = ingredientMap
        }
        return result
    }

    context(_: JsonPath)
    fun loadMatcherMap(matcherMap: IngredientMatcherMap, bufTypesDto: TJson.Object) {
        loadIngredientMap(matcherMap, bufTypesDto) { matcherMap, bufTypeEntry, matchersDto ->
            loadMatchersForType(matcherMap, bufTypeEntry, matchersDto)
        }
    }

    context(_: JsonPath)
    fun loadProviderMap(providerMap: IngredientProviderMap, bufTypesDto: TJson.Object) {
        loadIngredientMap(providerMap, bufTypesDto) { providerMap, bufTypeEntry, providersDto ->
            loadProvidersForType(providerMap, bufTypeEntry, providersDto)
        }
    }

    context(_: JsonPath)
    inline fun <T> loadIngredientMap(
        ingredientMap: T,
        bufTypesDto: TJson.Object,
        loadEntries: context(JsonPath) (T, BufferTypeRegistry.Entry<*, *, *, *>, TJson.Array) -> Unit
    ) {
        bufTypesDto.forEachArray { bufTypeId, matchersDto ->
            val bufTypeEntry = CbTweaker.defns.bufferTypes[ResourceLocation(bufTypeId)]
                ?: throw SerializationException.withPath("Unknown buffer type: $bufTypeId")
            loadEntries(ingredientMap, bufTypeEntry, matchersDto)
        }
    }

    context(_: JsonPath)
    fun <A, JA> loadMatchersForType(
        matcherMap: IngredientMatcherMap,
        bufTypeEntry: BufferTypeRegistry.Entry<*, A, *, JA>,
        matchersDto: TJson.Array
    ) {
        val matchers = mutableListOf<IngredientMatcher<A, JA>>()
        matchersDto.forEachObject { matcherDto ->
            val matcherType = matcherDto.useStringValue("type") {
                bufTypeEntry.getMatcherType(ResourceLocation(it)) ?: throw SerializationException.withPath(
                    "Unknown ingredient matcher type: ${bufTypeEntry.bufferType.id}/$it"
                )
            }
            matchers += matcherType.loadMatcher(matcherDto)
        }
        matcherMap[bufTypeEntry.bufferType] = matchers
    }

    context(_: JsonPath)
    fun <A, JA> loadProvidersForType(
        providerMap: IngredientProviderMap,
        bufTypeEntry: BufferTypeRegistry.Entry<*, A, *, JA>,
        providersDto: TJson.Array
    ) {
        val providers = mutableListOf<IngredientProvider<A, JA>>()
        providersDto.forEachObject { providerDto ->
            val providerType = providerDto.useStringValue("type") {
                bufTypeEntry.getProviderType(ResourceLocation(it)) ?: throw SerializationException.withPath(
                    "Unknown ingredient provider type: ${bufTypeEntry.bufferType.id}/$it"
                )
            }
            providers += providerType.loadProvider(providerDto)
        }
        providerMap[bufTypeEntry.bufferType] = providers
    }

    context(_: JsonPath)
    fun loadItemFilter(dto: TJson): Predicate<ItemStack> = when (dto) {
        is TJson.String -> OreEntry(dto.value)
        is TJson.Object -> ItemKey.Serializer.deserializeFromJson(dto)
        else -> throw SerializationException.withPath("Expected an ore dictionary name or an item key object!")
    }
}
