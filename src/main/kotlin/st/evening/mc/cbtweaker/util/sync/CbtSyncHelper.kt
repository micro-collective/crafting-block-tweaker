package st.evening.mc.cbtweaker.util.sync

import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.Piecewise

object CbtSyncHelper {
    inline fun buildSyncState(action: MutableList<Piecewise>.() -> Unit): Piecewise.Composite? {
        val stateList = mutableListOf<Piecewise>()
        stateList.action()
        return if (stateList.isEmpty()) null else ListStateComposite.fromStates(stateList)
    }
}
