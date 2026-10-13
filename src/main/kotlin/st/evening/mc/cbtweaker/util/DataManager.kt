package st.evening.mc.cbtweaker.util

import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

abstract class DataManager<T>(private val displayName: String, private val specDir: Path) : Collection<T> {
    private var preloadCache: Map<String, TJson.Object>? = null
    private val registry: MutableMap<String, T> = mutableMapOf()

    operator fun get(id: String): T? = registry[id]

    override val size: Int
        get() = registry.size

    override fun isEmpty(): Boolean = registry.isEmpty()

    override fun contains(element: T): Boolean = element in registry.values

    override fun containsAll(elements: Collection<T>): Boolean = registry.values.containsAll(elements)

    override fun iterator(): Iterator<T> = registry.values.iterator()

    fun preload() {
        CbTweaker.logger.info("Loading {} types...", displayName)
        try {
            val specCache = mutableMapOf<String, TJson.Object>()
            specDir.forEachFile { specFile ->
                try {
                    if (!Files.isRegularFile(specFile)) return@forEachFile
                    val specFileName = specFile.fileName.toString()
                    if (!specFileName.endsWith(".tjson")) {
                        CbTweaker.logger.warn(
                            "Ignoring non-TJSON file in {} specification directory: {}",
                            displayName, specFileName
                        )
                        return@forEachFile
                    }
                    val id = specFileName.dropLast(6)
                    if (id.isBlank()) {
                        throw SerializationException("Empty $displayName ID for specification file: $specFile")
                    }
                    val specDto = TypedJsonParser.parseObject(specFile.readText())
                    registry[id] = JsonPath.atRoot { preloadType(id, specDto) }
                    specCache[id] = specDto
                    CbTweaker.logger.debug("Loaded {} type: {}", displayName, id)
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to load $displayName specification file: $specFile", e)
                }
            }
            preloadCache = specCache
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load $displayName types!", e)
        }
        CbTweaker.logger.info("Loaded {} {} types", registry.size, displayName)
    }

    context(_: JsonPath)
    protected abstract fun preloadType(id: String, dto: TJson.Object): T

    fun load() {
        CbTweaker.logger.info("Loading {} data...", displayName)
        val specCache = preloadCache
        if (specCache != null) {
            preloadCache = null
            registry.forEach { (id, type) ->
                try {
                    JsonPath.atRoot {
                        loadType(type, specCache.getValue(id))
                    }
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to load data for $displayName: $id", e)
                }
            }
        } else {
            registry.forEach { (id, type) ->
                val specFile = specDir.resolve("$id.tjson")
                val specDto = try {
                    TypedJsonParser.parseObject(specFile.readText())
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to reload $displayName specification file: $specFile", e)
                }
                try {
                    JsonPath.atRoot {
                        loadType(type, specDto)
                    }
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to reload data for $displayName: $id", e)
                }
            }
        }
    }

    context(_: JsonPath)
    protected abstract fun loadType(type: T, dto: TJson.Object)
}
