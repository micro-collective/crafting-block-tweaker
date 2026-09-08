package st.evening.mc.cbtweaker.util

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtLang
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
object CbtClientHelper {
    private const val ING_INDEX_INC_INTERVAL: Long = 1000L

    private var lastGlobalIndexUpdateClientTick: Long = -1L
    private var lastGlobalIndexUpdate: Long = -1L
    private var globalIndex: Int = 0

    fun getGlobalTimerIndex(): Int {
        val tickNow = Prelude.defns.client().clientTick
        if (tickNow == lastGlobalIndexUpdateClientTick) return globalIndex
        lastGlobalIndexUpdateClientTick = tickNow
        val now = System.currentTimeMillis()
        if (lastGlobalIndexUpdate == -1L || GuiScreen.isShiftKeyDown()) {
            lastGlobalIndexUpdate = now
            return globalIndex
        }
        val elapsedIntervals = (now - lastGlobalIndexUpdate) / ING_INDEX_INC_INTERVAL
        globalIndex += elapsedIntervals.toInt()
        lastGlobalIndexUpdate += ING_INDEX_INC_INTERVAL * elapsedIntervals
        return globalIndex // it'll take 68 years for this to overflow, so we can just assume it'll never be negative
    }

    fun <T> indexByGlobalTimer(list: List<T?>): T? = when (list.size) {
        0 -> null
        1 -> list[0]
        else -> list[getGlobalTimerIndex() % list.size]
    }

    fun getLocalizedModName(): String = I18n.format(CbTweaker.defns.creativeTab.translationKey)

    fun formatTickTime(ticks: Int): String {
        if (ticks < 20 || GuiScreen.isShiftKeyDown()) {
            return I18n.format(CbtLang.TOOLTIP_TICKS, ticks)
        }
        var time = ticks / 20
        if (time < 60) {
            return I18n.format(CbtLang.TOOLTIP_SECONDS, time)
        }
        val seconds = time % 60
        time /= 60
        if (time < 60) {
            return "%d:%02d".format(time, seconds)
        }
        val minutes = time % 60
        time /= 60
        if (time < 24) {
            return "%d:%02d:%02d".format(time, minutes, seconds)
        }
        val hours = time % 24
        return "%dd %d:%02d:%02d".format(time / 24, hours, minutes, seconds)
    }
}
