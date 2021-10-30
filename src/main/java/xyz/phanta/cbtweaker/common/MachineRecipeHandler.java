package xyz.phanta.cbtweaker.common;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.recipe.ComponentSet;
import xyz.phanta.cbtweaker.recipe.RecipeExecutor;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.util.RefreshState;
import xyz.phanta.cbtweaker.util.TickModulator;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MachineRecipeHandler<R, C, D, S, E> implements RecipeExecutor<R, E> {

    private final MachineRecipeHost<R, C, D, S, E> host;
    private final ComponentSet baseComponents;
    private final Map<String, BufferGroup> bufGroups;
    private final TickModulator ticker = new TickModulator();

    @Nullable
    private ActiveRecipe activeRecipe = null;

    private S execState;
    private RefreshState recipeCheckState = RefreshState.NONE;
    @Nullable
    private R cachedRecipe = null;

    public MachineRecipeHandler(MachineRecipeHost<R, C, D, S, E> host,
                                ComponentSet baseComponents, Map<String, BufferGroup> bufGroups) {
        this.host = host;
        this.baseComponents = baseComponents;
        this.bufGroups = bufGroups;
        this.execState = computeExecutorState();
        ticker.setInterval(1);
    }

    @Override
    public Map<String, BufferGroup> getBufferGroups() {
        return bufGroups;
    }

    @Nullable
    @Override
    public Run<R, E> getCurrentRecipe() {
        return activeRecipe;
    }

    public void refreshState(boolean refreshComponents) {
        if (refreshComponents) {
            execState = computeExecutorState();
            if (activeRecipe != null) {
                host.getRecipeLogic().refreshExecution(
                        host.getRecipeConfig(), activeRecipe.getRecipe(), activeRecipe.getExecution(), this, execState);
            }
        }
        recipeCheckState = RefreshState.HARD_REFRESH;
        if (activeRecipe == null) {
            ticker.setInterval(1);
        }
    }

    private S computeExecutorState() {
        ComponentSet components = baseComponents.copy();
        for (BufferGroup bufGroup : bufGroups.values()) {
            bufGroup.forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    for (B buffer : buffers) {
                        bufType.collectComponents(components, buffer);
                    }
                }
            });
        }
        return host.getRecipeLogic().computeExecutorState(host.getRecipeConfig(), this, components);
    }

    public void tick() {
        if (!ticker.tick()) {
            return;
        }
        RecipeLogic<R, C, D, S, E> recipeLogic = host.getRecipeLogic();
        C recipeConfig = host.getRecipeConfig();

        if (activeRecipe != null) {
            host.setDirty();
            if (recipeLogic.process(
                    recipeConfig, activeRecipe.getRecipe(), activeRecipe.getExecution(), this, execState, ticker)) {
                return;
            }
            activeRecipe = null;
            recipeCheckState = recipeCheckState.escalate(RefreshState.SOFT_REFRESH);
        }

        switch (recipeCheckState) {
            case NONE:
                return;
            case SOFT_REFRESH:
                recipeCheckState = RefreshState.NONE;
                break;
        }
        if (cachedRecipe != null) {
            if (recipeLogic.doesRecipeMatch(recipeConfig, cachedRecipe, this, execState)) {
                activeRecipe = new ActiveRecipe(
                        cachedRecipe, recipeLogic.executeRecipe(recipeConfig, cachedRecipe, this, execState));
                ticker.setInterval(1);
                host.setDirty();
                return;
            } else {
                cachedRecipe = null;
            }
        }

        if (recipeCheckState != RefreshState.HARD_REFRESH) {
            return;
        }
        recipeCheckState = RefreshState.NONE;
        R recipe = recipeLogic.findRecipe(recipeConfig, host.getRecipeDatabase(), this, execState);
        if (recipe != null) {
            activeRecipe = new ActiveRecipe(recipe, recipeLogic.executeRecipe(recipeConfig, recipe, this, execState));
            ticker.setInterval(1);
            cachedRecipe = recipe;
            host.setDirty();
        }

        if (activeRecipe == null) {
            ticker.sleep();
        }
    }

    public void serializeToBytes(ByteUtils.Writer data) {
        if (activeRecipe == null) {
            data.writeByte((byte)2);
        } else {
            RecipeLogic<R, C, D, S, E> recipeLogic = host.getRecipeLogic();
            C recipeConfig = host.getRecipeConfig();
            data.writeByte((byte)1);
            data.writeString(recipeLogic.getRecipeId(recipeConfig, host.getRecipeDatabase(), activeRecipe.recipe));
            recipeLogic.serializeExecutionBytes(recipeConfig, data, activeRecipe.exec);
        }
    }

    // active flag is sometimes combined with other data, so we pass it in manually
    // see MultiBlockData
    public void deserializeFromBytes(ByteUtils.Reader data, byte activeFlag) {
        if (activeFlag == 1) {
            RecipeLogic<R, C, D, S, E> recipeLogic = host.getRecipeLogic();
            C recipeConfig = host.getRecipeConfig();
            R recipe = Objects.requireNonNull(
                    recipeLogic.getRecipeById(recipeConfig, host.getRecipeDatabase(), data.readString()));
            if (activeRecipe == null || activeRecipe.recipe != recipe) {
                E exec = recipeLogic.createEmptyExecution(recipeConfig, recipe, this, execState);
                recipeLogic.deserializeExecutionBytes(recipeConfig, data, exec);
                activeRecipe = new ActiveRecipe(recipe, exec);
            } else {
                recipeLogic.deserializeExecutionBytes(recipeConfig, data, activeRecipe.exec);
            }
        } else {
            activeRecipe = null;
        }
    }

    public void serializeToNbt(NBTTagCompound tag) {
        if (activeRecipe != null) {
            RecipeLogic<R, C, D, S, E> recipeLogic = host.getRecipeLogic();
            C recipeConfig = host.getRecipeConfig();
            tag.setString("Recipe",
                    recipeLogic.getRecipeId(recipeConfig, host.getRecipeDatabase(), activeRecipe.recipe));
            NBTTagCompound execTag = new NBTTagCompound();
            recipeLogic.serializeExecutionNbt(recipeConfig, execTag, activeRecipe.exec);
            tag.setTag("Exec", execTag);
        }
    }

    public void deserializeFromNbt(NBTTagCompound tag) {
        if (tag.hasKey("Recipe", Constants.NBT.TAG_STRING)) {
            RecipeLogic<R, C, D, S, E> recipeLogic = host.getRecipeLogic();
            C recipeConfig = host.getRecipeConfig();
            R recipe = Objects.requireNonNull(
                    recipeLogic.getRecipeById(recipeConfig, host.getRecipeDatabase(), tag.getString("Recipe")));
            if (activeRecipe == null || activeRecipe.recipe != recipe) {
                E exec = recipeLogic.createEmptyExecution(recipeConfig, recipe, this, execState);
                recipeLogic.deserializeExecutionNbt(recipeConfig, tag.getCompoundTag("Exec"), exec);
                activeRecipe = new ActiveRecipe(recipe, exec);
            } else {
                recipeLogic.deserializeExecutionNbt(recipeConfig, tag.getCompoundTag("Exec"), activeRecipe.exec);
            }
        } else {
            activeRecipe = null;
        }
    }

    private class ActiveRecipe implements Run<R, E> {

        private final R recipe;
        private final E exec;

        public ActiveRecipe(R recipe, E exec) {
            this.recipe = recipe;
            this.exec = exec;
        }

        @Override
        public R getRecipe() {
            return recipe;
        }

        @Override
        public E getExecution() {
            return exec;
        }

    }

}
