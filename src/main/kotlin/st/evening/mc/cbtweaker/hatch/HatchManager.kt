package st.evening.mc.cbtweaker.hatch

import net.minecraft.util.ResourceLocation
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.util.forEachFile
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import st.evening.mc.prelude.api.data.tjson.expectObject
import st.evening.mc.prelude.api.data.tjson.forEachObject
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useArrayValue
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.util.data.TypedJsonHelper
import java.nio.file.Files
import kotlin.io.path.readText

class HatchManager(private val specDir: java.nio.file.Path, private val reg: ModRegistrar) : Iterable<HatchType<*>> {
    private val hatchTypeTable: MutableMap<String, HatchType<*>> = mutableMapOf()

    operator fun get(id: String): HatchType<*>? = hatchTypeTable[id]

    override fun iterator(): Iterator<HatchType<*>> = hatchTypeTable.values.iterator()

    fun loadAll() {
        CbTweaker.logger.info("Loading hatch types...")
        try {
            specDir.forEachFile { specFile ->
                try {
                    if (!Files.isRegularFile(specFile)) return@forEachFile
                    val specFileName = specFile.fileName.toString()
                    if (!specFileName.endsWith(".tjson")) {
                        CbTweaker.logger.warn(
                            "Ignoring non-TJSON file in hatch specification directory: {}",
                            specFileName
                        )
                        return@forEachFile
                    }
                    val hatchId = specFileName.dropLast(5)
                    if (hatchId.isBlank()) {
                        throw SerializationException("Empty hatch ID!")
                    }

                    val specDto = TypedJsonParser.parseObject(specFile.readText())
                    JsonPath.atRoot {
                        val bufTypeEntry = specDto.useStringValue("type") {
                            CbTweaker.defns.bufferTypes[ResourceLocation(it)]
                                ?: throw SerializationException.withPath("Unknown buffer type: $it")
                        }
                        hatchTypeTable[hatchId] = loadHatchType(hatchId, bufTypeEntry.bufferType, specDto)
                    }
                    CbTweaker.logger.debug("Loaded hatch: {}", hatchId)
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to load hatch specification: $specFile", e)
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load hatch types!", e)
        }
        CbTweaker.logger.info("Finished loading hatch specifications.")
    }

    context(_: JsonPath)
    private fun <B> loadHatchType(
        id: String,
        bufType: BufferType<B, *, *, *>,
        specDto: TJson.Object
    ): HatchType<B> {
        val blockConfig = specDto.useObject("block") { BlockConfig.load(it) } ?: BlockConfig.DEFAULT
        val archetypeDto = specDto.expectObject("archetype")
        val tiers = specDto.useArrayValue("tiers") { tiersDto ->
            buildList {
                tiersDto.forEachObject { tierDtoRaw ->
                    val tierDto = merge(archetypeDto, tierDtoRaw)
                    add(
                        HatchType.TierData(
                            bufType.loadBufferFactory(tierDto.expectObject("buffer") ?: TJson.Object()),
                            tierDto.useAny("window") {
                                CbTweaker.defns.templates.windowTemplates.resolve(it)
                            } ?: WindowConfig.GENERIC_SMALL
                        )
                    )
                }
                if (isEmpty()) throw SerializationException.withPath("At least one tier must be defined!")
            }
        }
        return HatchType(reg, id, blockConfig, bufType, tiers)
    }

    private fun merge(archetype: TJson.Object?, instance: TJson.Object): TJson.Object =
        archetype?.let { TypedJsonHelper.merge(it, instance) } ?: instance
}
