package st.evening.mc.cbtweaker.util.capability

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.templates.FluidHandlerConcatenate
import net.minecraftforge.items.CapabilityItemHandler
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.prelude.api.component.energy.ConcatEnergyStorage
import st.evening.mc.prelude.api.component.item.ConcatItemHandler
import st.evening.mc.prelude.api.util.collection.IdentityHashStrategy
import java.util.IdentityHashMap

fun interface CapabilityMerger<T : Any> {
    fun merge(instances: List<T>): T

    companion object {
        private val mergerTable: MutableMap<Capability<*>, CapabilityMerger<*>> = IdentityHashMap()
        private val warned: MutableSet<Capability<*>> = ObjectOpenCustomHashSet(IdentityHashStrategy())

        init {
            registerMerger(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, ConcatItemHandler::wrapMaybeModifiable)
            registerMerger(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, ::FluidHandlerConcatenate)
            registerMerger(CapabilityEnergy.ENERGY, ::ConcatEnergyStorage)
        }

        fun <T : Any> registerMerger(capability: Capability<T>, merger: CapabilityMerger<T>) {
            if (capability in mergerTable) {
                CbTweaker.logger.warn(
                    "Ignoring duplicate capability merger {} for capability {} with existing merger {}",
                    merger.javaClass.canonicalName,
                    capability.name,
                    mergerTable[capability]!!.javaClass.canonicalName
                )
            } else {
                mergerTable[capability] = merger
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getMerger(capability: Capability<T>): CapabilityMerger<T>? =
            mergerTable[capability] as? CapabilityMerger<T>

        fun <T : Any> merge(capability: Capability<T>, capInstances: List<T>): T {
            val merger = getMerger(capability)
            if (merger == null) {
                if (capability !in warned) {
                    warned += capability
                    CbTweaker.logger.warn(
                        "Capability {} has no registered merger; this could cause unexpected behaviour!",
                        capability.name
                    )
                }
                return capInstances[0]
            }
            return merger.merge(capInstances)
        }
    }
}
