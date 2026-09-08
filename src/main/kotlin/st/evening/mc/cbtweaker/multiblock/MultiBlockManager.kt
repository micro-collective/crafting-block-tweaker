package st.evening.mc.cbtweaker.multiblock

import net.minecraft.util.ResourceLocation
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.util.forEachFile
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.registration.ModRegistrar
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class MultiBlockManager(private val specDir: Path, private val reg: ModRegistrar) : Iterable<MultiBlockType<*>> {
    private val mbSpecDtoCache: MutableMap<String, TJson.Object> = mutableMapOf()
    private val mbTypeTable: MutableMap<String, MultiBlockType<*>> = mutableMapOf()

    operator fun get(id: String): MultiBlockType<*>? = mbTypeTable[id]

    override fun iterator(): Iterator<MultiBlockType<*>> = mbTypeTable.values.iterator()

    fun preloadAll() {
        CbTweaker.logger.info("Loading multi-block types...")
        try {
            specDir.forEachFile { mbDir ->
                try {
                    if (!Files.isDirectory(mbDir)) return@forEachFile
                    val specFile = mbDir.resolve("multiblock.tjson")
                    if (!Files.isRegularFile(specFile)) {
                        CbTweaker.logger.warn("Ignoring multi-block directory without a multiblock.tjson: {}", mbDir)
                        return@forEachFile
                    }
                    val mbId = mbDir.fileName.toString()
                    val specDto = TypedJsonParser.parseObject(specFile.readText())
                    val mbType = JsonPath.atRoot {
                        MultiBlockType(
                            reg,
                            mbId,
                            specDto.useObject("block") { BlockConfig.load(it) } ?: BlockConfig.DEFAULT,
                            specDto.useObjectValue("behaviour") { behaviourDto ->
                                behaviourDto.useStringValue("type") {
                                    CbTweaker.defns.machineBehaviours[ResourceLocation(it)]
                                        ?: throw SerializationException.withPath("Unknown machine behaviour: $it")
                                }
                            },
                            specDto.useAny("ui") {
                                CbTweaker.defns.templates.windowTemplates.resolve(it)
                            } ?: WindowConfig.GENERIC_SMALL
                        )
                    }
                    mbSpecDtoCache[mbId] = specDto
                    mbTypeTable[mbId] = mbType
                    CbTweaker.logger.debug("Loaded multi-block: {}", mbId)
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to load multi-block specification: $mbDir", e)
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load multi-block types!", e)
        }
        CbTweaker.logger.info("Finished loading multi-block specifications.")
    }

    fun loadAll() {
        CbTweaker.logger.info("Loading multi-block data...")
        mbTypeTable.forEach { (mbId, mbType) ->
            try {
                lateInitMultiBlockType(mbType, mbSpecDtoCache.getValue(mbId))
                CbTweaker.logger.debug("Initialized multi-block: {}", mbId)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load data for multi-block: $mbId", e)
            }
        }
        mbSpecDtoCache.clear()
    }

    private fun <S> lateInitMultiBlockType(mbType: MultiBlockType<S>, specDto: TJson.Object) {
        JsonPath.atRoot {
            mbType.init(
                specDto.useObjectValue("structure") { structDto ->
                    structDto.useStringValue("type") {
                        CbTweaker.defns.structureMatchers[ResourceLocation(it)]
                            ?: throw SerializationException.withPath("Unknown structure matcher type: $it")
                    }.loadMatcher(mbType, structDto)
                },
                specDto.useObjectValue("behaviour") {
                    mbType.behaviour.loadStateFactory(mbType, it)
                }
            )
        }
    }
}
