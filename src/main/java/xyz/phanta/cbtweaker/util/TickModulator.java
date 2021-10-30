package xyz.phanta.cbtweaker.util;

public class TickModulator {

    private int interval = 0;
    private int tick = 0;

    public boolean tick() {
        if (interval <= 0) {
            return false;
        } else if (interval == 1) {
            return true;
        } else if (++tick >= interval) {
            tick = 0;
            return true;
        } else {
            return false;
        }
    }

    public void sleep() {
        setInterval(0);
    }

    public void setInterval(int newInterval) {
        interval = newInterval;
        tick = 0;
    }

    public int getInterval() {
        return interval;
    }

    public void increaseInterval(int offset) {
        interval += offset;
    }

    public void increaseIntervalUntil(int offset, int upperBound) {
        interval = Math.min(interval + offset, upperBound);
    }

    public void decreaseInterval(int offset) {
        interval -= offset;
    }

    public void decreaseIntervalUntil(int offset, int lowerBound) {
        interval = Math.max(interval - offset, lowerBound);
    }

}
