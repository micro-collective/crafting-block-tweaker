package xyz.phanta.cbtweaker.integration.jei;

import io.github.phantamanta44.libnine.util.math.Vec2i;
import io.github.phantamanta44.libnine.util.render.RenderUtils;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.render.IconJeiUiElement;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.singleblock.SingleBlockType;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.helper.CbtMathUtils;

import java.util.*;

public class CraftingBlockRecipeWrapper implements IRecipeWrapper {

    public static <R, C> CraftingBlockRecipeWrapper wrapSingleBlock(SingleBlockType<R, C, ?> sbType, R recipe,
                                                                    IJeiHelpers jeiHelpers) {
        ScreenRegion machInvRegion = sbType.getUiLayout().getMachineInventoryRegion();
        ScreenRegion recipeRegion = new ScreenRegion(0, 0, machInvRegion.getWidth(), machInvRegion.getHeight());
        RecipeLogic<R, C, ?, ?, ?> recipeLogic = sbType.getRecipeLogic();
        C recipeConfig = sbType.getRecipeConfig();
        CraftingBlockRecipeWrapper recipeWrapper = new CraftingBlockRecipeWrapper();

        JeiUiElement<?> recipeUiElem = recipeLogic.createJeiUiElement(recipeConfig, recipe, recipeRegion, jeiHelpers);
        if (recipeUiElem != null) {
            recipeWrapper.registerUiElement(recipeUiElem);
        }

        Map<String, JeiBufferGroup> bufGroups = sbType.createJeiBufferGroups();
        recipeLogic.populateJei(recipeConfig, recipe, bufGroups);
        for (JeiBufferGroup bufGroup : bufGroups.values()) {
            bufGroup.forEach(new JeiBufferGroup.Visitor() {
                @Override
                public <JB, JA> void visit(BufferType<?, ?, JB, JA> bufType, List<JB> buffers) {
                    for (JB buffer : buffers) {
                        for (JeiUiElement<?> uiElem : bufType.createJeiUiElements(buffer, recipeRegion, jeiHelpers)) {
                            recipeWrapper.registerUiElement(uiElem);
                        }
                    }
                }
            });
        }

        return recipeWrapper;
    }

    public static <R, C> CraftingBlockRecipeWrapper wrapMultiBlock(MultiBlockType<R, C, ?> mbType, R recipe,
                                                                   IJeiHelpers jeiHelpers) {
        ScreenRegion machInvRegion = mbType.getUiLayout().getMachineInventoryRegion();
        ScreenRegion recipeRegion = new ScreenRegion(0, 0, machInvRegion.getWidth(), machInvRegion.getHeight());
        RecipeLogic<R, C, ?, ?, ?> recipeLogic = mbType.getRecipeLogic();
        C recipeConfig = mbType.getRecipeConfig();
        CraftingBlockRecipeWrapper recipeWrapper = new CraftingBlockRecipeWrapper();

        ScreenRegion inputsRegion, outputsRegion;
        JeiUiElement<?> recipeUiElem = recipeLogic.createJeiUiElement(recipeConfig, recipe, recipeRegion, jeiHelpers);
        if (recipeUiElem != null) {
            recipeWrapper.registerUiElement(recipeUiElem);
            ScreenRegion uiRegion = recipeUiElem.getIngredientRegion();
            inputsRegion = new ScreenRegion(recipeRegion.getX(), recipeRegion.getY(),
                    uiRegion.getX() - recipeRegion.getX(), recipeRegion.getHeight());
            outputsRegion = new ScreenRegion(uiRegion.getX() + uiRegion.getWidth(), recipeRegion.getY(),
                    recipeRegion.getX() + recipeRegion.getWidth() - uiRegion.getX() - uiRegion.getWidth(),
                    recipeRegion.getHeight());
        } else {
            int width = recipeRegion.getWidth() / 2;
            inputsRegion = new ScreenRegion(recipeRegion.getX(), recipeRegion.getY(), width, recipeRegion.getHeight());
            outputsRegion = new ScreenRegion(
                    recipeRegion.getX() + width, recipeRegion.getY(), width, recipeRegion.getHeight());
        }

        List<Pair<String, JeiIngredient<?>>> inputIngs = new ArrayList<>(), outputIngs = new ArrayList<>();
        for (Map.Entry<String, Map<BufferType<?, ?, ?, ?>, List<JeiIngredient<?>>>> bufGroupEntry
                : recipeLogic.getJeiIngredients(recipeConfig, recipe).entrySet()) {
            String bufGroupId = bufGroupEntry.getKey();
            for (List<JeiIngredient<?>> ingList : bufGroupEntry.getValue().values()) {
                for (JeiIngredient<?> ing : ingList) {
                    switch (ing.getRole()) {
                        case INPUT:
                            inputIngs.add(Pair.of(bufGroupId, ing));
                            break;
                        case OUTPUT:
                            outputIngs.add(Pair.of(bufGroupId, ing));
                            break;
                    }
                }
            }
        }
        layOutIngredientGroup(recipeWrapper, inputIngs, inputsRegion);
        layOutIngredientGroup(recipeWrapper, outputIngs, outputsRegion);

        return recipeWrapper;
    }

    private static void layOutIngredientGroup(CraftingBlockRecipeWrapper recipeWrapper,
                                              List<Pair<String, JeiIngredient<?>>> ings, ScreenRegion contRegion) {
        Vec2i[] slotPosList = new Vec2i[ings.size()];
        Vec2i dims = CbtMathUtils.layOutSlotGroup(17, 17, slotPosList);
        ScreenRegion region = Positioning.FromCenter.CENTER
                .computeRegion(dims.getX() - 1, dims.getY() - 1, contRegion);
        for (int i = 0; i < slotPosList.length; i++) {
            Vec2i slotOffset = slotPosList[i];
            Pair<String, JeiIngredient<?>> ingEntry = ings.get(i);
            recipeWrapper.registerUiElement(new IconJeiUiElement<>(
                    region.getX() + slotOffset.getX(), region.getY() + slotOffset.getY(),
                    ingEntry.getRight(), ingEntry.getLeft()));
        }
    }

    private final Map<IIngredientType<?>, List<? extends List<?>>> inputLists = new HashMap<>();
    private final Map<IIngredientType<?>, List<? extends List<?>>> outputLists = new HashMap<>();
    private final Map<IIngredientType<?>, List<JeiUiElement<?>>> jeiElements = new HashMap<>();
    private final List<JeiUiElement<?>> nonJeiElements = new ArrayList<>();

    private CraftingBlockRecipeWrapper() {
        // NO-OP
    }

    @SuppressWarnings("unchecked")
    private <T> void registerUiElement(JeiUiElement<T> uiElem) {
        IIngredientType<T> jeiIngType = uiElem.getJeiIngredientType();
        if (jeiIngType != null) {
            JeiIngredient<T> ing = uiElem.getIngredient();
            if (ing != null) {
                switch (ing.getRole()) {
                    case INPUT:
                        ((List<List<T>>)inputLists.computeIfAbsent(jeiIngType, k -> new ArrayList<>()))
                                .add(ing.getIngredients());
                        break;
                    case OUTPUT:
                        ((List<List<T>>)outputLists.computeIfAbsent(jeiIngType, k -> new ArrayList<>()))
                                .add(ing.getIngredients());
                        break;
                }
            }
            jeiElements.computeIfAbsent(jeiIngType, k -> new ArrayList<>()).add(uiElem);
        } else {
            nonJeiElements.add(uiElem);
        }
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        for (Map.Entry<IIngredientType<?>, List<? extends List<?>>> entry : inputLists.entrySet()) {
            setInputLists(ingredients, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<IIngredientType<?>, List<? extends List<?>>> entry : outputLists.entrySet()) {
            setOutputLists(ingredients, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void setInputLists(IIngredients jeiIngs,
                                          IIngredientType<T> ingType, List<? extends List<?>> ings) {
        jeiIngs.setInputLists(ingType, (List<List<T>>)ings);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setOutputLists(IIngredients jeiIngs,
                                           IIngredientType<T> ingType, List<? extends List<?>> ings) {
        jeiIngs.setOutputLists(ingType, (List<List<T>>)ings);
    }

    public void layOutRecipe(IRecipeLayout layout) {
        MutableInt index = new MutableInt(0);
        for (Map.Entry<IIngredientType<?>, List<JeiUiElement<?>>> ingTypeEntry : jeiElements.entrySet()) {
            layOutIngredients(layout, index, ingTypeEntry.getKey(), ingTypeEntry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void layOutIngredients(IRecipeLayout layout, MutableInt index, IIngredientType<T> ingType,
                                              List<JeiUiElement<?>> uiElems) {
        IGuiIngredientGroup<T> ingGroup = layout.getIngredientsGroup(ingType);
        for (JeiUiElement<?> uiElemRaw : uiElems) {
            JeiUiElement<T> uiElem = (JeiUiElement<T>)uiElemRaw;
            int indexHere = index.getAndIncrement();
            ScreenRegion region = uiElem.getIngredientRegion();
            JeiIngredient<T> ing = uiElem.getIngredient();
            if (ing != null) {
                ingGroup.init(
                        indexHere, ing.getRole() == JeiIngredient.Role.INPUT, new JeiUiElement.JeiRenderer<>(uiElem),
                        region.getX(), region.getY(), region.getWidth(), region.getHeight(), 0, 0);
                ingGroup.set(indexHere, ing.getIngredients());
            } else {
                ingGroup.init(indexHere, false, new JeiUiElement.JeiRenderer<>(uiElem),
                        region.getX(), region.getY(), region.getWidth(), region.getHeight(), 0, 0);
            }
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        for (JeiUiElement<?> uiElem : nonJeiElements) {
            drawUiElem(uiElem, mouseX, mouseY);
        }
    }

    private static <T> void drawUiElem(JeiUiElement<T> uiElem, int mouseX, int mouseY) {
        GlStateManager.enableBlend();
        JeiIngredient<T> ing = uiElem.getIngredient();
        if (ing == null) {
            uiElem.render(null);
            return;
        }
        uiElem.render(CbtJeiPlugin.getIngredientByGlobalIndex(ing.getIngredients()));
        ScreenRegion ingRegion = uiElem.getIngredientRegion();
        if (ingRegion.contains(mouseX, mouseY)) {
            int x = ingRegion.getX(), y = ingRegion.getY();
            Gui.drawRect(x, y, x + ingRegion.getWidth(), y + ingRegion.getHeight(), 0x80FFFFFF);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        ITooltipFlag tooltipFlags = RenderUtils.getTooltipFlags();
        List<String> tooltip = new ArrayList<>();
        for (JeiUiElement<?> uiElem : nonJeiElements) {
            tryGetUiElemTooltip(uiElem, mouseX, mouseY, tooltip, tooltipFlags);
            if (!tooltip.isEmpty()) {
                return tooltip;
            }
        }
        return Collections.emptyList();
    }

    private static <T> void tryGetUiElemTooltip(JeiUiElement<T> uiElem, int mouseX, int mouseY,
                                                List<String> tooltip, ITooltipFlag tooltipFlags) {
        if (!uiElem.getIngredientRegion().contains(mouseX, mouseY)) {
            return;
        }
        JeiIngredient<T> ing = uiElem.getIngredient();
        uiElem.getTooltip(ing != null
                ? CbtJeiPlugin.getIngredientByGlobalIndex(ing.getIngredients()) : null, tooltip, tooltipFlags);
    }

}
