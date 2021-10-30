package xyz.phanta.cbtweaker.multiblock;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.math.Vec2i;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.ItemBlockRegistrar;
import xyz.phanta.cbtweaker.common.BlockMaterial;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.structure.StructureMatcher;
import xyz.phanta.cbtweaker.structure.StructureMatcherType;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MultiBlockManager {

    private static final UiLayout DEFAULT_UI_LAYOUT = new UiLayout(
            CbtTextureResources.GUI_GENERIC_SMALL, new ScreenRegion(7, 16, 162, 55), true, new Vec2i(7, 83), true);

    private final Path specDir;
    private final Map<String, JsonObject> mbSpecDtoCache = new HashMap<>();
    private final Map<String, MultiBlockType<?, ?, ?>> mbTypeTable = new HashMap<>();
    private final ItemBlockRegistrar registrar;

    public MultiBlockManager(Path specDir, ItemBlockRegistrar registrar) {
        this.specDir = specDir;
        this.registrar = registrar;
    }

    public void preloadAll() {
        CbtMod.LOGGER.info("Loading multi-block types...");
        try {
            Files.list(specDir).forEach(mbDir -> {
                try {
                    if (!Files.isDirectory(mbDir)) {
                        return;
                    }
                    Path specFile = mbDir.resolve("multiblock.json");
                    if (!Files.isRegularFile(specFile)) {
                        CbtMod.LOGGER.warn("Ignoring multi-block directory without a multiblock.json: {}", mbDir);
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

                    MultiBlockType<?, ?, ?> mbType = new MultiBlockType<>(
                            mbDir.getFileName().toString(), blockMat, mbDir, recipeLogic, uiLayout);
                    mbSpecDtoCache.put(mbType.getId(), specDto);
                    mbTypeTable.put(mbType.getId(), mbType);
                    registrar.addBlockRegistration("mb_" + mbType.getId(), new MultiBlockControllerBlock(mbType));
                    CbtMod.LOGGER.debug("Loaded multi-block: {}", mbType.getId());
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to load multi-block specification: " + mbDir, e);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load multi-block types!", e);
        }
        CbtMod.LOGGER.info("Finished loading multi-block specifications.");
    }

    public void loadAll() {
        CbtMod.LOGGER.info("Loading multi-block data...");
        for (MultiBlockType<?, ?, ?> mbType : mbTypeTable.values()) {
            try {
                JsonObject specDto = mbSpecDtoCache.get(mbType.getId());

                JsonObject structDto = specDto.getAsJsonObject("structure");
                ResourceLocation structTypeId = new ResourceLocation(structDto.get("type").getAsString());
                StructureMatcherType structType = CbtMod.PROXY.getStructureMatchers()
                        .lookUpStructureMatcher(structTypeId);
                if (structType == null) {
                    throw new ConfigException("Unknown structure matcher type: " + structTypeId);
                }
                StructureMatcher struct = structType.loadMatcher(mbType, structDto);

                lateInitMultiBlockType(mbType, struct, specDto.getAsJsonObject("recipe_logic"));
                CbtMod.LOGGER.debug("Initialized multi-block: {}", mbType.getId());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load data for multi-block: " + mbType.getId(), e);
            }
        }
        mbSpecDtoCache.clear();

        CbtMod.LOGGER.info("Loading multi-block recipes...");
        for (MultiBlockType<?, ?, ?> mbType : mbTypeTable.values()) {
            loadRecipesForMultiBlock(mbType);
        }
        CbtMod.LOGGER.info("Finished loading multi-block data.");
    }

    private static <R, C, D> void lateInitMultiBlockType(MultiBlockType<R, C, D> mbType,
                                                         StructureMatcher struct, JsonObject recipeLogicDto) {
        RecipeLogic<R, C, D, ?, ?> recipeLogic = mbType.getRecipeLogic();
        C recipeConfig = recipeLogic.loadConfig(recipeLogicDto);
        mbType.init(struct, recipeConfig, recipeLogic.createEmptyDatabase(recipeConfig));
    }

    private <R, C, D> void loadRecipesForMultiBlock(MultiBlockType<R, C, D> mbType) {
        try {
            DataLoadUtils.loadRecipes(mbType.getId(), mbType.getRecipeDirectory(), "multiblock.json",
                    mbType.getRecipeLogic(), mbType.getRecipeConfig(), mbType.getRecipeDatabase());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load recipes for multi-block: " + mbType.getId());
        }
    }

    public Collection<MultiBlockType<?, ?, ?>> getAll() {
        return mbTypeTable.values();
    }

    @Nullable
    public MultiBlockType<?, ?, ?> lookUp(String id) {
        return mbTypeTable.get(id);
    }

}
