package st.evening.mc.cbtweaker.compat.mekanism

import mekanism.api.gas.Gas
import mekanism.api.gas.GasStack
import mekanism.api.gas.GasTankInfo
import mekanism.api.gas.IGasHandler
import mekanism.api.lasers.ILaserReceptor
import net.minecraft.util.EnumFacing
import st.evening.mc.cbtweaker.compat.mekanism.gas.drawFiltered
import st.evening.mc.prelude.api.util.game.RequireMod

@RequireMod(MekanismCompat.MOD_ID)
class ConcatGasHandler(private val handlers: List<IGasHandler>) : IGasHandler {
    private val tankInfoArray: Array<GasTankInfo>

    init {
        val subArrays = handlers.map { it.tankInfo }
        val flattened = arrayOfNulls<GasTankInfo>(subArrays.sumOf { it.size })
        var i = 0
        subArrays.forEach { subArray ->
            subArray.forEach {
                flattened[i++] = it
            }
        }
        @Suppress("UNCHECKED_CAST")
        tankInfoArray = flattened as Array<GasTankInfo>
    }

    override fun getTankInfo(): Array<GasTankInfo> = tankInfoArray

    override fun receiveGas(face: EnumFacing?, gas: GasStack?, commit: Boolean): Int {
        if (gas == null || gas.amount <= 0) return 0
        val fullAmount = gas.amount
        handlers.forEach {
            gas.amount -= it.receiveGas(face, gas, commit)
            if (gas.amount <= 0) {
                gas.amount = fullAmount
                return fullAmount
            }
        }
        val transferred = fullAmount - gas.amount
        gas.amount = fullAmount
        return transferred
    }

    override fun drawGas(face: EnumFacing?, amount: Int, commit: Boolean): GasStack? {
        if (amount <= 0) return null
        var filter: Gas? = null
        var remAmount = amount
        handlers.forEach { handler ->
            if (filter == null) {
                val drained = handler.drawGas(null, remAmount, commit)
                if (drained != null && drained.amount > 0) {
                    remAmount -= drained.amount
                    if (remAmount <= 0) {
                        return drained
                    }
                    filter = drained.gas
                }
            } else {
                val drained = handler.drawFiltered(null, remAmount, filter, commit)
                if (drained > 0) {
                    remAmount -= drained
                    if (remAmount <= 0) {
                        return GasStack(filter, amount)
                    }
                }
            }
        }
        return if (filter == null || remAmount >= amount) null else GasStack(filter, amount - remAmount)
    }

    override fun canReceiveGas(face: EnumFacing?, gas: Gas): Boolean = handlers.any { it.canReceiveGas(face, gas) }

    override fun canDrawGas(face: EnumFacing?, gas: Gas): Boolean = handlers.any { it.canDrawGas(face, gas) }
}

@RequireMod(MekanismCompat.MOD_ID)
class ConcatLaserReceptor(private val receptors: List<ILaserReceptor>) : ILaserReceptor {
    override fun receiveLaserEnergy(amount: Double, face: EnumFacing?) {
        val portion = amount / receptors.size
        receptors.forEach {
            it.receiveLaserEnergy(portion, face)
        }
    }

    override fun canLasersDig(): Boolean = receptors.all { it.canLasersDig() }
}
