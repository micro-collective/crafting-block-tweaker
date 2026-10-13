package st.evening.mc.cbtweaker.hatch

import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.util.DataGenHelper
import st.evening.mc.cbtweaker.util.DataManager
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectArrayValue
import st.evening.mc.prelude.api.data.tjson.expectObject
import st.evening.mc.prelude.api.data.tjson.forEachObject
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useArrayValue
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.util.data.TypedJsonHelper
import java.nio.file.Files
import java.nio.file.Path

class HatchManager(specDir: Path, private val reg: ModRegistrar) : DataManager<HatchType>("hatch", specDir) {
    context(_: JsonPath)
    override fun preloadType(id: String, dto: TJson.Object): HatchType = HatchType(
        reg,
        id,
        dto.useObject("block") { BlockConfig.load(it) } ?: BlockConfig.DEFAULT,
        dto.expectArrayValue("tiers").size
    )

    context(_: JsonPath)
    override fun loadType(type: HatchType, dto: TJson.Object) {
        loadType(type, dto.useStringValue("type") { CbTweaker.defns.bufferTypes.load(it) }.bufferType, dto)
    }

    context(_: JsonPath)
    private fun <B> loadType(type: HatchType, bufType: BufferType<B, *, *, *>, dto: TJson.Object) {
        val archetypeDto = dto.expectObject("archetype")
        type.initData(
            bufType,
            dto.useArrayValue("tiers") { tiersDto ->
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
        )
    }

    private fun merge(archetype: TJson.Object?, instance: TJson.Object): TJson.Object =
        archetype?.let { TypedJsonHelper.merge(it, instance) } ?: instance

    fun dataGenBlockModels(resourceDir: Path) {
        if (isEmpty()) return
        val cbtDir = resourceDir.resolve("cbtweaker")
        val blockStateDir = Files.createDirectories(cbtDir.resolve("blockstates"))
        val itemModelDir = cbtDir.resolve("models/item")
        forEach { hatchType ->
            val id = hatchType.id
            val bsFile = blockStateDir.resolve("hatch_$id.json")
            if (!Files.exists(bsFile)) {
                CbTweaker.logger.info("Generating hatch block state mapping: {}", bsFile.fileName)
                DataGenHelper.writeToFile(bsFile) {
                    "forge_marker" number 1
                    "variants" obj {
                        "tier" obj {
                            for (i in 0..<hatchType.fixedTierCount) {
                                i.toString() obj {
                                    "model" string "cbtweaker:hatch_${i.coerceAtMost(3)}"
                                }
                            }
                        }
                    }
                }
            }
            val hatchItemModelDir = Files.createDirectories(itemModelDir.resolve("hatch_$id"))
            for (i in 0..<hatchType.fixedTierCount) {
                val tierFile = hatchItemModelDir.resolve("hatch_${id}_$i.json")
                if (!Files.exists(tierFile)) {
                    CbTweaker.logger.info("Generating hatch item model: {}", tierFile.fileName)
                    DataGenHelper.writeToFile(tierFile) {
                        "parent" string "cbtweaker:block/hatch_${i.coerceAtMost(3)}"
                    }
                }
            }
        }
    }
}
