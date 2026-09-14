package st.evening.mc.cbtweaker.util.config

import net.minecraft.nbt.NBTTagCompound
import st.evening.mc.prelude.api.util.data.NbtCompoundDsl
import st.evening.mc.prelude.api.util.data.runAction

inline fun NbtCompoundDsl.putNonEmpty(key: String, builder: (NBTTagCompound) -> Unit) {
    val subDto = NBTTagCompound()
    builder(subDto)
    if (!subDto.isEmpty) {
        key tag subDto
    }
}

inline fun NbtCompoundDsl.runNonEmpty(key: String, action: NbtCompoundDsl.() -> Unit) {
    putNonEmpty(key) { it.runAction(action) }
}

