package st.evening.mc.cbtweaker.template

import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectIsObject
import st.evening.mc.prelude.api.data.tjson.forEachObject
import st.evening.mc.prelude.api.data.tjson.useStringValue
import java.nio.file.Path

class TemplateManager(templateDir: Path) {
    companion object {
        context(_: JsonPath)
        private fun resolveStructureBlockMatcher(dto: TJson.Object): StructureBlockMatcher =
            dto.useStringValue("type") { CbTweaker.defns.structureBlockMatchers.load(it) }.loadMatcher(dto)
    }

    val windowTemplates: TemplateRegistry<WindowConfig> = TemplateRegistry(
        "window",
        templateDir.resolve("window.tjson")
    ) { WindowConfig.load(it.expectIsObject()) }

    val structureBlockMatcherTemplates: TemplateRegistry<List<StructureBlockMatcher>> = TemplateRegistry(
        "structure block matcher",
        templateDir.resolve("structure_block_matcher.tjson")
    ) { dto ->
        if (dto is TJson.Array) {
            buildList {
                dto.forEachObject {
                    add(resolveStructureBlockMatcher(it))
                }
            }
        } else {
            listOf(resolveStructureBlockMatcher(dto.expectIsObject()))
        }
    }

    fun clear() {
        windowTemplates.clear()
        structureBlockMatcherTemplates.clear()
    }

    fun loadWindowTemplates() {
        if (CbTweaker.config.builtIns.loadBuiltInWindowTemplates) {
            windowTemplates.register("generic_small", WindowConfig.GENERIC_SMALL)
            windowTemplates.register("generic_medium", WindowConfig.GENERIC_MEDIUM)
            windowTemplates.register("generic_large", WindowConfig.GENERIC_LARGE)
        }
        windowTemplates.loadTemplates()
    }

    fun loadStructureBlockMatcherTemplates() {
        structureBlockMatcherTemplates.loadTemplates()
    }
}
