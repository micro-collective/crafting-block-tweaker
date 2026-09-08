package st.evening.mc.cbtweaker.gui.inventory

import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.util.gui.DrawableData
import st.evening.mc.cbtweaker.util.gui.SamplableData
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.sliceSized
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.Rect2i

class WindowConfig(
    val background: SamplableData,
    val machineInvRegion: Rect2i,
    val renderMachineName: Boolean,
    val playerInvRegion: Rect2i,
    val renderPlayerName: Boolean
) {
    companion object {
        val SMALL_MACHINE_REGION: Rect2i = Rect2i(7, 16, 162, 55)
        val SMALL_PLAYER_REGION: Rect2i = Rect2i(7, 83, 162, 76)
        val GENERIC_SMALL: WindowConfig =
            WindowConfig(CbtGuiData.GUI_GENERIC_SMALL, SMALL_MACHINE_REGION, true, SMALL_PLAYER_REGION, true)

        val MEDIUM_MACHINE_REGION: Rect2i = Rect2i(7, 16, 162, 100)
        val MEDIUM_PLAYER_REGION: Rect2i = Rect2i(7, 128, 162, 76)
        val GENERIC_MEDIUM: WindowConfig =
            WindowConfig(CbtGuiData.GUI_GENERIC_MEDIUM, MEDIUM_MACHINE_REGION, true, MEDIUM_PLAYER_REGION, true)

        val LARGE_MACHINE_REGION: Rect2i = Rect2i(7, 16, 162, 145)
        val LARGE_PLAYER_REGION: Rect2i = Rect2i(7, 173, 162, 76)
        val GENERIC_LARGE: WindowConfig =
            WindowConfig(CbtGuiData.GUI_GENERIC_LARGE, LARGE_MACHINE_REGION, true, LARGE_PLAYER_REGION, true)

        context(_: JsonPath)
        fun load(dto: TJson.Object): WindowConfig = WindowConfig(
            dto.useObject("background") { DrawableData.loadSlice(it) } ?: CbtGuiData.GUI_GENERIC_SMALL,
            dto.useObject("machine_region") { Rect2i.Serializer.deserializeFromJson(it) } ?: SMALL_MACHINE_REGION,
            dto.expectBool("show_machine_name") ?: true,
            dto.useObject("player_region") { Rect2i.Serializer.deserializeFromJson(it) } ?: SMALL_PLAYER_REGION,
            dto.expectBool("show_player_name") ?: true
        )
    }
}

@ClientSide.Physical
fun WindowConfig.sliceBackground(): GuiSamplable = background.drawable.sliceSized(
    machineInvRegion.width,
    machineInvRegion.height,
    machineInvRegion.posX,
    machineInvRegion.posY
)
