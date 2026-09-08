package st.evening.mc.cbtweaker.util.gui

import net.minecraft.util.ResourceLocation
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectStringValue
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.drawable.GuiSamplable
import st.evening.mc.prelude.api.gui.drawable.prefab.DrawableBlank
import st.evening.mc.prelude.api.gui.drawable.prefab.DrawableTexture
import st.evening.mc.prelude.api.gui.drawable.sliceSized
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.absurdPhysicalSide
import st.evening.mc.prelude.api.util.game.physicalSided
import st.evening.mc.prelude.api.util.render.TextureResource

interface DrawableData {
    @ClientSide.Physical
    val drawable: GuiDrawable

    @ClientSide.Physical
    private class Client(@ClientSide.Physical override val drawable: GuiDrawable) : DrawableData

    companion object {
        fun of(factory: @ClientSide.Physical () -> GuiDrawable): DrawableData =
            physicalSided({ ServerDrawableData }, { Client(factory()) })

        context(_: JsonPath)
        fun loadSlice(dto: TJson): SamplableData {
            when (dto) {
                is TJson.String -> return SamplableData.of {
                    DrawableTexture(256, 256, TextureResource(ResourceLocation(dto.value)))
                }

                is TJson.Object -> {
                    if ("slice_width" !in dto && "slice_height" !in dto && "slice_x" !in dto && "slice_y" !in dto) {
                        return SamplableData.of {
                            DrawableTexture(
                                dto.expectInt("texture_width") ?: 256,
                                dto.expectInt("texture_height") ?: 256,
                                TextureResource(ResourceLocation(dto.expectStringValue("texture")))
                            )
                        }
                    }
                    val texWidth = dto.expectInt("texture_width") ?: 256
                    val texHeight = dto.expectInt("texture_height") ?: 256
                    return SamplableData.of {
                        DrawableTexture(
                            texWidth,
                            texHeight,
                            TextureResource(ResourceLocation(dto.expectStringValue("texture")))
                        ).sliceSized(
                            dto.expectInt("slice_width") ?: texWidth,
                            dto.expectInt("slice_height") ?: texHeight,
                            dto.expectInt("slice_x") ?: 0,
                            dto.expectInt("slice_y") ?: 0
                        )
                    }
                }

                else -> throw SerializationException.withPath("Expected a texture path or an object!")
            }
        }

        context(_: JsonPath)
        fun loadSliceOrBlank(dto: TJson, defaultWidth: Int, defaultHeight: Int): SamplableData =
            if (dto == TJson.Null) SamplableData.of { DrawableBlank(defaultWidth, defaultHeight) } else loadSlice(dto)
    }
}

interface SamplableData : DrawableData {
    @ClientSide.Physical
    override val drawable: GuiSamplable

    @ClientSide.Physical
    private class Client(@ClientSide.Physical override val drawable: GuiSamplable) : SamplableData

    companion object {
        fun of(factory: @ClientSide.Physical () -> GuiSamplable): SamplableData =
            physicalSided({ ServerDrawableData }, { Client(factory()) })
    }
}

@ServerSide.Physical
private object ServerDrawableData : SamplableData {
    @ClientSide.Physical
    override val drawable: GuiSamplable
        get() = absurdPhysicalSide()
}
