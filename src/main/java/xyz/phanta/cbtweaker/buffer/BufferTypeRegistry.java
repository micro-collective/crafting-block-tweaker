package xyz.phanta.cbtweaker.buffer;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcherType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientProviderType;
import xyz.phanta.cbtweaker.event.CbtIngredientHandlerRegistrationEvent;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class BufferTypeRegistry {

    private final Map<ResourceLocation, Entry<?, ?, ?, ?>> bufferTypeTable = new HashMap<>();

    public void init() {
        CbtMod.LOGGER.info("Loading buffer types...");
        MinecraftForge.EVENT_BUS.post(new CbtRegistrationEvent<>(BufferType.class, this::register));
        CbtMod.LOGGER.info("Loading ingredient matcher types...");
        for (Entry<?, ?, ?, ?> entry : bufferTypeTable.values()) {
            entry.initMatcherTypes();
        }
        CbtMod.LOGGER.info("Finished loading buffer types.");
    }

    private void register(BufferType<?, ?, ?, ?> type) {
        ResourceLocation typeId = type.getId();
        Entry<?, ?, ?, ?> clashing = bufferTypeTable.get(typeId);
        if (clashing != null) {
            throw new IllegalArgumentException(String.format("Buffer type ID clash! ID: %s, existing: %s, new: %s",
                    typeId,
                    clashing.getBufferType().getClass().getCanonicalName(),
                    type.getClass().getCanonicalName()));
        }
        bufferTypeTable.put(typeId, new Entry<>(type));
        CbtMod.LOGGER.debug("Registered buffer type {} ({})", typeId, type.getClass().getCanonicalName());
    }

    @Nullable
    public Entry<?, ?, ?, ?> lookUp(ResourceLocation id) {
        return bufferTypeTable.get(id);
    }

    public static class Entry<B, A, JB, JA> {

        private final BufferType<B, A, JB, JA> bufType;
        private final Map<ResourceLocation, IngredientMatcherType<A, JA>> matcherTypeTable = new HashMap<>();
        private final Map<ResourceLocation, IngredientProviderType<A, JA>> providerTypeTable = new HashMap<>();

        public Entry(BufferType<B, A, JB, JA> bufType) {
            this.bufType = bufType;
        }

        public BufferType<B, A, JB, JA> getBufferType() {
            return bufType;
        }

        private void initMatcherTypes() {
            MinecraftForge.EVENT_BUS.post(new CbtIngredientHandlerRegistrationEvent<>(
                    bufType.getAccumulatorClass(), bufType, this::registerMatcherType, this::registerProviderType));
        }

        private void registerMatcherType(IngredientMatcherType<A, JA> type) {
            ResourceLocation typeId = type.getId();
            IngredientMatcherType<A, JA> clashing = matcherTypeTable.get(typeId);
            if (clashing != null) {
                throw new IllegalArgumentException(String.format(
                        "Ingredient matcher type ID clash! Buffer type: %s, ID: %s, existing: %s, new: %s",
                        bufType.getId(),
                        typeId,
                        clashing.getClass().getCanonicalName(),
                        type.getClass().getCanonicalName()));
            }
            matcherTypeTable.put(typeId, type);
            CbtMod.LOGGER.debug("Registered matcher type {}/{} ({})",
                    bufType.getId(), typeId, type.getClass().getCanonicalName());
        }

        private void registerProviderType(IngredientProviderType<A, JA> type) {
            ResourceLocation typeId = type.getId();
            IngredientProviderType<A, JA> clashing = providerTypeTable.get(typeId);
            if (clashing != null) {
                throw new IllegalArgumentException(String.format(
                        "Ingredient provider type ID clash! Buffer type: %s, ID: %s, existing: %s, new: %s",
                        bufType.getId(),
                        typeId,
                        clashing.getClass().getCanonicalName(),
                        type.getClass().getCanonicalName()));
            }
            providerTypeTable.put(typeId, type);
            CbtMod.LOGGER.debug("Registered provider type {}/{} ({})",
                    bufType.getId(), typeId, type.getClass().getCanonicalName());
        }

        @Nullable
        public IngredientMatcherType<A, JA> lookUpMatcherType(ResourceLocation id) {
            return matcherTypeTable.get(id);
        }

        @Nullable
        public IngredientProviderType<A, JA> lookUpProviderType(ResourceLocation id) {
            return providerTypeTable.get(id);
        }

    }

}
