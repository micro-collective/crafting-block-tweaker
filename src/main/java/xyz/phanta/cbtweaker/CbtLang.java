package xyz.phanta.cbtweaker;

public class CbtLang {

    private static final String KEY_TOOLTIP = CbtMod.MOD_ID + ".tooltip.";
    public static final String TOOLTIP_ENABLED = KEY_TOOLTIP + "enabled";
    public static final String TOOLTIP_DISABLED = KEY_TOOLTIP + "disabled";
    public static final String TOOLTIP_IDLE = KEY_TOOLTIP + "idle";
    public static final String TOOLTIP_TICKS = KEY_TOOLTIP + "ticks";
    public static final String TOOLTIP_BUFFER_GROUP = KEY_TOOLTIP + "buffer_group";
    public static final String TOOLTIP_VIS_TOOL = KEY_TOOLTIP + "vis_tool";
    public static final String TOOLTIP_BIND_TO_BLOCK = KEY_TOOLTIP + "bind_to_block";

    public static final String TOOLTIP_CONFIGURE_SIDES = KEY_TOOLTIP + "configure_sides";
    public static final String TOOLTIP_REDSTONE_BEHAVIOUR = KEY_TOOLTIP + "redstone_behaviour";
    public static final String TOOLTIP_AUTO_EXPORT = KEY_TOOLTIP + "auto_export";

    public static final String TOOLTIP_MB_NOT_ASSEMBLED = KEY_TOOLTIP + "multiblock_not_assembled";
    public static final String TOOLTIP_MB_ASSEMBLED = KEY_TOOLTIP + "multiblock_assembled";
    public static final String TOOLTIP_MB_VISUALIZE = KEY_TOOLTIP + "multiblock_visualize";

    public static final String TOOLTIP_TANK_INTERACT_INSERT = KEY_TOOLTIP + "tank_interact_insert";
    public static final String TOOLTIP_TANK_INTERACT_EXTRACT = KEY_TOOLTIP + "tank_interact_extract";

    public static final String TOOLTIP_VIS_CONTROLS = KEY_TOOLTIP + "vis_controls";
    public static final String TOOLTIP_VIS_HORZ_PAN = KEY_TOOLTIP + "vis_horizontal_pan";
    public static final String TOOLTIP_VIS_VERT_PAN = KEY_TOOLTIP + "vis_vertical_pan";
    public static final String TOOLTIP_VIS_ORBIT = KEY_TOOLTIP + "vis_orbit";
    public static final String TOOLTIP_VIS_ZOOM = KEY_TOOLTIP + "vis_zoom";
    public static final String TOOLTIP_VIS_CENTER = KEY_TOOLTIP + "vis_center";
    public static final String TOOLTIP_VIS_LAYER_UP = KEY_TOOLTIP + "vis_layer_up";
    public static final String TOOLTIP_VIS_LAYER_DOWN = KEY_TOOLTIP + "vis_layer_down";

    private static final String KEY_GUI = CbtMod.MOD_ID + ".gui.";
    private static final String KEY_GUI_VIS_TOOL = KEY_GUI + "vis_tool.";
    public static final String GUI_VIS_TOOL_NOT_BOUND = KEY_GUI_VIS_TOOL + "not_bound";

    private static final String KEY_JEI = CbtMod.MOD_ID + ".jei.";
    private static final String KEY_JEI_CATEGORY = KEY_JEI + "category.";
    public static final String JEI_CATEGORY_MULTIBLOCK_STRUCTURE = KEY_JEI_CATEGORY + "multiblock_structure";

    // integration

    private static final String KEY_INT = CbtMod.MOD_ID + ".integration.";
    private static final String KEY_MEKANISM = KEY_INT + "mekanism.";
    public static final String MEKANISM_LASER = KEY_MEKANISM + "laser";

}
