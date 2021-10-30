package xyz.phanta.cbtweaker.integration.jei;

import io.github.phantamanta44.libnine.client.event.ClientTickHandler;
import io.github.phantamanta44.libnine.util.helper.InputUtils;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.transfer.IRecipeTransferRegistry;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;
import xyz.phanta.cbtweaker.singleblock.SingleBlockType;
import xyz.phanta.cbtweaker.structure.VisualizationToolItem;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@JEIPlugin
public class CbtJeiPlugin implements IModPlugin {

    private static final long ING_INDEX_INC_INTERVAL = 1000L;

    private static long lastClientTick = -1L;
    private static long lastIngIndexUpdate = -1L;
    private static int ingIndex = 0;

    public static int getGlobalIngredientIndex() {
        long tickNow = ClientTickHandler.getTick();
        if (tickNow == lastClientTick) {
            return ingIndex;
        }
        lastClientTick = tickNow;
        long now = System.currentTimeMillis();
        if (lastIngIndexUpdate == -1L || InputUtils.ModKey.SHIFT.isActive()) {
            lastIngIndexUpdate = now;
            return ingIndex;
        }
        long elapsedIntervals = (now - lastIngIndexUpdate) / ING_INDEX_INC_INTERVAL;
        ingIndex += elapsedIntervals;
        lastIngIndexUpdate += ING_INDEX_INC_INTERVAL * elapsedIntervals;
        return ingIndex; // it'll take 68 years for this to overflow, so we can just assume it'll never be negative
    }

    @Nullable
    public static <T> T getIngredientByGlobalIndex(List<T> ingredients) {
        switch (ingredients.size()) {
            case 0:
                return null;
            case 1:
                return ingredients.get(0);
            default:
                return ingredients.get(getGlobalIngredientIndex() % ingredients.size());
        }
    }

    public static String getLocalizedCbtModName() {
        return I18n.format(Objects.requireNonNull(CbtMod.INSTANCE.getDefaultCreativeTab()).getTranslationKey());
    }

    private final StructureVisualizationRecipeCategory structVisCat = new StructureVisualizationRecipeCategory();
    private final Map<MultiBlockType<?, ?, ?>, MultiBlockRecipeCategory<?, ?, ?>> mbCats = new HashMap<>();
    private final Map<SingleBlockType<?, ?, ?>, SingleBlockRecipeCategory<?, ?, ?>> sbCats = new HashMap<>();

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(structVisCat);
        for (MultiBlockType<?, ?, ?> mbType : CbtMod.PROXY.getMultiBlocks().getAll()) {
            MultiBlockRecipeCategory<?, ?, ?> cat = new MultiBlockRecipeCategory<>(mbType);
            mbCats.put(mbType, cat);
            registry.addRecipeCategories(cat);
        }
        for (SingleBlockType<?, ?, ?> sbType : CbtMod.PROXY.getSingleBlocks().getAll()) {
            SingleBlockRecipeCategory<?, ?, ?> cat = new SingleBlockRecipeCategory<>(sbType);
            sbCats.put(sbType, cat);
            registry.addRecipeCategories(cat);
        }
    }

    @Override
    public void register(IModRegistry registry) {
        IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        IRecipeTransferRegistry tfrRegistry = registry.getRecipeTransferRegistry();
        registry.addRecipeCatalyst(new ItemStack(VisualizationToolItem.ITEM), structVisCat.getUid());
        for (MultiBlockType<?, ?, ?> mbType : CbtMod.PROXY.getMultiBlocks().getAll()) {
            registry.addRecipes(Collections.singletonList(
                            new StructureRecipeWrapper(mbType.getControllerBlock(), mbType.getStructureMatcher())),
                    structVisCat.getUid());
        }
        for (Map.Entry<MultiBlockType<?, ?, ?>, MultiBlockRecipeCategory<?, ?, ?>> mbEntry : mbCats.entrySet()) {
            registerMultiBlock(registry, mbEntry.getKey(), mbEntry.getValue().getUid(), jeiHelpers);
        }
        for (Map.Entry<SingleBlockType<?, ?, ?>, SingleBlockRecipeCategory<?, ?, ?>> sbEntry : sbCats.entrySet()) {
            registerSingleBlock(registry, tfrRegistry, sbEntry.getKey(), sbEntry.getValue().getUid(), jeiHelpers);
        }
    }

    private static <R, C, D> void registerMultiBlock(IModRegistry registry,
                                                     MultiBlockType<R, C, D> mbType, String catId,
                                                     IJeiHelpers jeiHelpers) {
        registry.addRecipeCatalyst(new ItemStack(mbType.getControllerBlock()), catId);
        registry.addRecipes(
                mbType.getRecipeLogic().getRecipes(mbType.getRecipeConfig(), mbType.getRecipeDatabase()).stream()
                        .map(r -> CraftingBlockRecipeWrapper.wrapMultiBlock(mbType, r, jeiHelpers))
                        .collect(Collectors.toList()),
                catId);
    }

    private static <R, C, D> void registerSingleBlock(IModRegistry registry, IRecipeTransferRegistry tfrRegistry,
                                                      SingleBlockType<R, C, D> sbType, String catId,
                                                      IJeiHelpers jeiHelpers) {
        registry.addRecipeCatalyst(new ItemStack(sbType.getMachineBlock()), catId);
        registry.addRecipes(
                sbType.getRecipeLogic().getRecipes(sbType.getRecipeConfig(), sbType.getRecipeDatabase()).stream()
                        .map(r -> CraftingBlockRecipeWrapper.wrapSingleBlock(sbType, r, jeiHelpers))
                        .collect(Collectors.toList()),
                catId);
        // TODO jei recipe transfer handlers
        // tfrRegistry.addRecipeTransferHandler();
    }

}
