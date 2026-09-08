package st.evening.mc.cbtweaker.compat

import st.evening.kt.invokecontrol.ICConstant
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.compat.mekanism.MekanismCompat
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.util.game.RequireMod
import st.evening.mc.prelude.api.util.game.ifModLoaded

object CbtCompat {
    fun init(reg: ModRegistrar) {
        val compatConfig = CbTweaker.config.compat
        checkCompat(MekanismCompat.MOD_ID, compatConfig.mekanism.enabled) {
            MekanismCompat.init(reg)
        }
    }
    
    private inline fun checkCompat(
        @ICConstant modId: String,
        condition: Boolean,
        action: @RequireMod("!{modId}") () -> Unit
    ) {
        if (condition) {
            ifModLoaded(modId) {
                CbTweaker.logger.info("Initializing $modId integration...")
                action()
            }
        }
    }
}
