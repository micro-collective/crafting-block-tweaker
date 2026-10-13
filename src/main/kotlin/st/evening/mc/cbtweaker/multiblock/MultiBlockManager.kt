package st.evening.mc.cbtweaker.multiblock

import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.util.DataGenHelper
import st.evening.mc.cbtweaker.util.DataManager
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.registration.ModRegistrar
import java.nio.file.Files
import java.nio.file.Path

class MultiBlockManager(specDir: Path, private val reg: ModRegistrar) :
    DataManager<MultiBlockType>("multi-block", specDir) {

    context(_: JsonPath)
    override fun preloadType(id: String, dto: TJson.Object): MultiBlockType =
        MultiBlockType(reg, id, dto.useObject("block") { BlockConfig.load(it) } ?: BlockConfig.DEFAULT)

    context(_: JsonPath)
    override fun loadType(type: MultiBlockType, dto: TJson.Object) {
        loadType(
            type,
            dto.useObjectValue("behaviour") { behaviourDto ->
                behaviourDto.useStringValue("type") { CbTweaker.defns.machineBehaviours.load(it) }
            },
            dto
        )
    }

    context(_: JsonPath)
    private fun <S> loadType(type: MultiBlockType, behaviour: MachineBehaviour<S>, dto: TJson.Object) {
        type.initData(
            behaviour,
            dto.useObjectValue("behaviour") { behaviour.loadStateFactory(type, it) },
            dto.useObjectValue("structure") { structDto ->
                structDto.useStringValue("type") { CbTweaker.defns.structureMatchers.load(it) }
                    .loadMatcher(type, structDto)
            },
            dto.useAny("ui") { CbTweaker.defns.templates.windowTemplates.resolve(it) } ?: WindowConfig.GENERIC_SMALL
        )
    }

    fun dataGenBlockModels(resourceDir: Path) {
        if (isEmpty()) return
        val cbtDir = resourceDir.resolve("cbtweaker")
        val blockStateDir = Files.createDirectories(cbtDir.resolve("blockstates"))
        val itemModelDir = Files.createDirectories(cbtDir.resolve("models/item"))
        forEach { mbType ->
            val id = mbType.id
            val bsFile = blockStateDir.resolve("mb_$id.json")
            if (!Files.exists(bsFile)) {
                CbTweaker.logger.info("Generating multi-block controller block state mapping: {}", bsFile.fileName)
                DataGenHelper.writeToFile(bsFile, DataGenHelper.machineBlockState)
            }
            val imFile = itemModelDir.resolve("mb_$id.json")
            if (!Files.exists(imFile)) {
                CbTweaker.logger.info("Generating multi-block controller item model: {}", imFile.fileName)
                DataGenHelper.writeToFile(imFile, DataGenHelper.machineItemModel)
            }
        }
    }
}
