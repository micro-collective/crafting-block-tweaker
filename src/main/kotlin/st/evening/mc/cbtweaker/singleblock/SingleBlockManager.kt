package st.evening.mc.cbtweaker.singleblock

import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.util.DataGenHelper
import st.evening.mc.cbtweaker.util.DataManager
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.forEachObject
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.registration.ModRegistrar
import java.nio.file.Files
import java.nio.file.Path

class SingleBlockManager(specDir: Path, private val reg: ModRegistrar) :
    DataManager<SingleBlockType>("single-block", specDir) {

    context(_: JsonPath)
    override fun preloadType(id: String, dto: TJson.Object): SingleBlockType =
        SingleBlockType(reg, id, dto.useObject("block") { BlockConfig.load(it) } ?: BlockConfig.DEFAULT)

    context(_: JsonPath)
    override fun loadType(type: SingleBlockType, dto: TJson.Object) {
        loadType(
            type,
            dto.useObjectValue("behaviour") { behaviourDto ->
                behaviourDto.useStringValue("type") { CbTweaker.defns.machineBehaviours.load(it) }
            },
            dto
        )
    }

    context(_: JsonPath)
    private fun <S> loadType(type: SingleBlockType, behaviour: MachineBehaviour<S>, dto: TJson.Object) {
        type.initData(
            behaviour,
            dto.useObjectValue("behaviour") { behaviour.loadStateFactory(type, it) },
            dto.useObject("buffers") { bufGroupsDto ->
                val bufGroupFactories = Object2ObjectRBTreeMap<String, BufferGroup.Factory>()
                bufGroupsDto.forEachObject { bufGroupId, bufGroupDto ->
                    val factory = BufferGroup.Factory()
                    bufGroupDto.forEachObject { bufTypeId, buffersDto ->
                        val bufType = CbTweaker.defns.bufferTypes.load(bufTypeId).bufferType
                        buffersDto.forEachObject { name, bufferDto ->
                            factory.loadFactory(bufType, name, bufferDto)
                        }
                    }
                    bufGroupFactories[bufGroupId] = factory
                }
                return@useObject bufGroupFactories
            } ?: Object2ObjectSortedMaps.emptyMap(),
            dto.useAny("ui") { CbTweaker.defns.templates.windowTemplates.resolve(it) } ?: WindowConfig.GENERIC_SMALL
        )
    }

    fun dataGenBlockModels(resourceDir: Path) {
        if (isEmpty()) return
        val cbtDir = resourceDir.resolve("cbtweaker")
        val blockStateDir = Files.createDirectories(cbtDir.resolve("blockstates"))
        val itemModelDir = Files.createDirectories(cbtDir.resolve("models/item"))
        forEach { sbType ->
            val id = sbType.id
            val bsFile = blockStateDir.resolve("sb_$id.json")
            if (!Files.exists(bsFile)) {
                CbTweaker.logger.info("Generating single-block machine block state mapping: {}", bsFile.fileName)
                DataGenHelper.writeToFile(bsFile, DataGenHelper.machineBlockState)
            }
            val imFile = itemModelDir.resolve("sb_$id.json")
            if (!Files.exists(imFile)) {
                CbTweaker.logger.info("Generating single-block machine item model: {}", imFile.fileName)
                DataGenHelper.writeToFile(imFile, DataGenHelper.machineItemModel)
            }
        }
    }
}
