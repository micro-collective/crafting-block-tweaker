package st.evening.mc.cbtweaker.serconfig

import net.minecraft.nbt.NBTTagCompound
import st.evening.mc.prelude.api.util.game.ServerSide

interface CopiableConfigHost {
    @ServerSide
    fun writeConfig(dto: NBTTagCompound)

    @ServerSide
    fun readConfig(dto: NBTTagCompound)
}
