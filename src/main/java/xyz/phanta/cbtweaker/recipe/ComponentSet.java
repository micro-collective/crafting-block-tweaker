package xyz.phanta.cbtweaker.recipe;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ComponentSet {

    public static final ComponentSet EMPTY = new ComponentSet() {
        @Override
        public void put(String componentId, int count) {
            throw new UnsupportedOperationException();
        }
    };

    private final TObjectIntMap<String> components = new TObjectIntHashMap<>(
            Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, 0);

    public void put(String componentId) {
        put(componentId, 1);
    }

    public void put(String componentId, int count) {
        if (count > 0) {
            components.adjustOrPutValue(componentId, count, count);
        }
    }

    public void addFrom(ComponentSet o) {
        o.components.forEachEntry((c, n) -> {
            put(c, n);
            return true;
        });
    }

    public int getCount(String componentId) {
        return components.get(componentId);
    }

    public ComponentSet copy() {
        ComponentSet result = new ComponentSet();
        result.addFrom(this);
        return result;
    }

}
