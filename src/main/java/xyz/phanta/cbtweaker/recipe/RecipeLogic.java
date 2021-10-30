package xyz.phanta.cbtweaker.recipe;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import mezz.jei.api.IJeiHelpers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.integration.jei.JeiBufferGroup;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.util.TickModulator;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RecipeLogic<R, C, D, S, E> {

    ResourceLocation getId();

    C loadConfig(JsonObject config);

    D createEmptyDatabase(C config);

    void loadRecipe(String id, JsonObject spec, C config, D recipeDb);

    Collection<R> getRecipes(C config, D recipeDb);

    String getRecipeId(C config, D recipeDb, R recipe);

    @Nullable
    R getRecipeById(C config, D recipeDb, String recipeId);

    S computeExecutorState(C config, RecipeExecutor<R, E> executor, ComponentSet components);

    @Nullable
    R findRecipe(C config, D recipeDb, RecipeExecutor<R, E> executor, S state);

    boolean doesRecipeMatch(C config, R recipe, RecipeExecutor<R, E> executor, S state);

    E createEmptyExecution(C config, R recipe, RecipeExecutor<R, E> executor, S state);

    E executeRecipe(C config, R recipe, RecipeExecutor<R, E> executor, S state);

    boolean process(C config, R recipe, E exec, RecipeExecutor<R, E> executor, S state, TickModulator ticker);

    default void refreshExecution(C config, R recipe, E exec, RecipeExecutor<R, E> executor, S state) {
        // NO-OP
    }

    UiElement createUiElement(C config, RecipeExecutor<R, E> executor);

    void serializeExecutionNbt(C config, NBTTagCompound tag, E exec);

    void serializeExecutionBytes(C config, ByteUtils.Writer stream, E exec);

    void deserializeExecutionNbt(C config, NBTTagCompound tag, E exec);

    void deserializeExecutionBytes(C config, ByteUtils.Reader stream, E exec);

    Map<String, Map<BufferType<?, ?, ?, ?>, List<JeiIngredient<?>>>> getJeiIngredients(C config, R recipe);

    void populateJei(C config, R recipe, Map<String, JeiBufferGroup> bufGroups);

    @Nullable
    JeiUiElement<?> createJeiUiElement(C config, R recipe, ScreenRegion contRegion, IJeiHelpers jeiHelpers);

}
