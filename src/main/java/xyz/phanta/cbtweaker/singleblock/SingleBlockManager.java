package xyz.phanta.cbtweaker.singleblock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.math.Vec2i;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.ItemBlockRegistrar;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.BufferTypeRegistry;
import xyz.phanta.cbtweaker.common.BlockMaterial;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SingleBlockManager {

    private static final UiLayout DEFAULT_UI_LAYOUT = new UiLayout(
            CbtTextureResources.GUI_GENERIC_SMALL, new ScreenRegion(7, 16, 162, 55), true, new Vec2i(7, 83), true);

    private final Path specDir;
    private final Map<String, JsonObject> sbSpecDtoCache = new HashMap<>();
    private final Map<String, SingleBlockType<?, ?, ?>> sbTypeTable = new HashMap<>();
    private final ItemBlockRegistrar registrar;

    public SingleBlockManager(Path specDir, ItemBlockRegistrar registrar) {
        this.specDir = specDir;
        this.registrar = registrar;
    }

    public void preloadAll() {
        CbtMod.LOGGER.info("Loading single-block types...");
        try {
            Files.list(specDir).forEach(sbDir -> {
                try {
                    if (!Files.isDirectory(sbDir)) {
                        return;
                    }
                    Path specFile = sbDir.resolve("singleblock.json");
                    if (!Files.isRegularFile(specFile)) {
                        CbtMod.LOGGER.warn("Ignoring single-block directory without a singleblock.json: {}", sbDir);
                        return;
                    }

                    JsonObject specDto = DataLoadUtils.readJsonFile(specFile).getAsJsonObject();
                    BlockMaterial blockMat = specDto.has("material")
                            ? BlockMaterial.fromString(specDto.get("material").getAsString()) : BlockMaterial.METAL;
                    RecipeLogic<?, ?, ?, ?, ?> recipeLogic = DataLoadUtils.loadRecipeLogic(
                            specDto.getAsJsonObject("recipe_logic"));
                    UiLayout uiLayout = specDto.has("ui")
                            ? CbtMod.PROXY.getTemplates().getUiTemplates().resolve(specDto.get("ui"))
                            : DEFAULT_UI_LAYOUT;

                    JsonObject buffersDto = specDto.getAsJsonObject("buffers");
                    SortedMap<String, BufferGroup.Factory> bufGroupFactories = new TreeMap<>();
                    for (Map.Entry<String, JsonElement> bufGroupDtoEntry : buffersDto.entrySet()) {
                        BufferGroup.Factory factory = new BufferGroup.Factory();
                        for (Map.Entry<String, JsonElement> bufferDtoEntry
                                : bufGroupDtoEntry.getValue().getAsJsonObject().entrySet()) {
                            ResourceLocation bufTypeId = new ResourceLocation(bufferDtoEntry.getKey());
                            BufferTypeRegistry.Entry<?, ?, ?, ?> bufTypeEntry = CbtMod.PROXY.getBufferTypes()
                                    .lookUp(bufTypeId);
                            if (bufTypeEntry == null) {
                                throw new ConfigException("Unknown buffer type: " + bufTypeId);
                            }
                            JsonElement bufferListDto = bufferDtoEntry.getValue();
                            if (bufferListDto.isJsonArray()) {
                                for (JsonElement bufferDto : bufferListDto.getAsJsonArray()) {
                                    loadBufferFactory(
                                            factory, bufTypeEntry.getBufferType(), bufferDto.getAsJsonObject());
                                }
                            } else {
                                loadBufferFactory(
                                        factory, bufTypeEntry.getBufferType(), bufferListDto.getAsJsonObject());
                            }
                        }
                        bufGroupFactories.put(bufGroupDtoEntry.getKey(), factory);
                    }

                    SingleBlockType<?, ?, ?> sbType = new SingleBlockType<>(
                            sbDir.getFileName().toString(), blockMat, bufGroupFactories, sbDir, recipeLogic, uiLayout);
                    sbSpecDtoCache.put(sbType.getId(), specDto);
                    sbTypeTable.put(sbType.getId(), sbType);
                    registrar.addBlockRegistration("sb_" + sbType.getId(), new SingleBlockMachineBlock(sbType));
                    CbtMod.LOGGER.debug("Loaded single-block: {}", sbType.getId());
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to load single-block specification: " + sbDir, e);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load single-block types!", e);
        }
        CbtMod.LOGGER.info("Finished loading single-block specifications.");
    }

    private static <B> void loadBufferFactory(BufferGroup.Factory bufGroupFactory, BufferType<B, ?, ?, ?> bufType,
                                              JsonObject bufferDto) {
        bufGroupFactory.addFactory(bufType, bufType.loadBufferFactory(bufferDto));
    }

    public void loadAll() {
        CbtMod.LOGGER.info("Loading single-block data...");
        for (SingleBlockType<?, ?, ?> sbType : sbTypeTable.values()) {
            try {
                lateInitSingleBlockType(sbType, sbSpecDtoCache.get(sbType.getId()).getAsJsonObject("recipe_logic"));
                CbtMod.LOGGER.debug("Initialized single-block: {}", sbType.getId());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load data for single-block: " + sbType.getId(), e);
            }
        }
        sbSpecDtoCache.clear();

        CbtMod.LOGGER.info("Loading single-block recipes...");
        for (SingleBlockType<?, ?, ?> sbType : sbTypeTable.values()) {
            loadRecipesForSingleBlock(sbType);
        }
        CbtMod.LOGGER.info("Finished loading single-block data.");
    }

    private static <R, C, D> void lateInitSingleBlockType(SingleBlockType<R, C, D> sbType,
                                                          JsonObject recipeLogicDto) {
        RecipeLogic<R, C, D, ?, ?> recipeLogic = sbType.getRecipeLogic();
        C recipeConfig = recipeLogic.loadConfig(recipeLogicDto);
        sbType.init(recipeConfig, recipeLogic.createEmptyDatabase(recipeConfig));
    }

    private <R, C, D> void loadRecipesForSingleBlock(SingleBlockType<R, C, D> sbType) {
        try {
            DataLoadUtils.loadRecipes(sbType.getId(), sbType.getRecipeDirectory(), "singleblock.json",
                    sbType.getRecipeLogic(), sbType.getRecipeConfig(), sbType.getRecipeDatabase());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load recipes for single-block: " + sbType.getId());
        }
    }

    public Collection<SingleBlockType<?, ?, ?>> getAll() {
        return sbTypeTable.values();
    }

    @Nullable
    public SingleBlockType<?, ?, ?> lookUp(String id) {
        return sbTypeTable.get(id);
    }

}
