package st.evening.mc.cbtweaker

import st.evening.mc.prelude.api.config.json.JsonConfig

class CbtConfig(config: JsonConfig.ConfigObject) {
    val builtIns: BuiltIns by config.obj(factory = ::BuiltIns)

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

    val compat: Compat by config.obj(factory = ::Compat)

    class Compat(config: JsonConfig.ConfigObject) {
        val mekanism: Mekanism by config.obj(factory = ::Mekanism)

        class Mekanism(config: JsonConfig.ConfigObject) {
            val enabled: Boolean by config.bool(
                true, "Enable Mekanism integration?",
                sync = false, restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
            )
        }
    }

    val dataGen: DataGen by config.obj(factory = ::DataGen)

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
