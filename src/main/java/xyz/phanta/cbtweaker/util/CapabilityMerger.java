package xyz.phanta.cbtweaker.util;

import io.github.phantamanta44.libnine.util.helper.EnergyUtils;
import io.github.phantamanta44.libnine.util.helper.InventoryUtils;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerConcatenate;
import net.minecraftforge.items.CapabilityItemHandler;
import xyz.phanta.cbtweaker.CbtMod;

import javax.annotation.Nullable;
import java.util.*;

@FunctionalInterface
public interface CapabilityMerger<T> {

    Registry REGISTRY = new Registry();

    T merge(List<T> instances);

    class Registry {

        private final Map<Capability<?>, CapabilityMerger<?>> mergerTable = new HashMap<>();
        private final Set<Capability<?>> warned = new HashSet<>();

        @SuppressWarnings("NullableProblems")
        private Registry() {
            registerMerger(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, InventoryUtils::join);
            registerMerger(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, FluidHandlerConcatenate::new);
            registerMerger(CapabilityEnergy.ENERGY, EnergyUtils::join);
        }

        public <T> void registerMerger(Capability<T> capability, CapabilityMerger<T> merger) {
            if (mergerTable.containsKey(capability)) {
                CbtMod.LOGGER.warn("Ignoring duplicate capability merger {} for capability {} with existing merger {}",
                        merger.getClass().getCanonicalName(),
                        capability.getName(),
                        mergerTable.get(capability).getClass().getCanonicalName());
            } else {
                mergerTable.put(capability, merger);
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public <T> CapabilityMerger<T> getMerger(Capability<T> capability) {
            return (CapabilityMerger<T>)mergerTable.get(capability);
        }

        public <T> T merge(Capability<T> capability, List<T> capInstances) {
            CapabilityMerger<T> merger = getMerger(capability);
            if (merger == null) {
                CbtMod.LOGGER.warn("Capability {} has no registered merger; this could cause unexpected behaviour!",
                        capability.getName());
                return capInstances.get(0);
            }
            return merger.merge(capInstances);
        }

    }

}
