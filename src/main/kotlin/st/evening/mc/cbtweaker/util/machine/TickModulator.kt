package st.evening.mc.cbtweaker.util.machine

class TickModulator(interval: Int = 0) {
    var interval: Int = interval
        set(value) {
            field = value
            tick = 0
        }
    private var tick: Int = 0

    constructor(active: Boolean) : this (if (active) 1 else 0)

    fun tick(): Boolean {
        if (interval <= 0) {
            return false
        } else if (interval == 1) {
            return true
        } else if (++tick >= interval) {
            tick = 0
            return true
        } else {
            return false
        }
    }

    fun sleep() {
        interval = 0
    }

    fun increaseInterval(offset: Int) {
        interval += offset
    }

    fun increaseIntervalUntil(offset: Int, upperBound: Int) {
        interval = (interval + offset).coerceAtMost(upperBound)
    }

    fun decreaseInterval(offset: Int) {
        interval -= offset
    }

    fun decreaseIntervalUntil(offset: Int, lowerBound: Int) {
        interval = (interval - offset).coerceAtLeast(lowerBound)
    }
}
