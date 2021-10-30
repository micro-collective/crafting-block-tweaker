package xyz.phanta.cbtweaker.util;

import java.util.Locale;

public enum ConsumeBehaviour {

    CONSUME, DAMAGE, KEEP;

    public static ConsumeBehaviour fromString(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Unknown consumption behaviour: " + name);
        }
    }

}
