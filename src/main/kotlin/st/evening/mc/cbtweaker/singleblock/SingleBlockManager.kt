package st.evening.mc.cbtweaker.singleblock

import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps
import net.minecraft.util.ResourceLocation
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.util.DataGenHelper
import st.evening.mc.cbtweaker.util.forEachFile
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import st.evening.mc.prelude.api.data.tjson.forEachObject
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.registration.ModRegistrar
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class SingleBlockManager(private val specDir: Path, private val reg: ModRegistrar) : Iterable<SingleBlockType<*>> {
    private val sbSpecDtoCache: MutableMap<String, TJson.Object> = mutableMapOf()
    private val sbTypeTable: MutableMap<String, SingleBlockType<*>> = mutableMapOf()

    operator fun get(id: String): SingleBlockType<*>? = sbTypeTable[id]

    override fun iterator(): Iterator<SingleBlockType<*>> = sbTypeTable.values.iterator()

    fun preloadAll() {
        CbTweaker.logger.info("Loading single-block types...")
        try {
            specDir.forEachFile { specFile ->
                try {
                    if (!Files.isRegularFile(specFile)) return@forEachFile
                    val specFileName = specFile.fileName.toString()
                    if (!specFileName.endsWith(".tjson")) {
                        CbTweaker.logger.warn(
                            "Ignoring non-TJSON file in single-block specification directory: {}",
                            specFileName
                        )
                        return@forEachFile
                    }
                    val sbId = specFileName.dropLast(6)
                    if (sbId.isBlank()) {
                        throw SerializationException("Empty single-block ID!")
                    }

                    val specDto = TypedJsonParser.parseObject(specFile.readText())
                    val sbType = JsonPath.atRoot {
                        SingleBlockType(
                            reg,
                            sbId,
                            specDto.useObject("block") { BlockConfig.load(it) } ?: BlockConfig.DEFAULT,
                            specDto.useObject("buffers") { bufGroupsDto ->
                                val bufGroupFactories = Object2ObjectRBTreeMap<String, BufferGroup.Factory>()
                                bufGroupsDto.forEachObject { bufGroupId, bufGroupDto ->
                                    val factory = BufferGroup.Factory()
                                    bufGroupDto.forEachObject { bufTypeId, buffersDto ->
                                        val bufType = (CbTweaker.defns.bufferTypes[ResourceLocation(bufTypeId)]
                                            ?: throw SerializationException.withPath("Unknown buffer type: $bufTypeId"))
                                            .bufferType
                                        buffersDto.forEachObject { name, bufferDto ->
                                            factory.loadFactory(bufType, name, bufferDto)
                                        }
                                    }
                                    bufGroupFactories[bufGroupId] = factory
                                }
                                return@useObject bufGroupFactories
                            } ?: Object2ObjectSortedMaps.emptyMap(),
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
                    sbSpecDtoCache[sbId] = specDto
                    sbTypeTable[sbId] = sbType
                    CbTweaker.logger.debug("Loaded single-block: {}", sbId)
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to load single-block specification: $specFile", e)
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load single-block types!", e)
        }
        CbTweaker.logger.info("Finished loading single-block specifications.")
    }

    fun dataGenBlockModels(resourceDir: Path) {
        if (sbTypeTable.isEmpty()) return
        val cbtDir = resourceDir.resolve("cbtweaker")
        val blockStateDir = Files.createDirectories(cbtDir.resolve("blockstates"))
        val itemModelDir = Files.createDirectories(cbtDir.resolve("models/item"))
        sbTypeTable.keys.forEach { id ->
            val bsFile = blockStateDir.resolve("sb_$id.json")
            if (!Files.exists(bsFile)) {
                CbTweaker.logger.info("Generating single-block machine block state mapping: ${bsFile.fileName}")
                DataGenHelper.writeToFile(bsFile, DataGenHelper.machineBlockState)
            }
            val imFile = itemModelDir.resolve("sb_$id.json")
            if (!Files.exists(imFile)) {
                CbTweaker.logger.info("Generating single-block machine item model: ${imFile.fileName}")
                DataGenHelper.writeToFile(imFile, DataGenHelper.machineItemModel)
            }
        }
    }

    fun loadAll() {
        CbTweaker.logger.info("Loading single-block data...")
        sbTypeTable.forEach { (sbId, sbType) ->
            try {
                lateInitSingleBlockType(sbType, sbSpecDtoCache.getValue(sbId))
                CbTweaker.logger.debug("Initialized single-block: {}", sbId)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load data for single-block: $sbId", e)
            }
        }
        sbSpecDtoCache.clear()
    }

    private fun <S> lateInitSingleBlockType(sbType: SingleBlockType<S>, specDto: TJson.Object) {
        JsonPath.atRoot {
            sbType.init(
                specDto.useObjectValue("behaviour") {
                    sbType.behaviour.loadStateFactory(sbType, it)
                }
            )
        }
    }
}
