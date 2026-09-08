package st.evening.mc.cbtweaker.util.component

import st.evening.mc.cbtweaker.buffer.AutoExportingBufferType
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace

abstract class AutoExportHandler<B>(
    private val exportBufType: AutoExportingBufferType<B, *, *, *>,
    private val buffer: B,
    initiallyExporting: Boolean
) {
    private val ticker: TickModulator = TickModulator(initiallyExporting)

    private var _autoExportState: Boolean = initiallyExporting

    protected fun setAutoExportState(exporting: Boolean): Boolean {
        if (exporting) {
            if (!_autoExportState) {
                _autoExportState = true
                ticker.interval = 1
                return true
            }
        } else if (_autoExportState) {
            _autoExportState = false
            ticker.sleep()
            return true
        }
        return false
    }

    var autoExporting: Boolean
        get() = _autoExportState
        set(value) {
            if (setAutoExportState(value)) {
                onAutoExportStateChange()
            }
        }

    protected abstract fun getFrontSide(): BlockSide

    protected abstract fun getEnabledFaces(): Set<RelativeFace>

    protected abstract fun onAutoExportStateChange()

    @ServerSide
    internal fun tick() {
        if (_autoExportState && ticker.tick()) {
            exportBufType.handleAutoExport(buffer, getFrontSide(), getEnabledFaces(), ticker)
        }
    }
}
