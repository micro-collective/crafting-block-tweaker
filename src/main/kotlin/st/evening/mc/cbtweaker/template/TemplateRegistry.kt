package st.evening.mc.cbtweaker.template

import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import st.evening.mc.prelude.api.data.tjson.forEachValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class TemplateRegistry<T>(
    private val templateTypeName: String,
    private val templateFile: Path,
    private val resolver: context(JsonPath) (TJson) -> T
) {
    private val templateTable: MutableMap<String, T> = mutableMapOf()

    fun loadTemplates() {
        try {
            if (!Files.isRegularFile(templateFile)) return
            JsonPath.atRoot {
                TypedJsonParser.parseObject(templateFile.readText()).forEachValue { key, dto ->
                    try {
                        register(key, resolver(dto))
                    } catch (e: SerializationException) {
                        CbTweaker.logger.warn("Ignoring bad {} template: {}", templateTypeName, key, e)
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load $templateTypeName templates!", e)
        }
    }

    fun register(key: String, template: T) {
        if (templateTable.put(key, template) != null) {
            throw IllegalStateException("Duplicate $templateTypeName template: $key")
        }
    }

    context(_: JsonPath)
    fun resolve(dto: TJson): T {
        if (dto is TJson.String) {
            val value = dto.value
            if (value.isNotEmpty() && value[0] == '#') {
                val key = value.substring(1)
                return templateTable[key]
                    ?: throw SerializationException.withPath("Unknown $templateTypeName template: $key")
            }
        }
        return resolver(dto)
    }
}
