package xyz.phanta.cbtweaker.recipe;

import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IngredientProviderMap {

    private final Map<BufferType<?, ?, ?, ?>, List<? extends IngredientProvider<?, ?>>> ingProviderTable
            = new HashMap<>();

    public <A, JA> void put(BufferType<?, A, ?, JA> bufType, List<IngredientProvider<A, JA>> providers) {
        ingProviderTable.put(bufType, providers);
    }

    @SuppressWarnings("unchecked")
    public <A, JA> List<IngredientProvider<A, JA>> get(BufferType<?, A, ?, JA> bufType) {
        List<? extends IngredientProvider<?, ?>> providers = ingProviderTable.get(bufType);
        return providers != null ? (List<IngredientProvider<A, JA>>)providers : Collections.emptyList();
    }

    public boolean forEach(Visitor visitor) {
        for (Map.Entry<BufferType<?, ?, ?, ?>, List<? extends IngredientProvider<?, ?>>> entry
                : ingProviderTable.entrySet()) {
            if (!visitUnchecked(visitor, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <B, A, JB, JA> boolean visitUnchecked(Visitor visitor, BufferType<B, A, JB, JA> bufType,
                                                         List<? extends IngredientProvider<?, ?>> providers) {
        return visitor.visit(bufType, (List<IngredientProvider<A, JA>>)providers);
    }

    public interface Visitor {

        <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType, List<IngredientProvider<A, JA>> providers);

    }

}
