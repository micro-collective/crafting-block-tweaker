package xyz.phanta.cbtweaker.util;

import com.google.gson.JsonObject;

import java.util.Locale;

public class NumberModifier {

    private double modMultiply = 1D, modAddMultiply = 0D, modAddFlat = 0D;

    public void addModifier(Operation op, double modValue, int multiplicity) {
        switch (op) {
            case MULTIPLY:
                modMultiply *= Math.pow(modValue, multiplicity);
                break;
            case ADD_MULTIPLY:
                modAddMultiply += modValue * multiplicity;
                break;
            case ADD_FLAT:
                modAddFlat += modValue * multiplicity;
                break;
        }
    }

    public void addModifier(Modifier mod, int multiplicity) {
        addModifier(mod.getOperation(), mod.getModifierValue(), multiplicity);
    }

    public double modify(double value) {
        return value * modMultiply * (1D + modAddMultiply) + modAddFlat;
    }

    public static class Modifier {

        public static Modifier fromJson(JsonObject spec) {
            return new Modifier(Operation.fromString(spec.get("op").getAsString()), spec.get("value").getAsDouble());
        }

        private final Operation op;
        private final double modValue;

        public Modifier(Operation op, double modValue) {
            this.op = op;
            this.modValue = modValue;
        }

        public Operation getOperation() {
            return op;
        }

        public double getModifierValue() {
            return modValue;
        }

    }

    public enum Operation {
        MULTIPLY,
        ADD_MULTIPLY,
        ADD_FLAT;

        public static Operation fromString(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ConfigException("Unknown modifier operation: " + name);
            }
        }
    }

}
