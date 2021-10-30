package xyz.phanta.cbtweaker.buffer.ingredient;

import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

public interface IngredientProviderType<A, JA> {

    ResourceLocation getId();

    IngredientProvider<A, JA> loadProvider(JsonObject config);

}
