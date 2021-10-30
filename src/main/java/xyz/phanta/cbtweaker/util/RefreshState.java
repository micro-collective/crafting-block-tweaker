package xyz.phanta.cbtweaker.util;

public enum RefreshState {

    NONE, SOFT_REFRESH, HARD_REFRESH;

    public RefreshState escalate(RefreshState o) {
        return o.compareTo(this) >= 0 ? o : this;
    }

}
