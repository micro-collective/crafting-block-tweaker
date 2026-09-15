package st.evening.mc.cbtweaker.util

import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectFloat
import st.evening.mc.prelude.api.data.tjson.expectStringValue
import st.evening.mc.prelude.api.sound.LerpingPersistentSound
import st.evening.mc.prelude.api.util.collection.WeaklyValid
import st.evening.mc.prelude.api.util.game.ClientSide

class SoundData(val soundEvent: SoundEvent, val volume: Float, val pitch: Float, val attackPitch: Float) {
    companion object {
        context(_: JsonPath)
        fun load(dto: TJson, defaultAttackPitch: Float): SoundData = when (dto) {
            is TJson.String -> SoundData(SoundEvent(ResourceLocation(dto.value)), 1F, 1F, 0F)

            is TJson.Object -> SoundData(
                SoundEvent(ResourceLocation(dto.expectStringValue("sound"))),
                dto.expectFloat("volume") ?: 1F,
                dto.expectFloat("pitch") ?: 1F,
                dto.expectFloat("attack_pitch") ?: defaultAttackPitch
            )

            else -> throw SerializationException.withPath("Expected a sound name or a sound config object!")
        }
    }
}

@ClientSide.Physical
class MachineSoundWrapper(host: WeaklyValid, pos: BlockPos, val soundData: SoundData) {
    private val sound: LerpingPersistentSound =
        LerpingPersistentSound(host, pos, soundData.soundEvent, SoundCategory.BLOCKS)

    init {
        Minecraft.getMinecraft().soundHandler.playSound(sound)
    }

    fun setPosition(pos: BlockPos) {
        sound.setPosition(pos)
    }

    fun setActive(active: Boolean) {
        if (active) {
            sound.targetVolume = soundData.volume
            sound.targetPitch = soundData.pitch
        } else {
            sound.targetVolume = 0F
            sound.targetPitch = soundData.attackPitch
        }
    }
}
