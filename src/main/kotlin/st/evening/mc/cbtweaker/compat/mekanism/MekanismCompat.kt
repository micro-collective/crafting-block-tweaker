package st.evening.mc.cbtweaker.compat.mekanism

import mekanism.common.capabilities.Capabilities
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.compat.mekanism.buffer.MekanismGasBuffer
import st.evening.mc.cbtweaker.compat.mekanism.buffer.MekanismHeatBuffer
import st.evening.mc.cbtweaker.compat.mekanism.buffer.MekanismLaserBuffer
import st.evening.mc.cbtweaker.event.CbtIngredientHandlerRegistrationEvent
import st.evening.mc.cbtweaker.event.CbtRegistrationEvent
import st.evening.mc.cbtweaker.util.capability.CapabilityMerger
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.game.RequireMod

object MekanismCompat {
    const val MOD_ID: String = "mekanism"

    @RequireMod(MOD_ID)
    fun init(reg: ModRegistrar) {
        reg.on<CbtRegistrationEvent<BufferType<*, *, *, *>>> { event ->
            event.register(MekanismGasBuffer.Type)
            event.register(MekanismHeatBuffer.Type)
            event.register(MekanismLaserBuffer.Type)
        }
        reg.on<CbtIngredientHandlerRegistrationEvent<MekanismGasBuffer.Accumulator, MekanismGasBuffer.JeiAccumulator>> { event ->
            when (event.bufferType) {
                MekanismGasBuffer.Type -> {
                    event.registerMatcherType(MekanismGasBuffer.GasMatcher.Type)
                    event.registerMatcherType(MekanismGasBuffer.GasRateMatcher.Type)
                    event.registerProviderType(MekanismGasBuffer.GasProvider.Type)
                    event.registerProviderType(MekanismGasBuffer.GasRateProvider.Type)
                }
            }
        }
        reg.on<CbtIngredientHandlerRegistrationEvent<MekanismHeatBuffer.Accumulator, MekanismHeatBuffer.JeiAccumulator>> { event ->
            when (event.bufferType) {
                MekanismHeatBuffer.Type -> {
                    event.registerMatcherType(MekanismHeatBuffer.TemperatureMatcher.Type)
                    event.registerMatcherType(MekanismHeatBuffer.HeatMatcher.Type)
                    event.registerMatcherType(MekanismHeatBuffer.HeatRateMatcher.Type)
                    event.registerProviderType(MekanismHeatBuffer.HeatProvider.Type)
                    event.registerProviderType(MekanismHeatBuffer.HeatRateProvider.Type)
                }
            }
        }
        reg.on<CbtIngredientHandlerRegistrationEvent<MekanismLaserBuffer.Accumulator, MekanismLaserBuffer.JeiAccumulator>> { event ->
            when (event.bufferType) {
                MekanismLaserBuffer.Type -> {
                    event.registerMatcherType(MekanismLaserBuffer.EnergyMatcher.Type)
                    event.registerMatcherType(MekanismLaserBuffer.PowerMatcher.Type)
                }
            }
        }
        reg.on<FMLInitializationEvent> {
            CapabilityMerger.registerMerger(Capabilities.GAS_HANDLER_CAPABILITY) { ConcatGasHandler(it) }
            CapabilityMerger.registerMerger(Capabilities.LASER_RECEPTOR_CAPABILITY) { ConcatLaserReceptor(it) }
        }
    }
}
