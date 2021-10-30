package xyz.phanta.cbtweaker.multiblock;

import net.minecraftforge.fml.common.registry.ForgeRegistries;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.common.BlockMaterial;
import xyz.phanta.cbtweaker.common.CraftingBlockType;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.structure.StructureMatcher;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;

public class MultiBlockType<R, C, D> implements CraftingBlockType<R, C, D> {

    private final String id;
    private final BlockMaterial blockMaterial;
    private final Path recipeDir;
    private final RecipeLogic<R, C, D, ?, ?> recipeLogic;
    private final UiLayout uiLayout;

    // multiblock type has to be initialized at pre-init time so that the controller block can be constructed
    // however, these properties may rely on registry entries from other mods, so they must be initialized at init time
    // therefore, we use the slightly awkward late-init pattern
    @SuppressWarnings("NotNullFieldNotInitialized")
    private StructureMatcher structMatcher;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private C recipeConfig;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private D recipeDb;

    @Nullable
    private MultiBlockControllerBlock controllerBlock = null;

    public MultiBlockType(String id, BlockMaterial blockMaterial,
                          Path recipeDir, RecipeLogic<R, C, D, ?, ?> recipeLogic, UiLayout uiLayout) {
        this.id = id;
        this.blockMaterial = blockMaterial;
        this.recipeDir = recipeDir;
        this.recipeLogic = recipeLogic;
        this.uiLayout = uiLayout;
    }

    void init(StructureMatcher structMatcher, C recipeConfig, D recipeDb) {
        this.structMatcher = structMatcher;
        this.recipeConfig = recipeConfig;
        this.recipeDb = recipeDb;
    }

    public String getId() {
        return id;
    }

    public BlockMaterial getBlockMaterial() {
        return blockMaterial;
    }

    public Path getRecipeDirectory() {
        return recipeDir;
    }

    public StructureMatcher getStructureMatcher() {
        return structMatcher;
    }

    @Override
    public RecipeLogic<R, C, D, ?, ?> getRecipeLogic() {
        return recipeLogic;
    }

    public UiLayout getUiLayout() {
        return uiLayout;
    }

    @Override
    public C getRecipeConfig() {
        return recipeConfig;
    }

    @Override
    public D getRecipeDatabase() {
        return recipeDb;
    }

    public MultiBlockControllerBlock getControllerBlock() {
        if (controllerBlock == null) {
            controllerBlock = Objects.requireNonNull((MultiBlockControllerBlock)ForgeRegistries.BLOCKS.getValue(
                    CbtMod.INSTANCE.newResourceLocation("mb_" + id)));
        }
        return controllerBlock;
    }

    public String getTranslationKey() {
        return CbtMod.MOD_ID + ".multiblock." + id + ".name";
    }

}
