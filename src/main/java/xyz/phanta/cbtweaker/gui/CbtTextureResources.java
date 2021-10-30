package xyz.phanta.cbtweaker.gui;

import io.github.phantamanta44.libnine.util.render.TextureRegion;
import io.github.phantamanta44.libnine.util.render.TextureResource;
import xyz.phanta.cbtweaker.CbtMod;

public class CbtTextureResources {

    public static final TextureRegion ICON_ENERGY = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/icon/energy.png"), 16, 16).asRegion();

    public static final TextureRegion ITEM_SLOT = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/item_slot.png"), 18, 18).asRegion();
    public static final TextureRegion FLUID_SLOT = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/fluid_slot.png"), 18, 18).asRegion();
    public static final TextureRegion SIDE_CONFIG_ICON = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/side_config_icon.png"), 11, 11).asRegion();

    private static final TextureResource ENERGY_BAR = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/energy_bar.png"), 10, 36);
    public static final TextureRegion ENERGY_BAR_BG = ENERGY_BAR.getRegion(0, 0, 6, 36);
    public static final TextureRegion ENERGY_BAR_FG = ENERGY_BAR.getRegion(6, 0, 4, 34);

    private static final TextureResource PROGRESS_BAR = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/progress_bar.png"), 24, 34);
    public static final TextureRegion PROGRESS_BAR_BG = PROGRESS_BAR.getRegion(0, 0, 24, 17);
    public static final TextureRegion PROGRESS_BAR_FG = PROGRESS_BAR.getRegion(0, 17, 24, 17);

    private static final TextureResource INFO_DISPLAY = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/info_display.png"), 11, 22);
    public static final TextureRegion INFO_DISPLAY_OFF = INFO_DISPLAY.getRegion(0, 0, 11, 11);
    public static final TextureRegion INFO_DISPLAY_ON = INFO_DISPLAY.getRegion(0, 11, 11, 11);

    private static final TextureResource REDSTONE_BEHAVIOUR = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/redstone_behaviour.png"), 11, 33);
    public static final TextureRegion REDSTONE_BEHAVIOUR_IGNORED = REDSTONE_BEHAVIOUR.getRegion(0, 0, 11, 11);
    public static final TextureRegion REDSTONE_BEHAVIOUR_DIRECT = REDSTONE_BEHAVIOUR.getRegion(0, 11, 11, 11);
    public static final TextureRegion REDSTONE_BEHAVIOUR_INVERTED = REDSTONE_BEHAVIOUR.getRegion(0, 22, 11, 11);

    private static final TextureResource AUTO_EXPORT = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/auto_export.png"), 11, 22);
    public static final TextureRegion AUTO_EXPORT_DISABLED = AUTO_EXPORT.getRegion(0, 0, 11, 11);
    public static final TextureRegion AUTO_EXPORT_ENABLED = AUTO_EXPORT.getRegion(0, 11, 11, 11);

    private static final TextureResource SIDE_CONFIG = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/side_config.png"), 18, 36);
    public static final TextureRegion SIDE_CONFIG_EXPORT_OFF = SIDE_CONFIG.getRegion(0, 0, 6, 6);
    public static final TextureRegion SIDE_CONFIG_EXPORT_ON = SIDE_CONFIG.getRegion(0, 18, 6, 6);
    public static final TextureRegion SIDE_CONFIG_UP_OFF = SIDE_CONFIG.getRegion(6, 0, 6, 6);
    public static final TextureRegion SIDE_CONFIG_UP_ON = SIDE_CONFIG.getRegion(6, 18, 6, 6);
    public static final TextureRegion SIDE_CONFIG_LEFT_OFF = SIDE_CONFIG.getRegion(0, 6, 6, 6);
    public static final TextureRegion SIDE_CONFIG_LEFT_ON = SIDE_CONFIG.getRegion(0, 24, 6, 6);
    public static final TextureRegion SIDE_CONFIG_FRONT_OFF = SIDE_CONFIG.getRegion(6, 6, 6, 6);
    public static final TextureRegion SIDE_CONFIG_FRONT_ON = SIDE_CONFIG.getRegion(6, 24, 6, 6);
    public static final TextureRegion SIDE_CONFIG_RIGHT_OFF = SIDE_CONFIG.getRegion(12, 6, 6, 6);
    public static final TextureRegion SIDE_CONFIG_RIGHT_ON = SIDE_CONFIG.getRegion(12, 24, 6, 6);
    public static final TextureRegion SIDE_CONFIG_DOWN_OFF = SIDE_CONFIG.getRegion(6, 12, 6, 6);
    public static final TextureRegion SIDE_CONFIG_DOWN_ON = SIDE_CONFIG.getRegion(6, 30, 6, 6);
    public static final TextureRegion SIDE_CONFIG_BACK_OFF = SIDE_CONFIG.getRegion(12, 12, 6, 6);
    public static final TextureRegion SIDE_CONFIG_BACK_ON = SIDE_CONFIG.getRegion(12, 30, 6, 6);

    public static final TextureRegion GUI_GENERIC_SMALL = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/generic_small.png"), 256, 256).getRegion(0, 0, 176, 166);
    public static final TextureRegion GUI_MB_VIS = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/mb_vis.png"), 256, 256).getRegion(0, 0, 162, 110);
    public static final TextureRegion GUI_MB_VIS_BG = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/mb_vis_bg.png"), 256, 256).getRegion(0, 0, 172, 120);

    // integration

    public static final TextureRegion MEKANISM_ICON_ENERGY = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/icon/mekanism_energy.png"), 16, 16).asRegion();
    public static final TextureRegion MEKANISM_ICON_HEAT = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/icon/mekanism_heat.png"), 16, 16).asRegion();
    private static final TextureResource MEKANISM_LASER_BAR = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/mekanism_laser_bar.png"), 10, 36);
    public static final TextureRegion MEKANISM_LASER_BAR_BG = MEKANISM_LASER_BAR.getRegion(0, 0, 6, 36);
    public static final TextureRegion MEKANISM_LASER_BAR_FG = MEKANISM_LASER_BAR.getRegion(6, 0, 4, 34);
    private static final TextureResource MEKANISM_HEAT_BAR = new TextureResource(
            CbtMod.INSTANCE.newResourceLocation("textures/gui/component/mekanism_heat_bar.png"), 10, 36);
    public static final TextureRegion MEKANISM_HEAT_BAR_BG = MEKANISM_HEAT_BAR.getRegion(0, 0, 6, 36);
    public static final TextureRegion MEKANISM_HEAT_BAR_FG = MEKANISM_HEAT_BAR.getRegion(6, 0, 4, 34);

}
