package st.evening.mc.cbtweaker.util.gui

import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation

class Positioned<T>(val uiPosition: UiPosition, val data: T) {
    companion object {
        context(_: JsonPath)
        inline fun <T> load(
            dto: TJson.Object,
            defaults: Positioned<T>,
            loadData: () -> T
        ): Positioned<T> = Positioned(
            dto.useAny("ui_position") { UiPosition.load(it) } ?: defaults.uiPosition,
            loadData()
        )
    }
}

open class BarDrawData(
    val bgTexture: DrawableData,
    val fgTexture: SamplableData,
    val fgOffsetX: Int,
    val fgOffsetY: Int,
    val orientation: DrawOrientation,
) {
    companion object {
        context(_: JsonPath)
        fun load(dto: TJson.Object, defaults: BarDrawData): BarDrawData = BarDrawData(
            dto.useAny("bg_texture") { DrawableData.loadSlice(it) } ?: defaults.bgTexture,
            dto.useAny("fg_texture") { DrawableData.loadSlice(it) } ?: defaults.fgTexture,
            dto.expectInt("fg_offset_x") ?: defaults.fgOffsetX,
            dto.expectInt("fg_offset_y") ?: defaults.fgOffsetY,
            dto.useString("orientation") { DrawOrientation.serializer.deserializeFromJson(it) }
                ?: defaults.orientation
        )

        context(_: JsonPath)
        fun loadPositioned(dto: TJson.Object, defaults: Positioned<BarDrawData>): Positioned<BarDrawData> =
            Positioned.load(dto, defaults) { load(dto, defaults.data) }
    }
}

open class TankDrawData(
    val bgTexture: DrawableData,
    val fgOffsetX: Int,
    val fgOffsetY: Int,
    val fgWidth: Int,
    val fgHeight: Int
) {
    companion object {
        context(_: JsonPath)
        fun load(dto: TJson.Object, defaults: TankDrawData): TankDrawData = TankDrawData(
            dto.useAny("bar_bg") {
                DrawableData.loadSliceOrBlank(
                    it,
                    defaults.fgWidth + defaults.fgOffsetX * 2,
                    defaults.fgHeight + defaults.fgOffsetY * 2
                )
            } ?: defaults.bgTexture,
            dto.expectInt("fg_offset_x") ?: defaults.fgOffsetX,
            dto.expectInt("fg_offset_y") ?: defaults.fgOffsetY,
            dto.expectInt("fg_width") ?: defaults.fgWidth,
            dto.expectInt("fg_height") ?: defaults.fgHeight,
        )

        context(_: JsonPath)
        fun loadPositioned(dto: TJson.Object, defaults: Positioned<TankDrawData>): Positioned<TankDrawData> =
            Positioned.load(dto, defaults) { load(dto, defaults.data) }
    }
}
