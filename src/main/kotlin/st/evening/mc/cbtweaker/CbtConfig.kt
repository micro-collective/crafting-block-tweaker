package st.evening.mc.cbtweaker

import st.evening.mc.prelude.api.config.json.JsonConfig

class CbtConfig(config: JsonConfig.ConfigObject) {
    val builtIns: BuiltIns by config.obj("Built-in crafting block data", ::BuiltIns)

    class BuiltIns(config: JsonConfig.ConfigObject) {
        val loadBuiltInBuffers: Boolean by config.bool(
            true, "Load built-in buffer types?",
            sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )
        val loadBuiltInMachineBehaviours: Boolean by config.bool(
            true, "Load built-in machine behaviours?",
            sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )
        val loadBuiltInStructureBlockMatchers: Boolean by config.bool(
            true, "Load built-in structure block matcher types?",
            sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )
        val loadBuiltInStructureMatchers: Boolean by config.bool(
            true, "Load built-in structure matcher types?",
            sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )
        val loadBuiltInWindowTemplates: Boolean by config.bool(
            true, "Load built-in window config templates?",
            sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )
        val loadBuiltInRecipeSets: Boolean by config.bool(
            true, "Load built-in recipe sets?",
            sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )
    }

    val compat: Compat by config.obj("Inter-mod compatibility", ::Compat)

    class Compat(config: JsonConfig.ConfigObject) {
        val mekanism: Mekanism by config.obj(factory = ::Mekanism)

        class Mekanism(config: JsonConfig.ConfigObject) {
            val enabled: Boolean by config.bool(
                true, "Enable Mekanism integration?",
                sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
            )
        }
    }

    val devTools: DevTools by config.obj("Modpack developer tools", ::DevTools)

    class DevTools(config: JsonConfig.ConfigObject) {
        val reloadOnServerStart: Boolean by config.bool(
            false,
            """
            Reload crafting block data when the server starts?
            Only applies to the integrated (single-player) server, since it would be redundant on the dedicated server.
            """.trimIndent(),
            sync = false
        )
    }

    val dataGen: DataGen by config.obj("Asset and data generation", ::DataGen)

    class DataGen(config: JsonConfig.ConfigObject) {
        val resourceDir: String by config.string(
            "resources", "Subdirectory of the game directory in which generated resources should be placed.",
            sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )
        val genBlockModels: Boolean by config.bool(
            false, "Generate missing block models?",
            sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )
    }
}
