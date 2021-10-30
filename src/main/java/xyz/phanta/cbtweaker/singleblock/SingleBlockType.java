package xyz.phanta.cbtweaker.singleblock;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.buffer.BufferObserver;
import xyz.phanta.cbtweaker.common.BlockMaterial;
import xyz.phanta.cbtweaker.common.CraftingBlockType;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.integration.jei.JeiBufferGroup;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

public class SingleBlockType<R, C, D> implements CraftingBlockType<R, C, D> {

    private final String id;
    private final BlockMaterial blockMaterial;
    private final SortedMap<String, BufferGroup.Factory> bufGroupFactories;
    private final Path recipeDir;
    private final RecipeLogic<R, C, D, ?, ?> recipeLogic;
    private final UiLayout uiLayout;

    // singleblock type has to be initialized at pre-init time so that the machine block can be constructed
    // however, these properties may rely on registry entries from other mods, so they must be initialized at init time
    // therefore, we use the slightly awkward late-init pattern
    @SuppressWarnings("NotNullFieldNotInitialized")
    private C recipeConfig;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private D recipeDb;

    @Nullable
    private SingleBlockMachineBlock machineBlock = null;

    public SingleBlockType(String id, BlockMaterial blockMaterial,
                           SortedMap<String, BufferGroup.Factory> bufGroupFactories,
                           Path recipeDir, RecipeLogic<R, C, D, ?, ?> recipeLogic, UiLayout uiLayout) {
        this.id = id;
        this.blockMaterial = blockMaterial;
        this.bufGroupFactories = bufGroupFactories;
        this.recipeDir = recipeDir;
        this.recipeLogic = recipeLogic;
        this.uiLayout = uiLayout;
    }

    void init(C recipeConfig, D recipeDb) {
        this.recipeConfig = recipeConfig;
        this.recipeDb = recipeDb;
    }

    public String getId() {
        return id;
    }

    public BlockMaterial getBlockMaterial() {
        return blockMaterial;
    }

    public SortedMap<String, BufferGroup> createBufferGroups(World world, BlockPos pos,
                                                             @Nullable BufferObserver observer) {
        // Object2ObjectLinkedOpenHashMap maintains insertion order and NOT the natural key order
        // but as long as we insert the keys in the correct order, we can cheat a little to squeeze out a bit more speed
        SortedMap<String, BufferGroup> bufGroups = new Object2ObjectLinkedOpenHashMap<>(bufGroupFactories.size());
        for (Map.Entry<String, BufferGroup.Factory> entry : bufGroupFactories.entrySet()) {
            bufGroups.put(entry.getKey(), entry.getValue().createBufferGroup(world, pos, observer));
        }
        return bufGroups;
    }

    public Path getRecipeDirectory() {
        return recipeDir;
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

    public SingleBlockMachineBlock getMachineBlock() {
        if (machineBlock == null) {
            machineBlock = Objects.requireNonNull((SingleBlockMachineBlock)ForgeRegistries.BLOCKS.getValue(
                    CbtMod.INSTANCE.newResourceLocation("sb_" + id)));
        }
        return machineBlock;
    }

    public String getTranslationKey() {
        return CbtMod.MOD_ID + ".singleblock." + id + ".name";
    }

    public Map<String, JeiBufferGroup> createJeiBufferGroups() {
        Map<String, JeiBufferGroup> jeiBufGroups = new HashMap<>();
        for (Map.Entry<String, BufferGroup.Factory> entry : bufGroupFactories.entrySet()) {
            jeiBufGroups.put(entry.getKey(), entry.getValue().createJeiBufferGroup());
        }
        return jeiBufGroups;
    }

}
