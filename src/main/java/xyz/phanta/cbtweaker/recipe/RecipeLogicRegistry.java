package xyz.phanta.cbtweaker.recipe;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class RecipeLogicRegistry {

    private final Map<ResourceLocation, RecipeLogic<?, ?, ?, ?, ?>> recipeLogicTable = new HashMap<>();

    public void init() {
        CbtMod.LOGGER.info("Loading recipe logics...");
        MinecraftForge.EVENT_BUS.post(new CbtRegistrationEvent<>(RecipeLogic.class, this::register));
        CbtMod.LOGGER.info("Finished loading recipe logics.");
    }

    private void register(RecipeLogic<?, ?, ?, ?, ?> logic) {
        ResourceLocation typeId = logic.getId();
        RecipeLogic<?, ?, ?, ?, ?> clashing = recipeLogicTable.get(typeId);
        if (clashing != null) {
            throw new IllegalArgumentException(
                    String.format("Recipe logic ID clash! ID: %s, existing: %s, new: %s",
                            typeId, clashing.getClass().getCanonicalName(), logic.getClass().getCanonicalName()));
        }
        recipeLogicTable.put(typeId, logic);
        CbtMod.LOGGER.debug("Registered recipe logic {} ({})", typeId, logic.getClass().getCanonicalName());
    }

    @Nullable
    public RecipeLogic<?, ?, ?, ?, ?> lookUp(ResourceLocation id) {
        return recipeLogicTable.get(id);
    }

}
