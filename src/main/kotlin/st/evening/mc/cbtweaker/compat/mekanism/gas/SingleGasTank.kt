package st.evening.mc.cbtweaker.compat.mekanism.gas

import mekanism.api.gas.Gas
import mekanism.api.gas.GasStack
import mekanism.api.gas.GasTankInfo
import mekanism.api.gas.IGasHandler
import net.minecraft.util.EnumFacing

interface SingleGasTank : GasTankInfo, IGasHandler {
    val gasType: Gas?

    override fun getTankInfo(): Array<GasTankInfo> = arrayOf(this)

    object Empty : SingleGasTank {
        override val gasType: Gas?
            get() = null

        override fun getGas(): GasStack? = null

        override fun getStored(): Int = 0

        override fun getMaxGas(): Int = 0

        override fun receiveGas(face: EnumFacing?, gasStack: GasStack, commit: Boolean): Int = 0

        override fun drawGas(face: EnumFacing?, amount: Int, commit: Boolean): GasStack? = null

        override fun canReceiveGas(face: EnumFacing?, gas: Gas): Boolean = false

        override fun canDrawGas(face: EnumFacing?, gas: Gas): Boolean = false
    }
}
