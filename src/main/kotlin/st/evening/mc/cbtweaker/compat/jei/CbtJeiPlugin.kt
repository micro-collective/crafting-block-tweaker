package st.evening.mc.cbtweaker.compat.jei

import mezz.jei.api.IJeiHelpers
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.JEIPlugin
import mezz.jei.api.recipe.IRecipeCategoryRegistration
import net.minecraft.client.resources.I18n
import net.minecraft.item.ItemStack
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.compat.jei.recipe.CraftingBlockRecipeCategory
import st.evening.mc.cbtweaker.compat.jei.recipe.CraftingBlockRecipeWrapper
import st.evening.mc.cbtweaker.compat.jei.structure.StructureRecipeWrapper
import st.evening.mc.cbtweaker.compat.jei.structure.StructureVisualizationRecipeCategory
import st.evening.mc.cbtweaker.recipe.RecipeSetManager
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.math.Rect2i
import java.util.IdentityHashMap

private typealias RecipeSetSubMap = Map<RecipeSetManager.Entry<*, *>.JeiEntry, CraftingBlockRecipeCategory<*>>

@JEIPlugin
@ClientSide.Physical
class CbtJeiPlugin : IModPlugin {
    private val structVisCat: StructureVisualizationRecipeCategory = StructureVisualizationRecipeCategory()
    private val recipeSetCats: MutableMap<String, RecipeSetSubMap> = mutableMapOf()

    override fun registerCategories(registry: IRecipeCategoryRegistration) {
        registry.addRecipeCategories(structVisCat)
        CbTweaker.defns.recipeSets.forEach { entry ->
            val subMap = IdentityHashMap<RecipeSetManager.Entry<*, *>.JeiEntry, CraftingBlockRecipeCategory<*>>()
            entry.getJeiMachines().forEach {
                val cat = CraftingBlockRecipeCategory(entry.id, it.adaptor)
                subMap[it] = cat
                registry.addRecipeCategories(cat)
            }
            if (subMap.isNotEmpty()) {
                recipeSetCats[entry.id] = subMap
            }
        }
    }

    override fun register(registry: IModRegistry) {
        val structVisCatUid = structVisCat.uid
        registry.addRecipeCatalyst(ItemStack(CbTweaker.defns.itemVisualizationTool), structVisCatUid)
        CbTweaker.defns.multiBlocks.forEach {
            registry.addRecipes(
                listOf(StructureRecipeWrapper(it.controllerBlock, it.structureMatcher)),
                structVisCatUid
            )
        }

        val jeiHelpers = registry.jeiHelpers
        CbTweaker.defns.recipeSets.forEach {
            registerRecipes(registry, it, jeiHelpers)
        }
    }

    private fun <R> registerRecipes(
        registry: IModRegistry,
        entry: RecipeSetManager.Entry<R, *>,
        jeiHelpers: IJeiHelpers
    ) {
        val subMap = recipeSetCats[entry.id] ?: return
        entry.getJeiMachines().forEach { jeiEntry ->
            val recipeCat = subMap[jeiEntry] ?: return@forEach
            val recipeCatUid = recipeCat.uid
            jeiEntry.machines.forEach {
                registry.addRecipeCatalyst(ItemStack(it.craftingBlock), recipeCatUid)
            }
            val bg = recipeCat.background
            val region = Rect2i(0, 0, bg.width, bg.height)
            registry.addRecipes(
                entry.map {
                    val recipeWrapper = CraftingBlockRecipeWrapper()
                    jeiEntry.adaptor.addJeiUiElements(it, recipeWrapper, region, jeiHelpers)
                    return@map recipeWrapper
                },
                recipeCatUid
            )
        }
    }

    companion object {
        fun getRecipeSetCategoryName(recipeSetId: String, discriminator: String?): String = I18n.format(
            discriminator?.let { "${CbtConsts.MOD_ID}.jei.category.recipe.$recipeSetId.$it" }
                ?: "${CbtConsts.MOD_ID}.jei.category.recipe.$recipeSetId"
        )
    }
}
