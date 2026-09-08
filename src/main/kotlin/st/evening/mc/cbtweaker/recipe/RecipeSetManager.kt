package st.evening.mc.cbtweaker.recipe

import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiRecipeSetAdaptor
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TypedJsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class RecipeSetManager(private val recipeSetsDir: Path) : Iterable<RecipeSetManager.Entry<*, *>> {
    private val recipeSetTable: MutableMap<String, Entry<*, *>> = mutableMapOf()

    fun <R, D> getRecipeSet(id: String, type: RecipeSetType<R, D>): Entry<R, D>? {
        val entry = recipeSetTable[id] ?: return null
        @Suppress("UNCHECKED_CAST")
        return if (entry.recipeType == type) entry as Entry<R, D> else null
    }

    fun <R, D> getOrCreateRecipeSet(id: String, type: RecipeSetType<R, D>): Entry<R, D> {
        val entry = recipeSetTable[id]
        if (entry == null) {
            val newEntry = Entry(id, type, recipeSetsDir.resolve(id))
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

    override fun iterator(): Iterator<Entry<*, *>> = recipeSetTable.values.iterator()

    fun loadRecipes() {
        CbTweaker.logger.info("Loading recipe sets...")
        recipeSetTable.forEach { (id, entry) ->
            try {
                entry.loadRecipes()
                CbTweaker.logger.debug("Loaded recipes for recipe set: $id")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to load recipes for recipe set: $id", e)
            }
        }
        CbTweaker.logger.info("Finished loading recipe sets.")
    }

    class Entry<R, D>(val id: String, val recipeType: RecipeSetType<R, D>, private val recipeDir: Path) : Iterable<R> {
        val database: D

        init {
            if (!Files.isDirectory(recipeDir)) {
                throw SerializationException("Recipe set directory is not a directory: $recipeDir")
            }
            val specFile = recipeDir.resolve("recipeset.tjson")
            if (!Files.isRegularFile(specFile)) {
                throw SerializationException("Recipe set directory has no recipeset.tjson: $recipeDir")
            }
            val specDto = TypedJsonParser.parseObject(specFile.readText())
            database = JsonPath.atRoot { recipeType.loadDatabase(id, specDto) }
        }

        private val defaultJeiEntry: JeiEntry? = recipeType.getJeiRecipeAdaptor(database)?.let { JeiEntry(it) }
        private val jeiEntries: MutableList<JeiEntry> = mutableListOf()

        override fun iterator(): Iterator<R> = recipeType.iterateRecipes(database)

        context(_: JsonPath)
        fun loadRecipe(recipeId: String, recipeDto: TJson.Object) {
            recipeType.loadRecipe(database, recipeId, recipeDto)
        }

        fun loadRecipes() {
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
