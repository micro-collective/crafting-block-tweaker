package xyz.phanta.cbtweaker.recipe.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.LazyConstant;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.gui.ITickTimer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.BufferTypeRegistry;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcher;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcherType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientProvider;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientProviderType;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.component.ProgressBarGuiComponent;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.integration.jei.JeiAccumulatorMap;
import xyz.phanta.cbtweaker.integration.jei.JeiBufferGroup;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredientProvider;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredientProviderMap;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.recipe.*;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.NumberModifier;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.TickModulator;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class CraftingRecipeLogic implements RecipeLogic<
        CraftingRecipeLogic.Recipe, CraftingRecipeLogic.Config, Map<String, CraftingRecipeLogic.Recipe>,
        Map<String, NumberModifier>, CraftingRecipeLogic.Execution> {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("crafting");
    public static final CraftingRecipeLogic INSTANCE = new CraftingRecipeLogic();

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void onRegisterRecipeLogics(CbtRegistrationEvent<RecipeLogic> event) {
        event.register(INSTANCE);
    }

    private CraftingRecipeLogic() {
        // NO-OP
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Config loadConfig(JsonObject config) {
        Map<String, Map<String, NumberModifier.Modifier>> mods = new HashMap<>();
        if (config.has("modifiers")) {
            JsonObject modsDto = config.getAsJsonObject("modifiers");
            for (Map.Entry<String, JsonElement> compDto : modsDto.entrySet()) {
                Map<String, NumberModifier.Modifier> byTarget = new HashMap<>();
                for (Map.Entry<String, JsonElement> targetDto : compDto.getValue().getAsJsonObject().entrySet()) {
                    byTarget.put(targetDto.getKey(),
                            NumberModifier.Modifier.fromJson(targetDto.getValue().getAsJsonObject()));
                }
                mods.put(compDto.getKey(), byTarget);
            }
        }
        return new Config(mods,
                config.has("position") ? Positioning.fromJson(config.get("position")) : Positioning.FromCenter.CENTER,
                config.has("progress_bg")
                        ? DataLoadUtils.loadTextureRegion(config.getAsJsonObject("progress_bg"))
                        : CbtTextureResources.PROGRESS_BAR_BG,
                config.has("progress_fg")
                        ? DataLoadUtils.loadTextureRegion(config.getAsJsonObject("progress_fg"))
                        : CbtTextureResources.PROGRESS_BAR_FG,
                config.has("bar_offset_x") ? config.get("bar_offset_x").getAsInt() : 0,
                config.has("bar_offset_y") ? config.get("bar_offset_y").getAsInt() : 0,
                config.has("bar_orientation")
                        ? DataLoadUtils.loadDrawOrientation(config.get("bar_orientation").getAsString())
                        : DrawOrientation.LEFT_TO_RIGHT);
    }

    @Override
    public Map<String, Recipe> createEmptyDatabase(Config config) {
        return new HashMap<>();
    }

    @Override
    public void loadRecipe(String id, JsonObject spec, Config config, Map<String, Recipe> recipeDb) {
        int duration = spec.get("duration").getAsInt();
        if (duration <= 0) {
            throw new ConfigException("Expected positive recipe duration, but got: " + duration);
        }
        Map<String, IngredientMatcherMap> inputTable = new HashMap<>();
        for (Map.Entry<String, JsonElement> bufGroupDto : spec.get("input").getAsJsonObject().entrySet()) {
            IngredientMatcherMap matchers = new IngredientMatcherMap();
            for (Map.Entry<String, JsonElement> bufTypeDto : bufGroupDto.getValue().getAsJsonObject().entrySet()) {
                ResourceLocation bufTypeId = new ResourceLocation(bufTypeDto.getKey());
                BufferTypeRegistry.Entry<?, ?, ?, ?> bufTypeEntry = CbtMod.PROXY.getBufferTypes().lookUp(bufTypeId);
                if (bufTypeEntry == null) {
                    throw new ConfigException("Unknown buffer type: " + bufTypeId);
                }
                loadMatchers(matchers, bufTypeEntry, bufTypeDto.getValue().getAsJsonArray());
            }
            inputTable.put(bufGroupDto.getKey(), matchers);
        }
        Map<String, IngredientProviderMap> outputTable = new HashMap<>();
        for (Map.Entry<String, JsonElement> bufGroupDto : spec.get("output").getAsJsonObject().entrySet()) {
            IngredientProviderMap providers = new IngredientProviderMap();
            for (Map.Entry<String, JsonElement> bufTypeDto : bufGroupDto.getValue().getAsJsonObject().entrySet()) {
                ResourceLocation bufTypeId = new ResourceLocation(bufTypeDto.getKey());
                BufferTypeRegistry.Entry<?, ?, ?, ?> bufTypeEntry = CbtMod.PROXY.getBufferTypes().lookUp(bufTypeId);
                if (bufTypeEntry == null) {
                    throw new ConfigException("Unknown buffer type: " + bufTypeId);
                }
                loadProviders(providers, bufTypeEntry, bufTypeDto.getValue().getAsJsonArray());
            }
            outputTable.put(bufGroupDto.getKey(), providers);
        }
        recipeDb.put(id, new Recipe(id, inputTable, outputTable, duration));
    }

    private static <A, JA> void loadMatchers(IngredientMatcherMap matcherMap,
                                             BufferTypeRegistry.Entry<?, A, ?, JA> bufType, JsonArray matchersDto) {
        List<IngredientMatcher<A, JA>> matchers = new ArrayList<>();
        for (JsonElement matcherDto0 : matchersDto) {
            JsonObject matcherDto = matcherDto0.getAsJsonObject();
            ResourceLocation matcherTypeId = new ResourceLocation(matcherDto.get("type").getAsString());
            IngredientMatcherType<A, JA> matcherType = bufType.lookUpMatcherType(matcherTypeId);
            if (matcherType == null) {
                throw new ConfigException(String.format("Unknown ingredient matcher type: %s/%s",
                        bufType.getBufferType().getId(), matcherTypeId));
            }
            matchers.add(matcherType.loadMatcher(matcherDto));
        }
        matcherMap.put(bufType.getBufferType(), matchers);
    }

    private static <A, JA> void loadProviders(IngredientProviderMap providerMap,
                                              BufferTypeRegistry.Entry<?, A, ?, JA> bufType, JsonArray providersDto) {
        List<IngredientProvider<A, JA>> providers = new ArrayList<>();
        for (JsonElement providerDto0 : providersDto) {
            JsonObject providerDto = providerDto0.getAsJsonObject();
            ResourceLocation providerTypeId = new ResourceLocation(providerDto.get("type").getAsString());
            IngredientProviderType<A, JA> providerType = bufType.lookUpProviderType(providerTypeId);
            if (providerType == null) {
                throw new ConfigException(String.format("Unknown ingredient provider type: %s/%s",
                        bufType.getBufferType().getId(), providerTypeId));
            }
            providers.add(providerType.loadProvider(providerDto));
        }
        providerMap.put(bufType.getBufferType(), providers);
    }

    @Override
    public Collection<Recipe> getRecipes(Config config, Map<String, Recipe> recipeDb) {
        return recipeDb.values();
    }

    @Override
    public String getRecipeId(Config config, Map<String, Recipe> recipeDb, Recipe recipe) {
        return recipe.getId();
    }

    @Nullable
    @Override
    public Recipe getRecipeById(Config config, Map<String, Recipe> recipeDb, String recipeId) {
        return recipeDb.get(recipeId);
    }

    @Override
    public Map<String, NumberModifier> computeExecutorState(Config config, RecipeExecutor<Recipe, Execution> executor,
                                                            ComponentSet components) {
        return config.computeModifiers(components);
    }

    @Nullable
    @Override
    public Recipe findRecipe(Config config, Map<String, Recipe> recipeDb,
                             RecipeExecutor<Recipe, Execution> executor, Map<String, NumberModifier> state) {
        LazyAccumulatorMap accums = new LazyAccumulatorMap.Impl(executor.getBufferGroups());
        for (Recipe recipe : recipeDb.values()) {
            if (recipe.checkExecutable(accums, state)) {
                return recipe;
            }
        }
        return null;
    }

    @Override
    public boolean doesRecipeMatch(Config config, Recipe recipe,
                                   RecipeExecutor<Recipe, Execution> executor, Map<String, NumberModifier> state) {
        return recipe.checkExecutable(new LazyAccumulatorMap.Impl(executor.getBufferGroups()), state);
    }

    @Override
    public Execution createEmptyExecution(Config config, Recipe recipe, RecipeExecutor<Recipe, Execution> executor,
                                          Map<String, NumberModifier> state) {
        return new Execution(recipe, state);
    }

    @Override
    public Execution executeRecipe(Config config, Recipe recipe, RecipeExecutor<Recipe, Execution> executor,
                                   Map<String, NumberModifier> state) {
        recipe.execute(new LazyAccumulatorMap.Impl(executor.getBufferGroups()), state);
        return new Execution(recipe, state);
    }

    @Override
    public boolean process(Config config, Recipe recipe,
                           Execution exec, RecipeExecutor<Recipe, Execution> executor,
                           Map<String, NumberModifier> state, TickModulator ticker) {
        if (recipe.processTick(new LazyAccumulatorMap.Impl(executor.getBufferGroups()), state) && exec.doWork()) {
            recipe.provideOutputs(new LazyAccumulatorMap.Impl(executor.getBufferGroups()));
            return false;
        }
        return true;
    }

    @Override
    public void refreshExecution(Config config, Recipe recipe, Execution exec,
                                 RecipeExecutor<Recipe, Execution> executor, Map<String, NumberModifier> state) {
        exec.recomputeDuration(state);
    }

    @Override
    public UiElement createUiElement(Config config, RecipeExecutor<Recipe, Execution> executor) {
        return (gui, index, contRegion) -> {
            TextureRegion bgTex = config.getProgressBarBackground();
            ScreenRegion region = config.getUiPosition().computeRegion(bgTex.getWidth(), bgTex.getHeight(), contRegion);
            gui.addComponent(new ProgressBarGuiComponent(
                    region.getX(), region.getY(), bgTex, config.getProgressBarForeground(),
                    config.getBarOffsetX(), config.getBarOffsetY(), config.getBarOrientation(),
                    () -> {
                        RecipeExecutor.Run<Recipe, Execution> run = executor.getCurrentRecipe();
                        return run != null ? run.getExecution().getProgress() : 0F;
                    }));
            return region;
        };
    }

    @Override
    public void serializeExecutionNbt(Config config, NBTTagCompound tag, Execution exec) {
        exec.serNBT(tag);
    }

    @Override
    public void serializeExecutionBytes(Config config, ByteUtils.Writer stream, Execution exec) {
        exec.serBytes(stream);
    }

    @Override
    public void deserializeExecutionNbt(Config config, NBTTagCompound tag, Execution exec) {
        exec.deserNBT(tag);
    }

    @Override
    public void deserializeExecutionBytes(Config config, ByteUtils.Reader stream, Execution exec) {
        exec.deserBytes(stream);
    }

    @Override
    public Map<String, Map<BufferType<?, ?, ?, ?>, List<JeiIngredient<?>>>> getJeiIngredients(Config config,
                                                                                              Recipe recipe) {
        return recipe.getJeiIngredients();
    }

    @Override
    public void populateJei(Config config, Recipe recipe, Map<String, JeiBufferGroup> bufGroups) {
        recipe.populateJei(new JeiAccumulatorMap(bufGroups));
    }

    @Override
    public JeiUiElement<?> createJeiUiElement(Config config, Recipe recipe, ScreenRegion contRegion,
                                              IJeiHelpers jeiHelpers) {
        TextureRegion bgTex = config.getProgressBarBackground();
        ScreenRegion region = config.getUiPosition().computeRegion(bgTex.getWidth(), bgTex.getHeight(), contRegion);
        int timerDuration = Math.max(recipe.getDuration(), 3); // fast timers hurt the eyes
        ITickTimer ticker = jeiHelpers.getGuiHelper().createTickTimer(timerDuration, timerDuration, false);
        return new JeiUiElement<Object>() {
            @Override
            public ScreenRegion getIngredientRegion() {
                return region;
            }

            @Override
            public void render(@Nullable Object ingredient) {
                int x = region.getX(), y = region.getY();
                bgTex.draw(x, y);
                config.getBarOrientation().draw(config.getProgressBarForeground(),
                        x + config.getBarOffsetX(), y + config.getBarOffsetY(),
                        ticker.getValue() / (float)ticker.getMaxValue());
            }

            @Override
            public void getTooltip(@Nullable Object ingredient, List<String> tooltip, ITooltipFlag tooltipFlags) {
                tooltip.add(I18n.format(CbtLang.TOOLTIP_TICKS, recipe.getDuration()));
            }
        };
    }

    public static class Recipe {

        private final String id;
        private final Map<String, IngredientMatcherMap> inputTable;
        private final Map<String, IngredientProviderMap> outputTable;
        private final int duration;

        private final LazyConstant<Map<String, JeiIngredientProviderMap>> jeiIngProviders;

        public Recipe(String id,
                      Map<String, IngredientMatcherMap> inputTable, Map<String, IngredientProviderMap> outputTable,
                      int duration) {
            this.id = id;
            this.inputTable = inputTable;
            this.outputTable = outputTable;
            this.duration = duration;
            this.jeiIngProviders = new LazyConstant<>(() -> {
                Map<String, JeiIngredientProviderMap> ingTable = new HashMap<>();
                for (Map.Entry<String, IngredientMatcherMap> inputEntry : inputTable.entrySet()) {
                    JeiIngredientProviderMap ingProvMap = new JeiIngredientProviderMap();
                    inputEntry.getValue().forEach(new IngredientMatcherMap.Visitor() {
                        @Override
                        public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                            List<IngredientMatcher<A, JA>> matchers) {
                            ingProvMap.put(bufType, new ArrayList<>(matchers));
                            return true;
                        }
                    });
                    ingTable.put(inputEntry.getKey(), ingProvMap);
                }
                for (Map.Entry<String, IngredientProviderMap> outputEntry : outputTable.entrySet()) {
                    JeiIngredientProviderMap ingProvMap = ingTable
                            .computeIfAbsent(outputEntry.getKey(), k -> new JeiIngredientProviderMap());
                    outputEntry.getValue().forEach(new IngredientProviderMap.Visitor() {
                        @Override
                        public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                            List<IngredientProvider<A, JA>> providers) {
                            ingProvMap.getOrCreate(bufType).addAll(providers);
                            return true;
                        }
                    });
                }
                return ingTable;
            });
        }

        public String getId() {
            return id;
        }

        public int getDuration() {
            return duration;
        }

        private boolean forEachInput(LazyAccumulatorMap accums, Map<String, NumberModifier> modderTable,
                                     InputVisitor visitor) {
            for (Map.Entry<String, IngredientMatcherMap> inputEntry : inputTable.entrySet()) {
                String groupId = inputEntry.getKey();
                LazyAccumulatorMap.Group bufGroupAccums = accums.getGroup(groupId);
                if (bufGroupAccums == null) {
                    return false;
                }
                NumberModifier consumeMod = modderTable.get("consumption:" + groupId);
                float consumeFactor = consumeMod != null ? (float)consumeMod.modify(1D) : 1F;
                if (!inputEntry.getValue().forEach(new IngredientMatcherMap.Visitor() {
                    @Override
                    public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                        List<IngredientMatcher<A, JA>> matchers) {
                        return visitor.visit(bufType, matchers, bufGroupAccums.getAccumulator(bufType), consumeFactor);
                    }
                })) {
                    return false;
                }
            }
            return true;
        }

        private boolean forEachOutput(LazyAccumulatorMap accums, OutputVisitor visitor) {
            for (Map.Entry<String, IngredientProviderMap> outputEntry : outputTable.entrySet()) {
                String groupId = outputEntry.getKey();
                LazyAccumulatorMap.Group bufGroupAccums = accums.getGroup(groupId);
                if (bufGroupAccums == null) {
                    return false;
                }
                if (!outputEntry.getValue().forEach(new IngredientProviderMap.Visitor() {
                    @Override
                    public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                        List<IngredientProvider<A, JA>> matchers) {
                        return visitor.visit(bufType, matchers, bufGroupAccums.getAccumulator(bufType));
                    }
                })) {
                    return false;
                }
            }
            return true;
        }

        public boolean checkExecutable(LazyAccumulatorMap accums, Map<String, NumberModifier> modderTable) {
            accums = accums.copyAccumulators();
            return forEachInput(accums, modderTable, new InputVisitor() {
                @Override
                public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                    List<IngredientMatcher<A, JA>> matchers, Supplier<A> acc,
                                                    float consumeFactor) {
                    for (IngredientMatcher<A, ?> matcher : matchers) {
                        if (!matcher.consumeInitial(acc, consumeFactor)) {
                            return false;
                        }
                    }
                    return true;
                }
            }) && forEachOutput(accums, new OutputVisitor() {
                @Override
                public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                    List<IngredientProvider<A, JA>> providers, Supplier<A> acc) {
                    for (IngredientProvider<A, ?> provider : providers) {
                        if (!provider.insertFinal(acc)) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        }

        public void execute(LazyAccumulatorMap accums, Map<String, NumberModifier> modderTable) {
            forEachInput(accums, modderTable, new InputVisitor() {
                @Override
                public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                    List<IngredientMatcher<A, JA>> matchers, Supplier<A> acc,
                                                    float consumeFactor) {
                    for (IngredientMatcher<A, ?> matcher : matchers) {
                        matcher.consumeInitial(acc, consumeFactor);
                    }
                    return true;
                }
            });
        }

        public boolean processTick(LazyAccumulatorMap accums, Map<String, NumberModifier> modderTable) {
            if (!forEachInput(accums.copyAccumulators(), modderTable, new InputVisitor() {
                @Override
                public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                    List<IngredientMatcher<A, JA>> matchers, Supplier<A> acc,
                                                    float consumeFactor) {
                    for (IngredientMatcher<A, ?> matcher : matchers) {
                        if (!matcher.consumePeriodic(acc, consumeFactor)) {
                            return false;
                        }
                    }
                    return true;
                }
            })) {
                return false;
            }
            forEachInput(accums, modderTable, new InputVisitor() {
                @Override
                public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                    List<IngredientMatcher<A, JA>> matchers, Supplier<A> acc,
                                                    float consumeFactor) {
                    for (IngredientMatcher<A, ?> matcher : matchers) {
                        matcher.consumePeriodic(acc, consumeFactor);
                    }
                    return true;
                }
            });
            forEachOutput(accums, new OutputVisitor() {
                @Override
                public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                    List<IngredientProvider<A, JA>> providers, Supplier<A> acc) {
                    for (IngredientProvider<A, ?> provider : providers) {
                        provider.insertPeriodic(acc);
                    }
                    return true;
                }
            });
            return true;
        }

        public void provideOutputs(LazyAccumulatorMap accums) {
            forEachOutput(accums, new OutputVisitor() {
                @Override
                public <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                                    List<IngredientProvider<A, JA>> providers, Supplier<A> acc) {
                    for (IngredientProvider<A, ?> provider : providers) {
                        provider.insertFinal(acc);
                    }
                    return true;
                }
            });
        }

        public Map<String, Map<BufferType<?, ?, ?, ?>, List<JeiIngredient<?>>>> getJeiIngredients() {
            Map<String, Map<BufferType<?, ?, ?, ?>, List<JeiIngredient<?>>>> bufGroupTable = new HashMap<>();
            for (Map.Entry<String, JeiIngredientProviderMap> bufGroupEntry : jeiIngProviders.get().entrySet()) {
                Map<BufferType<?, ?, ?, ?>, List<JeiIngredient<?>>> bufTypeTable = new HashMap<>();
                bufGroupEntry.getValue().forEach(new JeiIngredientProviderMap.Visitor() {
                    @Override
                    public <JB, JA> boolean visit(BufferType<?, ?, JB, JA> bufType,
                                                  List<JeiIngredientProvider<JA>> providers) {
                        bufTypeTable.put(bufType, providers.stream()
                                .flatMap(p -> p.getJeiIngredients().stream())
                                .collect(Collectors.toList()));
                        return true;
                    }
                });
                bufGroupTable.put(bufGroupEntry.getKey(), bufTypeTable);
            }
            return bufGroupTable;
        }

        public void populateJei(JeiAccumulatorMap accums) {
            for (Map.Entry<String, JeiIngredientProviderMap> bufGroupEntry : jeiIngProviders.get().entrySet()) {
                String groupId = bufGroupEntry.getKey();
                JeiAccumulatorMap.Group accumGroup = accums.getGroup(bufGroupEntry.getKey());
                if (accumGroup == null) {
                    CbtMod.LOGGER.warn("While populating JEI buffers for recipe {}, accumulator lacked group {}!",
                            id, groupId);
                    continue;
                }
                bufGroupEntry.getValue().forEach(new JeiIngredientProviderMap.Visitor() {
                    @Override
                    public <JB, JA> boolean visit(BufferType<?, ?, JB, JA> bufType,
                                                  List<JeiIngredientProvider<JA>> providers) {
                        JA accum = accumGroup.getAccumulator(bufType);
                        if (accum == null) {
                            CbtMod.LOGGER.warn(
                                    "While populating JEI buffers for recipe {}, group {} has no buffer of type {}!",
                                    id, groupId, bufType.getId());
                            return true;
                        }
                        for (JeiIngredientProvider<JA> provider : providers) {
                            if (!provider.populateJei(accum)) {
                                CbtMod.LOGGER.warn(
                                        "While populating JEI buffers for recipe {}, " +
                                                "group {} ran out of space for buffer type {}!",
                                        id, groupId, bufType.getId());
                            }
                        }
                        return true;
                    }
                });
            }
        }

        private interface InputVisitor {

            <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                         List<IngredientMatcher<A, JA>> matchers, Supplier<A> acc, float consumeFactor);

        }

        private interface OutputVisitor {

            <B, A, JB, JA> boolean visit(BufferType<B, A, JB, JA> bufType,
                                         List<IngredientProvider<A, JA>> providers, Supplier<A> acc);

        }

    }

    public static class Config {

        private final Map<String, Map<String, NumberModifier.Modifier>> modTable;

        private final Positioning uiPosition;
        private final TextureRegion progressBarBg, progressBarFg;
        private final int barOffsetX, barOffsetY;
        private final DrawOrientation barOrientation;

        public Config(Map<String, Map<String, NumberModifier.Modifier>> modTable,
                      Positioning uiPosition, TextureRegion progressBarBg, TextureRegion progressBarFg,
                      int barOffsetX, int barOffsetY, DrawOrientation barOrientation) {
            this.modTable = modTable;
            this.uiPosition = uiPosition;
            this.progressBarBg = progressBarBg;
            this.progressBarFg = progressBarFg;
            this.barOffsetX = barOffsetX;
            this.barOffsetY = barOffsetY;
            this.barOrientation = barOrientation;
        }

        public Positioning getUiPosition() {
            return uiPosition;
        }

        public TextureRegion getProgressBarBackground() {
            return progressBarBg;
        }

        public TextureRegion getProgressBarForeground() {
            return progressBarFg;
        }

        public Map<String, NumberModifier> computeModifiers(ComponentSet components) {
            Map<String, NumberModifier> modderTable = new HashMap<>();
            for (Map.Entry<String, Map<String, NumberModifier.Modifier>> compEntry : modTable.entrySet()) {
                int count = components.getCount(compEntry.getKey());
                if (count <= 0) {
                    continue;
                }
                for (Map.Entry<String, NumberModifier.Modifier> targetEntry : compEntry.getValue().entrySet()) {
                    modderTable.computeIfAbsent(targetEntry.getKey(), k -> new NumberModifier())
                            .addModifier(targetEntry.getValue(), count);
                }
            }
            return modderTable;
        }

        public int getBarOffsetX() {
            return barOffsetX;
        }

        public int getBarOffsetY() {
            return barOffsetY;
        }

        public DrawOrientation getBarOrientation() {
            return barOrientation;
        }

    }

    public static class Execution implements ISerializable {

        private final Recipe recipe;
        private int modifiedDuration = 0;
        private int workDone = 0;

        public Execution(Recipe recipe, Map<String, NumberModifier> modderTable) {
            this.recipe = recipe;
            recomputeDuration(modderTable);
        }

        private void recomputeDuration(Map<String, NumberModifier> modderTable) {
            NumberModifier modder = modderTable.get("duration");
            modifiedDuration = modder != null
                    ? Math.round((float)modder.modify(recipe.getDuration())) : recipe.getDuration();
        }

        public boolean doWork() {
            return ++workDone >= modifiedDuration;
        }

        public float getProgress() {
            return Math.min(workDone / (float)modifiedDuration, 1F);
        }

        @Override
        public void serBytes(ByteUtils.Writer data) {
            data.writeInt(workDone);
        }

        @Override
        public void deserBytes(ByteUtils.Reader data) {
            workDone = data.readInt();
        }

        @Override
        public void serNBT(NBTTagCompound tag) {
            tag.setInteger("WorkDone", workDone);
        }

        @Override
        public void deserNBT(NBTTagCompound tag) {
            workDone = tag.getInteger("WorkDone");
        }

    }

}
