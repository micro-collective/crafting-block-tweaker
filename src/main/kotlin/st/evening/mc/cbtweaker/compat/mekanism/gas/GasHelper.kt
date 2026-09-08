package st.evening.mc.cbtweaker.compat.mekanism.gas

import mekanism.api.gas.Gas
import mekanism.api.gas.GasRegistry
import mekanism.api.gas.GasStack
import mekanism.api.gas.IGasHandler
import net.minecraft.util.EnumFacing
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.util.game.RequireMod

@RequireMod(MekanismCompat.MOD_ID)
fun GasStack.isEqual(o: GasStack): Boolean = gas == o.gas && amount == o.amount

@RequireMod(MekanismCompat.MOD_ID)
fun IGasHandler.drawFiltered(face: EnumFacing?, amount: Int, filter: Gas, commit: Boolean): Int {
    if (this is SingleGasTank) {
        return if (gasType == filter) (drawGas(face, amount, commit) ?: return 0).amount else 0
    }
    if (commit) {
        val test = drawGas(face, amount, false) ?: return 0
        if (test.gas == filter) {
            val drawn = drawGas(face, amount, true) ?: return 0 // hopefully we get the same gas as in the test run
            if (drawn.gas == filter) {
                return drawn.amount
            } else { // uh oh
                receiveGas(face, drawn, true) // try to put it back; best-effort only
                return 0
            }
        }
    } else {
        val drawn = drawGas(face, amount, false) ?: return 0
        if (drawn.gas == filter) {
            return drawn.amount
        }
    }
    return 0
}

@RequireMod(MekanismCompat.MOD_ID)
object GasHelper {
    context(_: JsonPath)
    fun loadGas(name: String): Gas =
        GasRegistry.getGas(name) ?: throw SerializationException.withPath("Unknown Mekanism gas: $name")
}
