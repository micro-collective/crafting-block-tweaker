package xyz.phanta.cbtweaker.recipe;

import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcher;

import java.util.*;

public class IngredientMatcherMap {

    private final Map<BufferType<?, ?, ?, ?>, List<? extends IngredientMatcher<?, ?>>> ingMatcherTable
            = new HashMap<>();

    public <A, JA> void put(BufferType<?, A, ?, JA> bufType, List<IngredientMatcher<A, JA>> matchers) {
        ingMatcherTable.put(bufType, matchers);
    }

    @SuppressWarnings("unchecked")
    public <A, JA> List<IngredientMatcher<A, JA>> get(BufferType<?, A, ?, JA> bufType) {
        List<? extends IngredientMatcher<?, ?>> matchers = ingMatcherTable.get(bufType);
        return matchers != null ? (List<IngredientMatcher<A, JA>>)matchers : Collections.emptyList();
    }

    public Collection<BufferType<?, ?, ?, ?>> getBufferTypes() {
        return ingMatcherTable.keySet();
    }

    public boolean forEach(Visitor visitor) {
        for (Map.Entry<BufferType<?, ?, ?, ?>, List<? extends IngredientMatcher<?, ?>>> entry
                : ingMatcherTable.entrySet()) {
            if (!visitUnchecked(visitor, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <B, A, JB, JA> boolean visitUnchecked(Visitor visitor, BufferType<B, A, JB, JA> bufType,
                                                         List<? extends IngredientMatcher<?, ?>> matchers) {
        return visitor.visit(bufType, (List<IngredientMatcher<A, JA>>)matchers);
    }

    public interface Visitor {

        <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType, List<IngredientMatcher<A, JA>> matchers);

    }

}
