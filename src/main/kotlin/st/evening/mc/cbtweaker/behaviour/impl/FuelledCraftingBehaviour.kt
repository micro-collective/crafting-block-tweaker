package st.evening.mc.cbtweaker.behaviour.impl

import it.unimi.dsi.fastutil.objects.Object2FloatMap
import mezz.jei.api.IJeiHelpers
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
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
import st.evening.mc.cbtweaker.recipe.RecipeSetManager
import st.evening.mc.cbtweaker.recipe.impl.SimpleCraftingRecipe
import st.evening.mc.cbtweaker.recipe.impl.TimedFuelRecipe
import st.evening.mc.cbtweaker.serconfig.CopiableConfigHost
import st.evening.mc.cbtweaker.singleblock.SingleBlockType
import st.evening.mc.cbtweaker.util.MachineSoundWrapper
import st.evening.mc.cbtweaker.util.SoundData
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.cbtweaker.util.gui.BarDrawData
import st.evening.mc.cbtweaker.util.gui.Positioned
import st.evening.mc.cbtweaker.util.gui.UiPosition
import st.evening.mc.cbtweaker.util.machine.ComponentSet
import st.evening.mc.cbtweaker.util.machine.NumberModifier
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.cbtweaker.util.machine.tryModifyIntCeil
import st.evening.mc.cbtweaker.util.machine.tryModifyIntFloor
import st.evening.mc.cbtweaker.util.recipe.LazyAccumulatorMap
import st.evening.mc.cbtweaker.util.recipe.MatcherChecker
import st.evening.mc.cbtweaker.util.recipe.MatcherConsumer
import st.evening.mc.cbtweaker.util.recipe.ProviderChecker
import st.evening.mc.cbtweaker.util.recipe.checkInputs
import st.evening.mc.cbtweaker.util.recipe.checkOutputs
import st.evening.mc.cbtweaker.util.recipe.useInputs
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
import st.evening.mc.prelude.api.util.data.getStringOrNull
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.absurdLogicalSide
import st.evening.mc.prelude.api.util.game.sidedStrong
import st.evening.mc.prelude.api.util.math.IntRectangle
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.prelude.api.util.render.gui.DrawOrientation

object FuelledCraftingBehaviour : MachineBehaviour<FuelledCraftingBehaviour.State> {
    val DEFAULT_PROGRESS_BAR: Positioned<BarDrawData> = Positioned(
        UiPosition(DrawAlignment.CENTER, DrawAlignment.CENTER, 0, -9),
        BarDrawData(CbtGuiData.PROGRESS_BAR_BG, CbtGuiData.PROGRESS_BAR_FG, 0, 0, DrawOrientation.LEFT_TO_RIGHT)
    )
    val DEFAULT_FUEL_BAR: Positioned<BarDrawData> = Positioned(
        UiPosition(DrawAlignment.CENTER, DrawAlignment.CENTER, 0, 7),
        BarDrawData(CbtGuiData.FUEL_BAR_BG, CbtGuiData.FUEL_BAR_FG, 0, 0, DrawOrientation.BOTTOM_TO_TOP)
    )

    override val id: ResourceLocation = CbTweaker.resource("fuelled_crafting")

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
            dto.useObject("progress_bar") { BarDrawData.loadPositioned(it, DEFAULT_PROGRESS_BAR) }
                ?: DEFAULT_PROGRESS_BAR,
            dto.useObject("fuel_bar") { BarDrawData.loadPositioned(it, DEFAULT_FUEL_BAR) } ?: DEFAULT_FUEL_BAR,
            dto.useAny("working_sound") { SoundData.load(it, 1F) }
        )

        val craftingRecipeSetEntry = CbTweaker.defns.recipeSets.getOrCreateRecipeSet(
            dto.expectString("recipes") ?: machine.id,
            SimpleCraftingRecipe.Type
        )
        dto.useString("jei") {
            when (it) {
                "none" -> {}
                "default" -> craftingRecipeSetEntry.registerJeiMachine(machine, null)

                "icon" -> craftingRecipeSetEntry.registerJeiMachine(
                    machine,
                    SimpleCraftingRecipe.JeiIconAdaptor(
                        craftingRecipeSetEntry.id,
                        machine.id,
                        SimpleCraftingRecipe.JeiConfig(true, config.progressBar.data)
                    )
                )

                "buffer" -> { // TODO configurable custom buffer adaptor
                    if (machine !is SingleBlockType<*>) {
                        throw SerializationException.withPath("Not a single-block machine!")
                    }
                    craftingRecipeSetEntry.registerJeiMachine(
                        machine,
                        JeiBufferAdaptor(craftingRecipeSetEntry.id, machine.id, config, machine)
                    )
                }

                else -> throw SerializationException.withPath("Unknown JEI adaptor: $it")
            }
        } ?: run {
            craftingRecipeSetEntry.registerJeiMachine(
                machine,
                (machine as? SingleBlockType<*>)?.let { JeiBufferAdaptor(craftingRecipeSetEntry.id, it.id, config, it) }
            )
        }

        val fuelRecipeSetEntry = CbTweaker.defns.recipeSets.getOrCreateRecipeSet(
            dto.expectString("fuel_recipes") ?: RecipeSetManager.BUILT_IN_FURNACE_FUEL,
            TimedFuelRecipe.Type
        )
        fuelRecipeSetEntry.registerJeiMachine(machine, null)

        val recipeDb = craftingRecipeSetEntry.database
        val fuelDb = fuelRecipeSetEntry.database
        return factory@{ world, pos, bufGroups, components, host, oldState ->
            if (
                oldState != null && oldState.config === config &&
                oldState.recipeDb === recipeDb && oldState.fuelDb === fuelDb
            ) {
                oldState.reinit(world, pos, bufGroups, components, host)
                return@factory oldState
            } else {
                return@factory sidedStrong({
                    State.Server(config, recipeDb, fuelDb, world, pos, bufGroups, components, host)
                }, {
                    State.Client(config, recipeDb, fuelDb, world, pos)
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
    override fun writeMachineConfig(state: State, dto: NBTTagCompound) {
        state.getServer().writeConfig(dto)
    }

    @ServerSide
    override fun readMachineConfig(state: State, dto: NBTTagCompound) {
        state.getServer().readConfig(dto)
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
        val modTable: Map<String, Map<String, NumberModifier.Modifier>>,
        val progressBar: Positioned<BarDrawData>,
        val fuelBar: Positioned<BarDrawData>,
        val fuelledSound: SoundData?
    )

    abstract class State(
        val config: Config,
        val recipeDb: SimpleCraftingRecipe.Database,
        val fuelDb: TimedFuelRecipe.Database,
        protected var world: World,
        protected var pos: BlockPos
    ) : WeaklyValid {
        companion object {
            private const val MOD_DURATION: String = "duration"
            private const val MOD_FUEL_DURATION: String = "fuel_duration"
        }

        val rsHandler: RedstoneControlHandler = RedstoneControlHandler(world, pos)

        protected val fuelState: ValueStateAtom<Int> = ValueStateAtom(0, IntSerializer)
        protected val maxFuelState: ValueStateAtom<Int> = ValueStateAtom(0, IntSerializer)

        val activeState: ValueStateAtom<Boolean> = ValueStateAtom(false, BoolSerializer)

        protected val uiWork: ValueStateAtom<Int> = ValueStateAtom(0, IntSerializer)
        protected val uiMaxWork: ValueStateAtom<Int> = ValueStateAtom(0, IntSerializer)

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

        private inner class UiElementImpl : SyncedUiElement {
            override val syncData: Piecewise = ListStateComposite(fuelState, maxFuelState, uiWork, uiMaxWork)

            @ClientSide.Strong
            override fun addToGuiScreen(
                uiIndex: Int,
                layout: StackLayout,
                baseSlotIndex: Int,
                wrapper: UiElementWrapper
            ) {
                config.progressBar.uiPosition.placeElement(
                    uiIndex, layout, wrapper,
                    BarControl.Progress(config.progressBar.data, { uiWork.value }, { uiMaxWork.value })
                )
                config.fuelBar.uiPosition.placeElement(
                    uiIndex, layout, wrapper,
                    BarControl.Progress(config.fuelBar.data, { fuelState.value }, { maxFuelState.value })
                )
            }
        }

        @ServerSide
        class Server(
            config: Config,
            recipeDb: SimpleCraftingRecipe.Database,
            fuelDb: TimedFuelRecipe.Database,
            world: World,
            pos: BlockPos,
            private var bufGroups: BufferGroups,
            components: ComponentSet,
            private var host: MachineHost
        ) : State(config, recipeDb, fuelDb, world, pos), CopiableConfigHost, NbtCompoundSerializable {
            companion object {
                private const val SER_REDSTONE: String = "redstone"
                private const val SER_FUEL: String = "fuel"
                private const val SER_MAX_FUEL: String = "max_fuel"
            }

            private var modState: SimpleCraftingBehaviour.ModState =
                SimpleCraftingBehaviour.computeModifierState(config.modTable, components)
            private var cachedFuelRecipe: TimedFuelRecipe? = null
            private val executor: Executor = Executor()
            private var stateDirty: Boolean = true

            init {
                rsHandler.observeState(Observer.Simple.fixed {
                    stateDirty = true
                })
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
                this.modState = SimpleCraftingBehaviour.computeModifierState(config.modTable, components)
                this.host = host
                executor.updateCurrentRecipeWorkNeeded()
                executor.notifyRecipeDirty()
            }

            @ServerSide
            override fun getServer(): Server = this

            @ClientSide.Strong
            override fun getClient(): Client = absurdLogicalSide()

            fun handleBlockUpdate() {
                rsHandler.updateRedstoneState(world, pos)
            }

            override fun notifyState(newComponents: ComponentSet?) {
                if (newComponents != null) {
                    modState = SimpleCraftingBehaviour.computeModifierState(config.modTable, newComponents)
                    executor.updateCurrentRecipeWorkNeeded()
                }
                executor.notifyRecipeDirty()
            }

            override fun tick(ticker: TickModulator) {
                val fuelValue = fuelState.value
                if (fuelValue > 0) {
                    fuelState.update(fuelValue - 1)
                } else {
                    activeState.update(false)
                }
                if (rsHandler.canWork()) {
                    executor.tick(ticker)
                }
                if (stateDirty) {
                    host.onMachineStateChanged()
                    stateDirty = false
                }
            }

            private fun checkOrBurnFuel(accs: LazyAccumulatorMap, consumeFactors: Object2FloatMap<String>): Boolean {
                if (fuelState.value > 0) return true
                cachedFuelRecipe?.let {
                    if (it.inputTable.checkInputs(accs, consumeFactors, MatcherChecker.Initial)) {
                        burnFuel(it, accs, consumeFactors)
                        return true
                    }
                    cachedFuelRecipe = null
                }
                val recipe = fuelDb.recipeMap.values.firstOrNull {
                    it.inputTable.checkInputs(accs, consumeFactors, MatcherChecker.Initial)
                } ?: return false
                cachedFuelRecipe = recipe
                burnFuel(recipe, accs, consumeFactors)
                return true
            }

            private fun burnFuel(
                recipe: TimedFuelRecipe,
                accs: LazyAccumulatorMap,
                consumeFactors: Object2FloatMap<String>
            ) {
                recipe.inputTable.useInputs(accs, consumeFactors, MatcherConsumer.Initial)
                val modifiedDuration = modState.modTable.tryModifyIntFloor(MOD_FUEL_DURATION, recipe.duration)
                fuelState.update(modifiedDuration)
                maxFuelState.update(modifiedDuration)
            }

            override fun writeConfig(dto: NBTTagCompound) {
                dto.runAction {
                    SER_REDSTONE tag rsHandler.writeToNbt()
                }
            }

            override fun readConfig(dto: NBTTagCompound) {
                dto.getStringOrNull(SER_REDSTONE)?.let {
                    rsHandler.readFromNbt(it)
                }
            }

            override fun writeToNbt(dto: NBTTagCompound) {
                dto.runAction {
                    SER_REDSTONE tag rsHandler.writeToNbt()
                    SER_FUEL int fuelState.value
                    SER_MAX_FUEL int maxFuelState.value
                }
                executor.writeToNbt(dto)
            }

            override fun readFromNbt(dto: NBTTagCompound) {
                rsHandler.readFromNbt(dto.getString(SER_REDSTONE))
                fuelState.setValueFromSync(dto.getInteger(SER_FUEL))
                activeState.update(fuelState.value > 0)
                maxFuelState.setValueFromSync(dto.getInteger(SER_FUEL))
                executor.readFromNbt(dto)
            }

            private inner class Executor : SimpleCraftingRecipe.Executor() {
                override val debugName: String
                    get() = host.machineType.id

                override fun getBuffers(): BufferGroups = bufGroups

                override fun getConsumeFactors(): Object2FloatMap<String> = modState.consumeFactors

                override fun getRecipeById(recipeId: String): SimpleCraftingRecipe? = recipeDb.recipeMap[recipeId]

                override fun getRecipeDuration(recipe: SimpleCraftingRecipe): Int =
                    modState.modTable.tryModifyIntCeil(MOD_DURATION, recipe.duration)

                override fun findRecipe(
                    accs: LazyAccumulatorMap,
                    consumeFactors: Object2FloatMap<String>
                ): SimpleCraftingRecipe? = recipeDb.recipeMap.values.firstOrNull {
                    it.inputTable.checkInputs(accs, consumeFactors, MatcherChecker.Initial) &&
                        it.outputTable.checkOutputs(accs, ProviderChecker.Final)
                }

                override fun canStartRecipe(
                    recipe: SimpleCraftingRecipe,
                    accs: LazyAccumulatorMap,
                    consumeFactors: Object2FloatMap<String>
                ): Boolean = checkOrBurnFuel(accs, consumeFactors)

                override fun canProgressRecipe(
                    recipe: SimpleCraftingRecipe,
                    accs: LazyAccumulatorMap,
                    consumeFactors: Object2FloatMap<String>,
                    workNeeded: Int,
                    workDone: Int
                ): Boolean = checkOrBurnFuel(accs, consumeFactors)

                override fun updateWork(newWork: Int) {
                    uiWork.update(newWork)
                }

                override fun updateMaxWork(newMaxWork: Int) {
                    uiMaxWork.update(newMaxWork)
                }

                override fun onStateChanged() {
                    stateDirty = true
                }
            }
        }

        @ClientSide.Strong
        class Client(
            config: Config,
            recipeDb: SimpleCraftingRecipe.Database,
            fuelDb: TimedFuelRecipe.Database,
            world: World,
            pos: BlockPos,
        ) : State(config, recipeDb, fuelDb, world, pos) {
            private val fuelledSound: MachineSoundWrapper?

            init {
                val soundData = config.fuelledSound
                if (soundData != null) {
                    fuelledSound = MachineSoundWrapper(this, pos, soundData)
                    activeState.observeAll(Observer.Simple.fixed {
                        fuelledSound.setActive(activeState.value)
                    })
                } else {
                    fuelledSound = null
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
                fuelledSound?.setPosition(pos)
            }

            @ServerSide
            override fun getServer(): Server = absurdLogicalSide()

            @ClientSide.Strong
            override fun getClient(): Client = this
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
            val progBar = config.progressBar.data
            val progBg = progBar.bgTexture.drawable
            val progPos = config.progressBar.uiPosition.computePosition(region, progBg.width, progBg.height)
            container.addJeiUiElement(
                JeiProgressBarElement(progPos.x, progPos.y, progBar, recipe.duration, jeiHelpers.guiHelper)
            )

            val fuelBar = config.fuelBar.data
            val fuelBg = fuelBar.bgTexture.drawable
            val fuelPos = config.fuelBar.uiPosition.computePosition(region, fuelBg.width, fuelBg.height)
            container.addJeiUiElement(
                JeiProgressBarElement(fuelPos.x, fuelPos.y, fuelBar, 32, jeiHelpers.guiHelper, true)
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
