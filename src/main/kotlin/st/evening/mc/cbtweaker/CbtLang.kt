package st.evening.mc.cbtweaker

object CbtLang {
    private const val KEY_NOTIF: String = "${CbtConsts.MOD_ID}.notification"
    const val NOTIF_CONFIG_COPY: String = "${KEY_NOTIF}.config_copy"
    const val NOTIF_CONFIG_COPY_EMPTY: String = "${KEY_NOTIF}.config_copy_empty"
    const val NOTIF_CONFIG_PASTE: String = "${KEY_NOTIF}.config_paste"
    
    private const val KEY_TOOLTIP: String = "${CbtConsts.MOD_ID}.tooltip"
    const val TOOLTIP_ENABLED: String = "$KEY_TOOLTIP.enabled"
    const val TOOLTIP_DISABLED: String = "$KEY_TOOLTIP.disabled"
    const val TOOLTIP_EMPTY: String = "$KEY_TOOLTIP.empty"
    const val TOOLTIP_IDLE: String = "$KEY_TOOLTIP.idle"
    const val TOOLTIP_TICKS: String = "$KEY_TOOLTIP.ticks"
    const val TOOLTIP_SECONDS: String = "$KEY_TOOLTIP.seconds"
    const val TOOLTIP_BUFFER_GROUP: String = "$KEY_TOOLTIP.buffer_group"
    const val TOOLTIP_VIS_TOOL: String = "$KEY_TOOLTIP.vis_tool"
    const val TOOLTIP_BIND_TO_BLOCK: String = "$KEY_TOOLTIP.bind_to_block"
    const val TOOLTIP_CONFIG_COPIER: String = "$KEY_TOOLTIP.config_copier"
    const val TOOLTIP_CONFIG_COPY: String = "$KEY_TOOLTIP.config_copy"
    const val TOOLTIP_CONFIG_PASTE: String = "$KEY_TOOLTIP.config_paste"

    fun tooltipEnabledDisabled(condition: Boolean): String = if (condition) TOOLTIP_ENABLED else TOOLTIP_DISABLED

    const val TOOLTIP_CONFIGURE_IO: String = "$KEY_TOOLTIP.configure_io"
    const val TOOLTIP_REDSTONE_BEHAVIOUR: String = "$KEY_TOOLTIP.redstone_behaviour"
    const val TOOLTIP_AUTO_EXPORT: String = "$KEY_TOOLTIP.auto_export"

    const val TOOLTIP_MB_NOT_ASSEMBLED: String = "$KEY_TOOLTIP.multiblock_not_assembled"
    const val TOOLTIP_MB_ASSEMBLED: String = "$KEY_TOOLTIP.multiblock_assembled"
    const val TOOLTIP_MB_VISUALIZE: String = "$KEY_TOOLTIP.multiblock_visualize"

    const val TOOLTIP_TANK_INTERACT_INSERT: String = "$KEY_TOOLTIP.tank_interact_insert"
    const val TOOLTIP_TANK_INTERACT_EXTRACT: String = "$KEY_TOOLTIP.tank_interact_extract"
    const val TOOLTIP_EMPTY_FLUID: String = "$KEY_TOOLTIP.empty_fluid"
    const val TOOLTIP_EMPTY_GAS: String = "$KEY_TOOLTIP.empty_gas"

    const val TOOLTIP_VIS_CONTROLS: String = "$KEY_TOOLTIP.vis_controls"
    const val TOOLTIP_VIS_HORZ_PAN: String = "$KEY_TOOLTIP.vis_horizontal_pan"
    const val TOOLTIP_VIS_VERT_PAN: String = "$KEY_TOOLTIP.vis_vertical_pan"
    const val TOOLTIP_VIS_ORBIT: String = "$KEY_TOOLTIP.vis_orbit"
    const val TOOLTIP_VIS_ZOOM: String = "$KEY_TOOLTIP.vis_zoom"
    const val TOOLTIP_VIS_CENTER: String = "$KEY_TOOLTIP.vis_center"
    const val TOOLTIP_VIS_LAYER_UP: String = "$KEY_TOOLTIP.vis_layer_up"
    const val TOOLTIP_VIS_LAYER_DOWN: String = "$KEY_TOOLTIP.vis_layer_down"

    const val TOOLTIP_ING_KEEP: String = "$KEY_TOOLTIP.ingredient_keep"
    const val TOOLTIP_ING_DAMAGE: String = "$KEY_TOOLTIP.ingredient_damage"
    const val TOOLTIP_ING_CHANCE: String = "$KEY_TOOLTIP.ingredient_chance"

    private const val KEY_GUI: String = "${CbtConsts.MOD_ID}.gui"
    private const val KEY_GUI_VIS_TOOL: String = "$KEY_GUI.vis_tool"
    const val GUI_VIS_TOOL_NOT_BOUND: String = "$KEY_GUI_VIS_TOOL.not_bound"

    private const val KEY_JEI: String = "${CbtConsts.MOD_ID}.jei"
    private const val KEY_JEI_CATEGORY: String = "$KEY_JEI.category"
    const val JEI_CATEGORY_MULTIBLOCK_STRUCTURE: String = "$KEY_JEI_CATEGORY.multiblock_structure"

    // integration

    private const val KEY_INT: String = "${CbtConsts.MOD_ID}.integration"
    private const val KEY_MEKANISM: String = "$KEY_INT.mekanism"
}
