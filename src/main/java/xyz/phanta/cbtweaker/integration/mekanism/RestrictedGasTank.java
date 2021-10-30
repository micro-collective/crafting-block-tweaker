package xyz.phanta.cbtweaker.integration.mekanism;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public class RestrictedGasTank implements SingleGasTank {

    private final SingleGasTank delegate;
    private final boolean canInsert, canExtract;

    public RestrictedGasTank(SingleGasTank delegate, boolean canInsert, boolean canExtract) {
        this.delegate = delegate;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Nullable
    @Override
    public Gas getGasType() {
        return delegate.getGasType();
    }

    @Override
    @Nullable
    public GasStack getGas() {
        return delegate.getGas();
    }

    @Override
    public int getStored() {
        return delegate.getStored();
    }

    @Override
    public int getMaxGas() {
        return delegate.getMaxGas();
    }

    @Override
    public int receiveGas(EnumFacing face, GasStack gasStack, boolean commit) {
        if (!canInsert) {
            return 0;
        }
        return delegate.receiveGas(face, gasStack, commit);
    }

    @Nullable
    @Override
    public GasStack drawGas(EnumFacing face, int amount, boolean commit) {
        if (!canExtract) {
            return null;
        }
        return delegate.drawGas(face, amount, commit);
    }

    @Override
    public boolean canReceiveGas(EnumFacing face, Gas gas) {
        return canInsert && delegate.canReceiveGas(face, gas);
    }

    @Override
    public boolean canDrawGas(EnumFacing face, Gas gas) {
        return canExtract && delegate.canDrawGas(face, gas);
    }

}
