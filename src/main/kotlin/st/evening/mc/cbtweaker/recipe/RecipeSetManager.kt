package st.evening.mc.cbtweaker.recipe

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityFurnace
import net.minecraft.util.NonNullList
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.oredict.OreDictionary
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.impl.ItemStackBuffer
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiRecipeSetAdaptor
import st.evening.mc.cbtweaker.recipe.impl.TimedFuelRecipe
import st.evening.mc.cbtweaker.util.machine.ItemConsumeType
import st.evening.mc.cbtweaker.util.recipe.IngredientMatcherMap
import st.evening.mc.cbtweaker.util.recipe.ItemSpecifier
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import st.evening.mc.prelude.api.util.game.ItemKey
import st.evening.mc.prelude.api.util.game.OreDictHelper
import st.evening.mc.prelude.api.util.game.OreEntry
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class RecipeSetManager(private val recipeSetsDir: Path) : Iterable<RecipeSetManager.Entry<*, *>> {
    companion object {
        const val BUILT_IN_FURNACE_FUEL: String = "furnace_fuel"
    }

    private val recipeSetTable: MutableMap<String, Entry<*, *>> = mutableMapOf()

    fun <R, D> getRecipeSet(id: String, type: RecipeSetType<R, D>): Entry<R, D>? {
        val entry = recipeSetTable[id] ?: return null
        @Suppress("UNCHECKED_CAST")
        return if (entry.recipeType == type) entry as Entry<R, D> else null
    }

    fun <R, D> getOrCreateRecipeSet(id: String, type: RecipeSetType<R, D>): Entry<R, D> {
        val entry = recipeSetTable[id]
        if (entry == null) {
            val newEntry = Entry.loadDatabase(id, type, recipeSetsDir.resolve(id))
            recipeSetTable[id] = newEntry
            return newEntry
        }
        if (entry.recipeType != type) {
            throw IllegalStateException(
                "Conflicting types for recipe set $id! Existing: ${entry.recipeType.debugName}, new: ${type.debugName}"
            )
        }
        @Suppress("UNCHECKED_CAST")
        return entry as Entry<R, D>
    }

    fun <R, D> createBuiltInRecipeSet(id: String, type: RecipeSetType<R, D>, database: D) {
        val newEntry = Entry(id, type, null, database)
        val clash = recipeSetTable.put(id, newEntry)
        if (clash != null) {
            throw IllegalStateException(
                "Duplicate recipe set! ID: $id, existing: ${clash.recipeType.debugName}, new: ${type.debugName}"
            )
        }
    }

    override fun iterator(): Iterator<Entry<*, *>> = recipeSetTable.values.iterator()

    fun loadRecipes() {
        CbTweaker.logger.info("Loading recipe sets...")
        recipeSetTable.forEach { (id, entry) ->
            try {
                entry.loadRecipes()
                CbTweaker.logger.debug("Loaded recipes for recipe set: {}", id)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load recipes for recipe set: $id", e)
            }
        }
        CbTweaker.logger.info(
            "Loaded {} recipe sets with {} total recipes",
            recipeSetTable.size,
            recipeSetTable.values.sumOf { it.size }
        )
    }

    fun loadBuiltInRecipes() {
        createBuiltInRecipeSet(
            BUILT_IN_FURNACE_FUEL,
            TimedFuelRecipe.Type,
            TimedFuelRecipe.Database(BUILT_IN_FURNACE_FUEL, TimedFuelRecipe.JeiConfig.DEFAULT).also { db ->
                // there's no actual registry of furnace fuel items because fuel-ness of an item is a dynamic
                // property, so we'll approximate it by checking the fuel-ness of every registered item
                val fuelItems = Object2IntOpenHashMap<ItemKey>()
                ForgeRegistries.ITEMS.forEach { item ->
                    val subItems = NonNullList.create<ItemStack>()
                    item.getSubItems(CreativeTabs.SEARCH, subItems)
                    subItems.forEach { stack ->
                        val burnTime = TileEntityFurnace.getItemBurnTime(stack)
                        if (burnTime > 0) {
                            fuelItems.put(ItemKey.fromStack(stack)!!, burnTime)
                        }
                    }
                }
                val seenOreIds = Int2ObjectOpenHashMap<Boolean>()
                fuelItems.forEach { (item, burnTime) ->
                    var shouldMakeItemEntry = true
                    OreDictHelper.forEachOreId(item) { oreId ->
                        when (seenOreIds[oreId]) {
                            true -> shouldMakeItemEntry = false
                            false -> {} // a different ore ID might work
                            null -> {
                                if (
                                    OreDictHelper.getOreStacks(oreId).all {
                                        TileEntityFurnace.getItemBurnTime(it) == burnTime
                                    }
                                ) {
                                    seenOreIds.put(oreId, true)
                                    shouldMakeItemEntry = false
                                    val oreName = OreDictionary.getOreName(oreId)
                                    val id = "ore/$oreName"
                                    db.recipeMap[id] = TimedFuelRecipe(
                                        id,
                                        mapOf(
                                            "fuel" to IngredientMatcherMap().also {
                                                it[ItemStackBuffer.Type] = listOf(
                                                    ItemStackBuffer.OreDictionaryMatcher(
                                                        OreEntry(oreName), 1, ItemConsumeType.CONSUME
                                                    )
                                                )
                                            }
                                        ),
                                        burnTime
                                    )
                                } else {
                                    seenOreIds.put(oreId, false)
                                }
                            }
                        }
                    }
                    if (shouldMakeItemEntry) {
                        val id = item.dataTag?.let { // this *should* be deterministic
                            "item/${item.item.registryName!!}/${item.meta}/${it.hashCode()}"
                        } ?: "item/${item.item.registryName!!}/${item.meta}"
                        db.recipeMap[id] = TimedFuelRecipe(
                            id,
                            mapOf(
                                "fuel" to IngredientMatcherMap().also {
                                    it[ItemStackBuffer.Type] = listOf(
                                        ItemStackBuffer.ItemMatcher(
                                            run {
                                                val stack = item.newStack(1)
                                                if (stack.isItemStackDamageable) {
                                                    stack.itemDamage = 1
                                                    if (TileEntityFurnace.getItemBurnTime(stack) == burnTime) {
                                                        return@run ItemSpecifier.Wildcard.fromKey(item)
                                                    }
                                                }
                                                return@run ItemSpecifier.ByMeta.fromKey(item)
                                            },
                                            1,
                                            ItemConsumeType.CONSUME
                                        )
                                    )
                                }
                            ),
                            burnTime
                        )
                    }
                }
            }
        )
    }

    class Entry<R, D>(
        val id: String,
        val recipeType: RecipeSetType<R, D>,
        private val recipeDir: Path?,
        val database: D
    ) : AbstractCollection<R>() {
        companion object {
            fun <R, D> loadDatabase(id: String, recipeType: RecipeSetType<R, D>, recipeDir: Path): Entry<R, D> {
                if (!Files.exists(recipeDir)) {
                    CbTweaker.logger.warn("Creating missing recipe set directory: $recipeDir")
                    Files.createDirectories(recipeDir)
                } else if (!Files.isDirectory(recipeDir)) {
                    throw SerializationException("Recipe set directory is not a directory: $recipeDir")
                }
                val specFile = recipeDir.resolve("recipeset.tjson")
                val specDto = if (Files.isRegularFile(specFile)) {
                    TypedJsonParser.parseObject(specFile.readText())
                } else {
                    TJson.Object()
                }
                return Entry(id, recipeType, recipeDir, JsonPath.atRoot { recipeType.loadDatabase(id, specDto) })
            }
        }

        private val defaultJeiEntry: JeiEntry? = recipeType.getJeiRecipeAdaptor(database)?.let { JeiEntry(it) }
        private val jeiEntries: MutableList<JeiEntry> = mutableListOf()

        override val size: Int
            get() = recipeType.getRecipeCount(database)

        override fun iterator(): Iterator<R> = recipeType.iterateRecipes(database)

        context(_: JsonPath)
        fun loadRecipe(recipeId: String, recipeDto: TJson.Object) {
            recipeType.loadRecipe(database, recipeId, recipeDto)
        }

        fun loadRecipes() {
            if (recipeDir == null) return
            val recipeDirName = recipeDir.fileName.toString()
            Files.newDirectoryStream(recipeDir).use { dirStream ->
                dirStream.asSequence().sortedBy { it.fileName }.forEach { recipeFile ->
                    try {
                        if (!Files.isRegularFile(recipeFile)) return@forEach
                        val recipeFileName = recipeFile.fileName.toString()
                        if (!recipeFileName.endsWith(".tjson")) {
                            CbTweaker.logger.warn(
                                "Ignoring non-TJSON file in recipe directory: {}/{}", recipeDirName, recipeFileName
                            )
                            return@forEach
                        }
                        val recipeId = recipeFileName.dropLast(6)
                        if (recipeId == "recipeset") return@forEach // skip the recipe set spec
                        if (recipeId.isBlank()) {
                            throw SerializationException(
                                "Empty recipe ID for recipe file: $recipeDirName/$recipeFileName"
                            )
                        }
                        val recipeDto = TypedJsonParser.parseObject(recipeFile.readText())
                        JsonPath.atRoot {
                            loadRecipe(recipeId, recipeDto)
                        }
                        CbTweaker.logger.debug("Loaded recipe: {}/{}", recipeDirName, recipeId)
                    } catch (e: Exception) {
                        CbTweaker.logger.warn(
                            "Could not load recipe file: {}/{}", recipeDirName, recipeFile.fileName, e
                        )
                    }
                }
            }
        }

        fun registerJeiMachine(machine: CraftingBlockType<*>, adaptor: JeiRecipeSetAdaptor<R>?) {
            if (adaptor != null) {
                val entry = JeiEntry(adaptor)
                jeiEntries += entry
                entry.registerMachine(machine)
            } else {
                defaultJeiEntry?.registerMachine(machine)
            }
        }

        fun getJeiMachines(): Sequence<JeiEntry> =
            defaultJeiEntry?.let { sequenceOf(it) + jeiEntries.asSequence() } ?: jeiEntries.asSequence()

        inner class JeiEntry(val adaptor: JeiRecipeSetAdaptor<R>) {
            private val _machines: MutableSet<CraftingBlockType<*>> = mutableSetOf()

            val machines: Set<CraftingBlockType<*>>
                get() = _machines

            fun registerMachine(machine: CraftingBlockType<*>) {
                _machines += machine
            }
        }
    }
}
