package st.evening.mc.cbtweaker.util.gui

import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.gui.engine.GuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.OffsetBox
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.IntArithmetic
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.math.Rect2i
import st.evening.mc.prelude.api.util.math.Vec2i
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

class UiPosition(
    val alignX: DrawAlignment,
    val alignY: DrawAlignment,
    val offsetX: Int,
    val offsetY: Int
) {
    companion object {
        val TOP_LEFT: UiPosition = UiPosition(DrawAlignment.START, DrawAlignment.START, 0, 0)
        val TOP: UiPosition = UiPosition(DrawAlignment.CENTER, DrawAlignment.START, 0, 0)
        val TOP_RIGHT: UiPosition = UiPosition(DrawAlignment.END, DrawAlignment.START, 0, 0)
        val LEFT: UiPosition = UiPosition(DrawAlignment.START, DrawAlignment.CENTER, 0, 0)
        val CENTER: UiPosition = UiPosition(DrawAlignment.CENTER, DrawAlignment.CENTER, 0, 0)
        val RIGHT: UiPosition = UiPosition(DrawAlignment.END, DrawAlignment.CENTER, 0, 0)
        val BOTTOM_LEFT: UiPosition = UiPosition(DrawAlignment.START, DrawAlignment.END, 0, 0)
        val BOTTOM: UiPosition = UiPosition(DrawAlignment.CENTER, DrawAlignment.END, 0, 0)
        val BOTTOM_RIGHT: UiPosition = UiPosition(DrawAlignment.END, DrawAlignment.END, 0, 0)

        context(_: JsonPath)
        fun load(dto: TJson): UiPosition = when (dto) {
            is TJson.String -> when (dto.value) {
                "top_left" -> TOP_LEFT
                "top" -> TOP
                "top_right" -> TOP_RIGHT
                "left" -> LEFT
                "center" -> CENTER
                "right" -> RIGHT
                "bottom_left" -> BOTTOM_LEFT
                "bottom" -> BOTTOM
                "bottom_right" -> BOTTOM_RIGHT
                else -> throw SerializationException.withPath("Unknown alignment: ${dto.value}")
            }
            is TJson.Object -> UiPosition(
                dto.useString("align_x") { DrawAlignment.serializer.deserializeFromJson(it) } ?: DrawAlignment.CENTER,
                dto.useString("align_y") { DrawAlignment.serializer.deserializeFromJson(it) } ?: DrawAlignment.CENTER,
                dto.expectInt("offset_x") ?: 0,
                dto.expectInt("offset_y") ?: 0
            )
            else -> throw SerializationException.withPath("Expected an alignment name or an object!")
        }
    }

    fun computePosition(contRegion: IntRectangle, width: Int, height: Int): Vec2i = Vec2i(
        contRegion.posX + alignX.computeOffset(IntArithmetic, width, contRegion.width) + offsetX,
        contRegion.posY + alignY.computeOffset(IntArithmetic, height, contRegion.height) + offsetY
    )

    fun computeRegion(contRegion: IntRectangle, width: Int, height: Int): Rect2i = Rect2i(
        contRegion.posX + alignX.computeOffset(IntArithmetic, width, contRegion.width) + offsetX,
        contRegion.posY + alignY.computeOffset(IntArithmetic, height, contRegion.height) + offsetY,
        width,
        height
    )

    @ClientSide.Strong
    fun placeElement(uiIndex: Int, layout: StackLayout, wrapper: UiElementWrapper, element: GuiElement) {
        if (offsetX != 0 || offsetY != 0) {
            layout.addChild(OffsetBox(wrapper.wrap(uiIndex, element), offsetX, offsetY), alignX, alignY)
        } else {
            layout.addChild(wrapper.wrap(uiIndex, element), alignX, alignY)
        }
    }
}
