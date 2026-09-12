package st.evening.mc.cbtweaker.behaviour.impl

import it.unimi.dsi.fastutil.objects.Object2FloatMap
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap
import mezz.jei.api.IJeiHelpers
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.MachineHost
import st.evening.mc.cbtweaker.behaviour.MachineStateFactory
import st.evening.mc.cbtweaker.buffer.BufferGroups
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.compat.jei.CbtJeiPlugin
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiRecipeSetAdaptor
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiUi
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiUiElementConstructVisitor
import st.evening.mc.cbtweaker.compat.jei.ui.impl.JeiProgressBarElement
import st.evening.mc.cbtweaker.gui.CbtGuiData
import st.evening.mc.cbtweaker.gui.element.BarControl
import st.evening.mc.cbtweaker.gui.inventory.SyncedUiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.gui.inventory.UiElementWrapper
import st.evening.mc.cbtweaker.gui.inventory.sliceBackground
import st.evening.mc.cbtweaker.recipe.impl.SimpleCraftingRecipe
import st.evening.mc.cbtweaker.singleblock.SingleBlockType
import st.evening.mc.cbtweaker.util.MachineSoundWrapper
import st.evening.mc.cbtweaker.util.SoundData
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.cbtweaker.util.gui.DrawableData
import st.evening.mc.cbtweaker.util.gui.SamplableData
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.cbtweaker.util.machine.ComponentSet
import st.evening.mc.cbtweaker.util.machine.NumberModifier
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.cbtweaker.util.recipe.LazyAccumulatorMap
import st.evening.mc.cbtweaker.util.recipe.MatcherChecker
import st.evening.mc.cbtweaker.util.recipe.MatcherConsumer
import st.evening.mc.cbtweaker.util.recipe.ProviderChecker
import st.evening.mc.cbtweaker.util.recipe.ProviderConsumer
import st.evening.mc.cbtweaker.util.recipe.checkInputs
import st.evening.mc.cbtweaker.util.recipe.checkOutputs
import st.evening.mc.cbtweaker.util.recipe.useInputs
import st.evening.mc.cbtweaker.util.recipe.useOutputs
import st.evening.mc.prelude.api.data.ser.BoolSerializer
import st.evening.mc.prelude.api.data.ser.IntSerializer
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.state.observeAll
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectString
import st.evening.mc.prelude.api.data.tjson.forEachObject
import st.evening.mc.prelude.api.data.tjson.useAny
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.gui.drawable.GuiDrawable
import st.evening.mc.prelude.api.gui.engine.prefab.StackLayout
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.collection.WeaklyValid
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.absurdLogicalSide
import st.evening.mc.prelude.api.util.game.sidedStrong
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation

object SimpleCraftingBehaviour : MachineBehaviour<SimpleCraftingBehaviour.State> {
    override val id: ResourceLocation = CbTweaker.resource("crafting")

    context(_: JsonPath)
    override fun loadStateFactory(machine: CraftingBlockType<*>, dto: TJson.Object): MachineStateFactory<State> {
        val config = Config(
            dto.useObject("modifiers") { modsDto ->
                buildMap {
                    modsDto.forEachObject { compId, compDto ->
                        put(compId, buildMap {
                            compDto.forEachObject { target, modDto ->
                                put(target, NumberModifier.Modifier.Serializer.deserializeFromJson(modDto))
                            }
                        })
                    }
                }
            } ?: emptyMap(),
            dto.useAny("ui_position") { UiPosition.load(it) } ?: UiPosition.CENTER,
            dto.useAny("bar_bg") { DrawableData.loadSlice(it) } ?: CbtGuiData.PROGRESS_BAR_BG,
            dto.useAny("bar_fg") { DrawableData.loadSlice(it) } ?: CbtGuiData.PROGRESS_BAR_FG,
            dto.expectInt("bar_offset_x") ?: 0,
            dto.expectInt("bar_offset_y") ?: 0,
            dto.useString("bar_orientation") { DrawOrientation.serializer.deserializeFromJson(it) }
                ?: DrawOrientation.LEFT_TO_RIGHT,
            dto.useAny("working_sound") { SoundData.load(it) }
        )
        val recipeSetEntry = CbTweaker.defns.recipeSets.getOrCreateRecipeSet(
            dto.expectString("recipes") ?: machine.id,
            SimpleCraftingRecipe.Type
        )
        dto.useString("jei") {
            when (it) {
                "none" -> {}
                "default" -> recipeSetEntry.registerJeiMachine(machine, null)

                "icon" -> recipeSetEntry.registerJeiMachine(
                    machine,
                    SimpleCraftingRecipe.JeiIconAdaptor(
                        recipeSetEntry.id,
                        machine.id,
                        SimpleCraftingRecipe.JeiConfig(
                            true,
                            config.progressBarBg,
                            config.progressBarFg,
                            config.barOffsetX,
                            config.barOffsetY,
                            config.barOrientation
                        )
                    )
                )

                "buffer" -> { // TODO configurable custom buffer adaptor
                    if (machine !is SingleBlockType<*>) {
                        throw SerializationException.withPath("Not a single-block machine!")
                    }
                    recipeSetEntry.registerJeiMachine(
                        machine,
                        JeiBufferAdaptor(recipeSetEntry.id, machine.id, config, machine)
                    )
                }

                else -> throw SerializationException.withPath("Unknown JEI adaptor: $it")
            }
        } ?: run {
            recipeSetEntry.registerJeiMachine(
                machine,
                (machine as? SingleBlockType<*>)?.let { JeiBufferAdaptor(recipeSetEntry.id, it.id, config, it) }
            )
        }
        val recipeDb = recipeSetEntry.database
        return factory@{ world, pos, bufGroups, components, host, oldState ->
            if (oldState != null && oldState.config === config && oldState.recipeDb === recipeDb) {
                oldState.reinit(world, pos, bufGroups, components, host)
                return@factory oldState
            } else {
                return@factory sidedStrong({
                    State.Server(config, recipeDb, world, pos, bufGroups, components, host)
                }, {
                    State.Client(config, recipeDb, world, pos)
                })
            }
        }
    }

    override fun notifyState(state: State, newComponents: ComponentSet?) {
        state.notifyState(newComponents)
    }

    override fun isActive(state: State): Boolean = state.activeState.value

    override fun getActiveState(state: State): Piecewise = state.activeState

    override fun getRedstoneControlHandler(state: State): RedstoneControlHandler = state.rsHandler

    override fun tick(state: State, ticker: TickModulator) {
        state.tick(ticker)
    }

    @ServerSide
    override fun handleBlockUpdate(state: State, blockState: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        state.getServer().handleBlockUpdate()
    }

    @ServerSide
    override fun serializeMachineToNbt(state: State, dto: NBTTagCompound) {
        state.getServer().writeToNbt(dto)
    }

    @ServerSide
    override fun deserializeMachineFromNbt(state: State, dto: NBTTagCompound) {
        state.getServer().readFromNbt(dto)
    }

    override fun createUiElement(state: State): UiElement = state.createUiElement()

    class Config(
        private val modTable: Map<String, Map<String, NumberModifier.Modifier>>,
        val uiPosition: UiPosition,
        val progressBarBg: DrawableData,
        val progressBarFg: SamplableData,
        val barOffsetX: Int,
        val barOffsetY: Int,
        val barOrientation: DrawOrientation,
        val workingSound: SoundData?
    ) {
        fun computeModifierState(components: ComponentSet): ModState {
            val modStateTable = mutableMapOf<String, NumberModifier>()
            modTable.forEach { (componentId, mods) ->
                val count = components.getCount(componentId)
                if (count <= 0) return@forEach
                mods.forEach { (target, mod) ->
                    modStateTable.getOrPut(target) { NumberModifier() }.addModifier(mod, count)
                }
            }
            val consumeFactors = Object2FloatOpenHashMap<String>()
            modStateTable.forEach { (target, mod) ->
                if (target.startsWith("consumption:")) {
                    consumeFactors.put(target.substring(12), mod.modify(1.0).toFloat())
                }
            }
            consumeFactors.defaultReturnValue(1F)
            return ModState(modStateTable, consumeFactors)
        }
    }

    class ModState(val modTable: Map<String, NumberModifier>, val consumeFactors: Object2FloatMap<String>)

    abstract class State(
        val config: Config,
        val recipeDb: SimpleCraftingRecipe.Database,
        protected var world: World,
        protected var pos: BlockPos
    ) : WeaklyValid {
        val rsHandler: RedstoneControlHandler = RedstoneControlHandler(world, pos)

        val activeState: ValueStateAtom<Boolean> = ValueStateAtom(false, BoolSerializer)
        protected val uiState: UiState = UiState()

        override val weakValidity: WeakValidity
            get() = WeakValidity.WEAK_VALID

        open fun reinit(
            world: World,
            pos: BlockPos,
            bufGroups: BufferGroups,
            components: ComponentSet,
            host: MachineHost
        ) {
            this.world = world
            this.pos = pos
        }

        @ServerSide
        abstract fun getServer(): Server

        @ClientSide.Strong
        abstract fun getClient(): Client

        open fun notifyState(newComponents: ComponentSet?) {}

        open fun tick(ticker: TickModulator) {}

        fun createUiElement(): UiElement = UiElementImpl()

        private class RunningRecipe(
            val recipe: SimpleCraftingRecipe,
            var workDone: Int,
            modTable: Map<String, NumberModifier>
        ) {
            var workNeeded: Int = recipe.computeModifiedDuration(modTable)
        }

        protected class UiState(state: ListStateComposite.Builder = ListStateComposite.Builder()) :
            Piecewise.Composite by state.build() {
            val work: ValueStateAtom<Int> = state part ValueStateAtom(0, IntSerializer)
            val maxWork: ValueStateAtom<Int> = state part ValueStateAtom(0, IntSerializer)
        }

        private inner class UiElementImpl : SyncedUiElement {
            override val syncData: Piecewise
                get() = uiState

            @ClientSide.Strong
            override fun addToGuiScreen(
                uiIndex: Int,
                layout: StackLayout,
                baseSlotIndex: Int,
                wrapper: UiElementWrapper
            ) {
                config.uiPosition.placeElement(
                    uiIndex, layout, wrapper,
                    BarControl.Progress(
                        config.progressBarBg.drawable,
                        config.progressBarFg.drawable,
                        { uiState.work.value },
                        { uiState.maxWork.value },
                        config.barOrientation,
                        config.barOffsetX,
                        config.barOffsetY
                    )
                )
            }
        }

        @ServerSide
        open class Server(
            config: Config,
            recipeDb: SimpleCraftingRecipe.Database,
            world: World,
            pos: BlockPos,
            protected var bufGroups: BufferGroups,
            components: ComponentSet,
            protected var host: MachineHost
        ) : State(config, recipeDb, world, pos), Observer.Simple, NbtCompoundSerializable {
            companion object {
                private const val SER_RECIPE: String = "recipe"
                private const val SER_WORK: String = "work"
                private const val SER_REDSTONE: String = "redstone"
            }

            protected var modState: ModState = config.computeModifierState(components)

            private var currentRecipe: RunningRecipe? = null
            private var cachedRecipe: SimpleCraftingRecipe? = null
            private var working: Boolean = false
                set(value) {
                    if (field != value) {
                        field = value
                        updateActiveState()
                    }
                }
            private var recipeDirty: Boolean = true
            private var stateDirty: Boolean = true

            init {
                rsHandler.observeState(this)
            }

            override fun reinit(
                world: World,
                pos: BlockPos,
                bufGroups: BufferGroups,
                components: ComponentSet,
                host: MachineHost
            ) {
                super.reinit(world, pos, bufGroups, components, host)
                this.bufGroups = bufGroups
                this.modState = config.computeModifierState(components)
                this.host = host
                updateCurrentRecipeWorkNeeded()
                recipeDirty = true
            }

            @ServerSide
            override fun getServer(): Server = this

            @ClientSide.Strong
            override fun getClient(): Client = absurdLogicalSide()

            override fun onObservableUpdate() { // observing redstone handler
                stateDirty = true
            }

            protected fun updateActiveState() {
                activeState.update(working && rsHandler.canWork())
            }

            fun handleBlockUpdate() {
                if (rsHandler.updateRedstoneState(world, pos)) {
                    updateActiveState()
                }
            }

            override fun notifyState(newComponents: ComponentSet?) {
                if (newComponents != null) {
                    modState = config.computeModifierState(newComponents)
                    updateCurrentRecipeWorkNeeded()
                }
                recipeDirty = true
            }

            private fun updateCurrentRecipeWorkNeeded() {
                currentRecipe?.let {
                    val workNeeded = it.recipe.computeModifiedDuration(modState.modTable)
                    it.workNeeded = workNeeded
                    uiState.maxWork.update(workNeeded)
                }
            }

            override fun tick(ticker: TickModulator) {
                doWork(ticker)
                if (stateDirty) {
                    host.onMachineStateChanged()
                    stateDirty = false
                }
            }

            private fun doWork(ticker: TickModulator) {
                if (!rsHandler.canWork() || !ticker.tick()) return
                val job = currentRecipe ?: tryFindAndStartRecipe(ticker) ?: return
                val recipe = job.recipe
                val outputs = recipe.outputTable
                val accs = LazyAccumulatorMap.Impl(bufGroups)
                if (job.workDone < job.workNeeded) {
                    val inputs = recipe.inputTable
                    val consumeFactors = modState.consumeFactors
                    if (!inputs.checkInputs(accs, consumeFactors, MatcherChecker.Periodic)) {
                        working = false
                        ticker.increaseIntervalUntil(8, 60)
                        return
                    }
                    inputs.useInputs(accs, consumeFactors, MatcherConsumer.Periodic)
                    outputs.useOutputs(accs, ProviderConsumer.Periodic)
                    job.workDone++
                    uiState.work.update(job.workDone)
                    stateDirty = true
                }
                if (job.workDone >= job.workNeeded) {
                    if (!outputs.checkOutputs(accs, ProviderChecker.Final)) {
                        working = false
                        ticker.increaseIntervalUntil(8, 60)
                        return
                    }
                    outputs.useOutputs(accs, ProviderConsumer.Final)
                    currentRecipe = null
                    uiState.work.update(0)
                    uiState.maxWork.update(0)
                    recipeDirty = true
                    stateDirty = true
                }
                ticker.interval = 1
                working = true
            }

            private fun tryFindAndStartRecipe(ticker: TickModulator): RunningRecipe? {
                if (!recipeDirty) return null
                recipeDirty = false
                val accs = LazyAccumulatorMap.Impl(bufGroups)
                val consumeFactors = modState.consumeFactors
                cachedRecipe?.let {
                    if (it.inputTable.checkInputs(accs, consumeFactors, MatcherChecker.Initial)) {
                        ticker.interval = 1
                        return startRecipe(it, accs)
                    }
                    cachedRecipe = null
                }
                val recipe = recipeDb.recipes.values.firstOrNull {
                    it.inputTable.checkInputs(accs, consumeFactors, MatcherChecker.Initial)
                }
                if (recipe == null) {
                    working = false
                    ticker.increaseIntervalUntil(8, 60)
                    return null
                }
                cachedRecipe = recipe
                ticker.interval = 1
                return startRecipe(recipe, accs)
            }

            private fun startRecipe(recipe: SimpleCraftingRecipe, accs: LazyAccumulatorMap): RunningRecipe {
                recipe.inputTable.useInputs(accs, modState.consumeFactors, MatcherConsumer.Initial)
                val job = RunningRecipe(recipe, 0, modState.modTable)
                currentRecipe = job
                uiState.work.update(job.workDone)
                uiState.maxWork.update(job.workNeeded)
                working = true
                stateDirty = true
                return job
            }

            override fun writeToNbt(dto: NBTTagCompound) {
                dto.runAction {
                    currentRecipe?.let {
                        SER_RECIPE string it.recipe.id
                        SER_WORK int it.workDone
                    }
                    SER_REDSTONE tag rsHandler.writeToNbt()
                }
            }

            override fun readFromNbt(dto: NBTTagCompound) {
                run {
                    if (dto.hasKey(SER_RECIPE, Constants.NBT.TAG_STRING)) {
                        val recipeId = dto.getString(SER_RECIPE)
                        val recipe = recipeDb.recipes[recipeId]
                        if (recipe != null) {
                            val job = RunningRecipe(recipe, dto.getInteger(SER_WORK), modState.modTable)
                            currentRecipe = job
                            uiState.work.update(job.workDone)
                            uiState.maxWork.update(job.workNeeded)
                            return@run
                        }
                        CbTweaker.logger.warn("Unknown {} recipe: {}", host.machineType.id, recipeId)
                    }
                    currentRecipe = null
                    uiState.work.update(0)
                    uiState.maxWork.update(0)
                }
                rsHandler.readFromNbt(dto.getString(SER_REDSTONE))
            }
        }

        @ClientSide.Strong
        class Client(
            config: Config,
            recipeDb: SimpleCraftingRecipe.Database,
            world: World,
            pos: BlockPos,
        ) : State(config, recipeDb, world, pos), Observer.Simple {
            private val workingSound: MachineSoundWrapper?

            init {
                val soundData = config.workingSound
                if (soundData != null) {
                    workingSound = MachineSoundWrapper(this, pos, soundData)
                    activeState.observeAll(this)
                } else {
                    workingSound = null
                }
            }

            override fun reinit(
                world: World,
                pos: BlockPos,
                bufGroups: BufferGroups,
                components: ComponentSet,
                host: MachineHost
            ) {
                super.reinit(world, pos, bufGroups, components, host)
                workingSound?.setPosition(pos)
            }

            @ServerSide
            override fun getServer(): Server = absurdLogicalSide()

            @ClientSide.Strong
            override fun getClient(): Client = this

            override fun onObservableUpdate() { // observing active state
                workingSound?.setActive(activeState.value)
            }
        }
    }

    class JeiBufferAdaptor(
        private val id: String,
        override val jeiDiscriminator: String?,
        private val config: Config,
        private val sbType: SingleBlockType<*>
    ) : JeiRecipeSetAdaptor<SimpleCraftingRecipe> {
        @ClientSide.Physical
        override fun getJeiCategoryName(): String = CbtJeiPlugin.getRecipeSetCategoryName(id, jeiDiscriminator)

        @ClientSide.Physical
        override fun getJeiBackground(): GuiDrawable = sbType.windowConfig.sliceBackground()

        @ClientSide.Physical
        override fun addJeiUiElements(
            recipe: SimpleCraftingRecipe,
            container: JeiUi,
            region: IntRectangle,
            jeiHelpers: IJeiHelpers
        ) {
            val barBg = config.progressBarBg.drawable
            val barPos = config.uiPosition.computePosition(region, barBg.width, barBg.height)
            container.addJeiUiElement(
                JeiProgressBarElement(
                    barPos.x,
                    barPos.y,
                    recipe.duration,
                    barBg,
                    config.progressBarFg.drawable,
                    config.barOffsetX,
                    config.barOffsetY,
                    config.barOrientation,
                    jeiHelpers.guiHelper
                )
            )

            val bufGroups = sbType.createJeiBufferGroups()
            recipe.populateJei(recipe, bufGroups)
            val visitor = JeiUiElementConstructVisitor(container, region, jeiHelpers)
            bufGroups.values.forEach {
                it.forEach(visitor)
            }
        }
    }
}
