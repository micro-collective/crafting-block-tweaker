package st.evening.mc.cbtweaker.util.recipe

import it.unimi.dsi.fastutil.objects.Object2FloatMap
import net.minecraft.nbt.NBTTagCompound
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.buffer.BufferGroups
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.util.data.getStringOrNull
import st.evening.mc.prelude.api.util.data.runAction

abstract class RecipeExecutor<R> : NbtCompoundSerializable {
    companion object {
        private const val SER_RECIPE: String = "recipe"
        private const val SER_WORK: String = "work"
    }

    private var currentRecipe: RunningRecipe? = null
    private var cachedRecipe: R? = null
    private var recipeDirty: Boolean = true

    var working: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                onWorkingChanged()
            }
        }

    abstract val debugName: String

    protected abstract fun getBuffers(): BufferGroups

    protected abstract fun getConsumeFactors(): Object2FloatMap<String>

    protected abstract fun getRecipeId(recipe: R): String

    protected abstract fun getRecipeById(recipeId: String): R?

    protected abstract fun getRecipeDuration(recipe: R): Int

    protected abstract fun getRecipeInputs(recipe: R): Map<String, IngredientMatcherMap>

    protected abstract fun getRecipeOutputs(recipe: R): Map<String, IngredientProviderMap>

    protected abstract fun findRecipe(accs: LazyAccumulatorMap, consumeFactors: Object2FloatMap<String>): R?

    protected open fun canStartRecipe(
        recipe: R,
        accs: LazyAccumulatorMap,
        consumeFactors: Object2FloatMap<String>
    ): Boolean = true

    protected open fun canProgressRecipe(
        recipe: R,
        accs: LazyAccumulatorMap,
        consumeFactors: Object2FloatMap<String>,
        workNeeded: Int,
        workDone: Int
    ): Boolean = true

    protected open fun updateWork(newWork: Int) {}

    protected open fun updateMaxWork(newMaxWork: Int) {}

    protected open fun onWorkingChanged() {}

    protected open fun onStateChanged() {}

    fun notifyRecipeDirty() {
        recipeDirty = true
    }

    fun updateCurrentRecipeWorkNeeded() {
        currentRecipe?.let {
            val workNeeded = getRecipeDuration(it.recipe)
            it.workNeeded = workNeeded
            updateMaxWork(workNeeded)
        }
    }

    private fun backOff(ticker: TickModulator) {
        working = false
        ticker.increaseIntervalUntil(8, 60)
    }

    fun tick(ticker: TickModulator) {
        if (!ticker.tick()) return
        val job = currentRecipe ?: tryFindAndStartRecipe(ticker) ?: return
        val recipe = job.recipe
        val outputs = getRecipeOutputs(recipe)
        val accs = LazyAccumulatorMap.Impl(getBuffers())
        if (job.workDone < job.workNeeded) {
            val consumeFactors = getConsumeFactors()
            if (!canProgressRecipe(recipe, accs, consumeFactors, job.workNeeded, job.workDone)) {
                backOff(ticker)
                return
            }
            val inputs = getRecipeInputs(recipe)
            if (!inputs.checkInputs(accs, consumeFactors, MatcherChecker.Periodic)) {
                backOff(ticker)
                return
            }
            inputs.useInputs(accs, consumeFactors, MatcherConsumer.Periodic)
            outputs.useOutputs(accs, ProviderConsumer.Periodic)
            job.workDone++
            updateWork(job.workDone)
            onStateChanged()
        }
        if (job.workDone >= job.workNeeded) {
            if (!outputs.checkOutputs(accs, ProviderChecker.Final)) {
                backOff(ticker)
                return
            }
            outputs.useOutputs(accs, ProviderConsumer.Final)
            currentRecipe = null
            updateWork(0)
            updateMaxWork(0)
            onStateChanged()
            recipeDirty = true
        }
        ticker.interval = 1
        working = true
    }

    private fun tryFindAndStartRecipe(ticker: TickModulator): RunningRecipe? {
        if (!recipeDirty) return null
        recipeDirty = false
        val accs = LazyAccumulatorMap.Impl(getBuffers())
        val consumeFactors = getConsumeFactors()
        cachedRecipe?.let {
            if (
                getRecipeInputs(it).checkInputs(accs, consumeFactors, MatcherChecker.Initial) &&
                getRecipeOutputs(it).checkOutputs(accs, ProviderChecker.Final)
            ) {
                return tryStartRecipe(it, accs, consumeFactors, ticker)
            }
            cachedRecipe = null
        }
        val recipe = findRecipe(accs, consumeFactors)
        if (recipe == null) {
            backOff(ticker)
            return null
        }
        cachedRecipe = recipe
        return tryStartRecipe(recipe, accs, consumeFactors, ticker)
    }

    private fun tryStartRecipe(
        recipe: R,
        accs: LazyAccumulatorMap,
        consumeFactors: Object2FloatMap<String>,
        ticker: TickModulator
    ): RunningRecipe? {
        if (!canStartRecipe(recipe, accs, consumeFactors)) return null // recipe will have been cached
        getRecipeInputs(recipe).useInputs(accs, consumeFactors, MatcherConsumer.Initial)
        val job = RunningRecipe(recipe, getRecipeDuration(recipe), 0)
        currentRecipe = job
        updateWork(job.workDone)
        updateMaxWork(job.workNeeded)
        onStateChanged()
        working = true
        ticker.interval = 1
        return job
    }

    override fun writeToNbt(dto: NBTTagCompound) {
        dto.runAction {
            currentRecipe?.let {
                SER_RECIPE string getRecipeId(it.recipe)
                SER_WORK int it.workDone
            }
        }
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        run {
            dto.getStringOrNull(SER_RECIPE)?.let { recipeId ->
                val recipe = getRecipeById(recipeId)
                if (recipe != null) {
                    val job = RunningRecipe(recipe, getRecipeDuration(recipe), dto.getInteger(SER_WORK))
                    currentRecipe = job
                    updateWork(job.workDone)
                    updateMaxWork(job.workNeeded)
                    return@run
                }
                CbTweaker.logger.warn("Unknown {} recipe: {}", debugName, recipeId)
            }
            currentRecipe = null
            updateWork(0)
            updateMaxWork(0)
        }
    }

    private inner class RunningRecipe(val recipe: R, var workNeeded: Int, var workDone: Int)
}
