package xyz.phanta.cbtweaker.integration.mekanism;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public interface SingleGasTank extends GasTankInfo, IGasHandler {

    @Nullable
    Gas getGasType();

    @Override
    default GasTankInfo[] getTankInfo() {
        return new GasTankInfo[] { this };
    }

    class Empty implements SingleGasTank {

        public static final Empty INSTANCE = new Empty();

        private Empty() {
            // NO-OP
        }

        @Nullable
        @Override
        public Gas getGasType() {
            return null;
        }

        @Nullable
        @Override
        public GasStack getGas() {
            return null;
        }

        @Override
        public int getStored() {
            return 0;
        }

        @Override
        public int getMaxGas() {
            return 0;
        }

        @Override
        public int receiveGas(EnumFacing enumFacing, GasStack gasStack, boolean b) {
            return 0;
        }

        @Nullable
        @Override
        public GasStack drawGas(EnumFacing enumFacing, int i, boolean b) {
            return null;
        }

        @Override
        public boolean canReceiveGas(EnumFacing enumFacing, Gas gas) {
            return false;
        }

        @Override
        public boolean canDrawGas(EnumFacing enumFacing, Gas gas) {
            return false;
        }

    }

}
