package xyz.phanta.cbtweaker.buffer.ingredient;

import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

public interface IngredientMatcherType<A, JA> {

    ResourceLocation getId();

    IngredientMatcher<A, JA> loadMatcher(JsonObject config);

}
