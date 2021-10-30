package xyz.phanta.cbtweaker.integration.jei.ingredient;

import xyz.phanta.cbtweaker.buffer.BufferType;

import java.util.*;

public class JeiIngredientProviderMap {

    private final Map<BufferType<?, ?, ?, ?>, List<? extends JeiIngredientProvider<?>>> providerTable = new HashMap<>();

    public <JA> void put(BufferType<?, ?, ?, JA> bufType, List<JeiIngredientProvider<JA>> providers) {
        providerTable.put(bufType, providers);
    }

    @SuppressWarnings("unchecked")
    public <JA> List<JeiIngredientProvider<JA>> get(BufferType<?, ?, ?, JA> bufType) {
        List<? extends JeiIngredientProvider<?>> providers = providerTable.get(bufType);
        return providers != null ? (List<JeiIngredientProvider<JA>>)providers : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public <JA> List<JeiIngredientProvider<JA>> getOrCreate(BufferType<?, ?, ?, JA> bufType) {
        return (List<JeiIngredientProvider<JA>>)providerTable.computeIfAbsent(bufType, k -> new ArrayList<>());
    }

    public Collection<BufferType<?, ?, ?, ?>> getBufferTypes() {
        return providerTable.keySet();
    }

    public boolean forEach(Visitor visitor) {
        for (Map.Entry<BufferType<?, ?, ?, ?>, List<? extends JeiIngredientProvider<?>>> entry
                : providerTable.entrySet()) {
            if (!visitUnchecked(visitor, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <JB, JA> boolean visitUnchecked(Visitor visitor, BufferType<?, ?, JB, JA> bufType,
                                                   List<? extends JeiIngredientProvider<?>> providers) {
        return visitor.visit(bufType, (List<JeiIngredientProvider<JA>>)providers);
    }

    public interface Visitor {

        <JB, JA> boolean visit(BufferType<?, ?, JB, JA> bufType, List<JeiIngredientProvider<JA>> providers);

    }

}
