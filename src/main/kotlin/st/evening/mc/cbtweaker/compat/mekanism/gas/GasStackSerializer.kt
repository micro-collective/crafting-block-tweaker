package st.evening.mc.cbtweaker.compat.mekanism.gas

import mekanism.api.gas.GasRegistry
import mekanism.api.gas.GasStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.prelude.api.data.ser.FullSerializer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TJsonDsl
import st.evening.mc.prelude.api.data.tjson.expectIntValue
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.util.data.readString
import st.evening.mc.prelude.api.util.game.RequireMod

@RequireMod(MekanismCompat.MOD_ID)
object GasStackSerializer : FullSerializer<GasStack?, NBTTagCompound, TJson.Object> {
    private const val SER_GAS: String = "gas"
    private const val SER_AMOUNT: String = "amount"

    override fun serializeToNbt(x: GasStack?): NBTTagCompound = x?.write(NBTTagCompound()) ?: NBTTagCompound()

    override fun deserializeFromNbt(dto: NBTTagCompound): GasStack? = GasStack.readFromNBT(dto)

    override fun serializeToNetwork(buf: PacketBuffer, x: GasStack?) {
        if (x == null || x.amount <= 0) {
            buf.writeVarInt(0)
        } else {
            buf.writeVarInt(x.amount)
            buf.writeString(x.gas.name)
        }
    }

    override fun deserializeFromNetwork(buf: PacketBuffer): GasStack? {
        val amount = buf.readVarInt()
        if (amount <= 0) return null
        val gas = GasRegistry.getGas(buf.readString()) ?: return null
        return GasStack(gas, amount)
    }

    override fun serializeToJson(x: GasStack?): TJson.Object = if (x == null) TJson.Object() else TJsonDsl.obj {
        SER_GAS string x.gas.name
        SER_AMOUNT int x.amount
    }

    context(_: JsonPath)
    override fun deserializeFromJson(dto: TJson.Object): GasStack? {
        if (dto.isEmpty()) return null
        val amount = dto.expectIntValue(SER_AMOUNT)
        if (amount <= 0) return null
        return GasStack(dto.useStringValue(SER_GAS) { GasHelper.loadGas(it) }, amount)
    }
}
