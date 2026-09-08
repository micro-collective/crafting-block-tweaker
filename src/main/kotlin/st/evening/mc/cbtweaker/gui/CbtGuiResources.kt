package st.evening.mc.cbtweaker.gui

import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.prefab.Drawable9Tile
import st.evening.mc.prelude.api.gui.drawable.prefab.DrawableTexture
import st.evening.mc.prelude.api.gui.drawable.sliceSized
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.machine.RedstoneBehaviour
import st.evening.mc.prelude.api.util.render.TextureResource
import st.evening.mc.prelude.api.util.world.RelativeFace

@ClientSide.Physical
object CbtGuiResources {
    val ICON_ENERGY: GuiSamplable =
        DrawableTexture(16, 16, TextureResource(CbTweaker.resource("textures/gui/icon/energy.png")))

    val ITEM_SLOT: GuiDrawable = Drawable9Tile.fromSlices(
        DrawableTexture(18, 18, TextureResource(CbTweaker.resource("textures/gui/component/item_slot.png"))),
        1, 1, 1, 1
    )

    val FLUID_SLOT: GuiDrawable = Drawable9Tile.fromSlices(
        DrawableTexture(18, 18, TextureResource(CbTweaker.resource("textures/gui/component/fluid_slot.png"))),
        1, 1, 1, 1
    )

    val IO_CONFIG: GuiSamplable =
        DrawableTexture(11, 11, TextureResource(CbTweaker.resource("textures/gui/component/io_config.png")))

    val ENERGY_BAR: GuiSamplable =
        DrawableTexture(10, 36, TextureResource(CbTweaker.resource("textures/gui/component/energy_bar.png")))
    val ENERGY_BAR_BG: GuiSamplable = ENERGY_BAR.sliceSized(6, 36, 0, 0)
    val ENERGY_BAR_FG: GuiSamplable = ENERGY_BAR.sliceSized(4, 34, 6, 0)

    val PROGRESS_BAR: GuiSamplable =
        DrawableTexture(24, 34, TextureResource(CbTweaker.resource("textures/gui/component/progress_bar.png")))
    val PROGRESS_BAR_BG: GuiSamplable = PROGRESS_BAR.sliceSized(24, 17, 0, 0)
    val PROGRESS_BAR_FG: GuiSamplable = PROGRESS_BAR.sliceSized(24, 17, 0, 17)

    val INFO_DISPLAY: GuiSamplable =
        DrawableTexture(11, 22, TextureResource(CbTweaker.resource("textures/gui/component/info_display.png")))
    val INFO_DISPLAY_OFF: GuiSamplable = INFO_DISPLAY.sliceSized(11, 11, 0, 0)
    val INFO_DISPLAY_ON: GuiSamplable = INFO_DISPLAY.sliceSized(11, 11, 0, 11)

    fun infoDisplay(on: Boolean): GuiSamplable = if (on) INFO_DISPLAY_ON else INFO_DISPLAY_OFF

    val REDSTONE_BEHAVIOUR: GuiSamplable =
        DrawableTexture(11, 33, TextureResource(CbTweaker.resource("textures/gui/component/redstone_behaviour.png")))
    val REDSTONE_BEHAVIOUR_IGNORED: GuiSamplable = REDSTONE_BEHAVIOUR.sliceSized(11, 11, 0, 0)
    val REDSTONE_BEHAVIOUR_DIRECT: GuiSamplable = REDSTONE_BEHAVIOUR.sliceSized(11, 11, 0, 11)
    val REDSTONE_BEHAVIOUR_INVERTED: GuiSamplable = REDSTONE_BEHAVIOUR.sliceSized(11, 11, 0, 22)

    fun redstoneBehaviour(behaviour: RedstoneBehaviour): GuiSamplable = when (behaviour) {
        RedstoneBehaviour.NONE -> REDSTONE_BEHAVIOUR_IGNORED
        RedstoneBehaviour.ACTIVE_LOW -> REDSTONE_BEHAVIOUR_INVERTED
        RedstoneBehaviour.ACTIVE_HIGH -> REDSTONE_BEHAVIOUR_DIRECT
    }

    val AUTO_EXPORT: GuiSamplable =
        DrawableTexture(11, 22, TextureResource(CbTweaker.resource("textures/gui/component/auto_export.png")))
    val AUTO_EXPORT_DISABLED: GuiSamplable = AUTO_EXPORT.sliceSized(11, 11, 0, 0)
    val AUTO_EXPORT_ENABLED: GuiSamplable = AUTO_EXPORT.sliceSized(11, 11, 0, 11)

    fun autoExport(enabled: Boolean): GuiSamplable = if (enabled) AUTO_EXPORT_ENABLED else AUTO_EXPORT_DISABLED

    val SIDE_CONFIG: GuiSamplable =
        DrawableTexture(18, 36, TextureResource(CbTweaker.resource("textures/gui/component/side_config.png")))
    val SIDE_CONFIG_EXPORT_OFF: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 0, 0)
    val SIDE_CONFIG_EXPORT_ON: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 0, 18)
    val SIDE_CONFIG_UP_OFF: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 6, 0)
    val SIDE_CONFIG_UP_ON: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 6, 18)
    val SIDE_CONFIG_LEFT_OFF: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 0, 6)
    val SIDE_CONFIG_LEFT_ON: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 0, 24)
    val SIDE_CONFIG_FRONT_OFF: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 6, 6)
    val SIDE_CONFIG_FRONT_ON: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 6, 24)
    val SIDE_CONFIG_RIGHT_OFF: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 12, 6)
    val SIDE_CONFIG_RIGHT_ON: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 12, 24)
    val SIDE_CONFIG_DOWN_OFF: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 6, 12)
    val SIDE_CONFIG_DOWN_ON: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 6, 30)
    val SIDE_CONFIG_BACK_OFF: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 12, 12)
    val SIDE_CONFIG_BACK_ON: GuiSamplable = SIDE_CONFIG.sliceSized(6, 6, 12, 30)

    fun sideConfigExport(on: Boolean): GuiSamplable = if (on) SIDE_CONFIG_EXPORT_ON else SIDE_CONFIG_EXPORT_OFF

    fun sideConfig(face: RelativeFace, on: Boolean): GuiSamplable = when (face) {
        RelativeFace.FRONT -> if (on) SIDE_CONFIG_FRONT_ON else SIDE_CONFIG_FRONT_OFF
        RelativeFace.LEFT -> if (on) SIDE_CONFIG_LEFT_ON else SIDE_CONFIG_LEFT_OFF
        RelativeFace.BACK -> if (on) SIDE_CONFIG_BACK_ON else SIDE_CONFIG_BACK_OFF
        RelativeFace.RIGHT -> if (on) SIDE_CONFIG_RIGHT_ON else SIDE_CONFIG_RIGHT_OFF
        RelativeFace.UP -> if (on) SIDE_CONFIG_UP_ON else SIDE_CONFIG_UP_OFF
        RelativeFace.DOWN -> if (on) SIDE_CONFIG_DOWN_ON else SIDE_CONFIG_DOWN_OFF
    }

    val GUI_GENERIC_SMALL: GuiSamplable = DrawableTexture(
        256, 256, TextureResource(CbTweaker.resource("textures/gui/generic_small.png"))
    ).sliceSized(176, 166, 0, 0)
    val GUI_GENERIC_MEDIUM: GuiSamplable = DrawableTexture(
        256, 256, TextureResource(CbTweaker.resource("textures/gui/generic_medium.png"))
    ).sliceSized(176, 211, 0, 0)
    val GUI_GENERIC_LARGE: GuiSamplable = DrawableTexture(
        256, 256, TextureResource(CbTweaker.resource("textures/gui/generic_large.png"))
    ).sliceSized(176, 256, 0, 0)

    val GUI_MB_VIS: GuiSamplable = DrawableTexture(
        256, 256, TextureResource(CbTweaker.resource("textures/gui/mb_vis.png"))
    ).sliceSized(162, 110, 0, 0)

    val GUI_MB_VIS_BG: GuiSamplable = DrawableTexture(
        256, 256, TextureResource(CbTweaker.resource("textures/gui/mb_vis_bg.png"))
    ).sliceSized(172, 120, 0, 0)

    // MEKANISM ========================================================================================================

    val MEKANISM_ICON_ENERGY: GuiSamplable =
        DrawableTexture(16, 16, TextureResource(CbTweaker.resource("textures/gui/icon/mekanism_energy.png")))

    val MEKANISM_ICON_HEAT: GuiSamplable =
        DrawableTexture(16, 16, TextureResource(CbTweaker.resource("textures/gui/icon/mekanism_heat.png")))

    val MEKANISM_LASER_BAR: GuiSamplable =
        DrawableTexture(10, 36, TextureResource(CbTweaker.resource("textures/gui/component/mekanism_energy_bar.png")))
    val MEKANISM_ENERGY_BAR_BG: GuiSamplable = MEKANISM_LASER_BAR.sliceSized(6, 36, 0, 0)
    val MEKANISM_ENERGY_BAR_FG: GuiSamplable = MEKANISM_LASER_BAR.sliceSized(4, 34, 6, 0)

    val MEKANISM_HEAT_BAR: GuiSamplable =
        DrawableTexture(10, 36, TextureResource(CbTweaker.resource("textures/gui/component/mekanism_heat_bar.png")))
    val MEKANISM_HEAT_BAR_BG: GuiSamplable = MEKANISM_HEAT_BAR.sliceSized(6, 36, 0, 0)
    val MEKANISM_HEAT_BAR_FG: GuiSamplable = MEKANISM_HEAT_BAR.sliceSized(4, 34, 6, 0)
}
