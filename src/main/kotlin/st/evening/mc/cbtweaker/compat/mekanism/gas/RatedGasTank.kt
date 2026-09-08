package st.evening.mc.cbtweaker.compat.mekanism.gas

import mekanism.api.gas.Gas
import mekanism.api.gas.GasStack
import mekanism.api.gas.GasTankInfo
import net.minecraft.util.EnumFacing

class RatedGasTank(
    private val delegate: SingleGasTank,
    private val inputRate: Int = -1,
    private val outputRate: Int = -1
) : SingleGasTank by delegate {
    override fun getTankInfo(): Array<GasTankInfo> = delegate.tankInfo

    override fun receiveGas(face: EnumFacing?, gasStack: GasStack?, commit: Boolean): Int = when {
        inputRate == 0 -> 0
        gasStack == null || inputRate < 0 || gasStack.amount <= inputRate -> delegate.receiveGas(face, gasStack, commit)
        else -> delegate.receiveGas(face, GasStack(gasStack.gas, inputRate), commit)
    }

    override fun drawGas(face: EnumFacing?, amount: Int, commit: Boolean): GasStack? =
        delegate.drawGas(face, if (outputRate < 0) amount else amount.coerceAtMost(outputRate), commit)

    override fun canReceiveGas(face: EnumFacing?, gas: Gas): Boolean =
        inputRate != 0 && delegate.canReceiveGas(face, gas)

    override fun canDrawGas(face: EnumFacing?, gas: Gas): Boolean = outputRate != 0 && delegate.canDrawGas(face, gas)
}
