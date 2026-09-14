package st.evening.mc.cbtweaker

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraft.util.SoundEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import org.apache.logging.log4j.Logger
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.impl.SimpleCraftingBehaviour
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.buffer.BufferTypeRegistry
import st.evening.mc.cbtweaker.buffer.impl.FluidBuffer
import st.evening.mc.cbtweaker.buffer.impl.ForgeEnergyBuffer
import st.evening.mc.cbtweaker.buffer.impl.ItemComponentBuffer
import st.evening.mc.cbtweaker.buffer.impl.ItemStackBuffer
import st.evening.mc.cbtweaker.compat.CbtCompat
import st.evening.mc.cbtweaker.event.CbtIngredientHandlerRegistrationEvent
import st.evening.mc.cbtweaker.event.CbtRegistrationEvent
import st.evening.mc.cbtweaker.hatch.HatchContainer
import st.evening.mc.cbtweaker.hatch.HatchManager
import st.evening.mc.cbtweaker.hatch.HatchTileEntity
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerContainer
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerTileEntity
import st.evening.mc.cbtweaker.multiblock.MultiBlockManager
import st.evening.mc.cbtweaker.network.C2SInteractTankTransfer
import st.evening.mc.cbtweaker.network.C2SSetBufferAutoExporting
import st.evening.mc.cbtweaker.network.C2SSetBufferSideEnabled
import st.evening.mc.cbtweaker.network.C2SSetHatchAutoExporting
import st.evening.mc.cbtweaker.network.C2SSetRedstoneBehaviour
import st.evening.mc.cbtweaker.network.C2SVisualizationLevel
import st.evening.mc.cbtweaker.network.S2CBindMultiBlockAssembly
import st.evening.mc.cbtweaker.network.S2CClientEffect
import st.evening.mc.cbtweaker.recipe.RecipeSetManager
import st.evening.mc.cbtweaker.serconfig.ConfigCopierItem
import st.evening.mc.cbtweaker.singleblock.SingleBlockMachineContainer
import st.evening.mc.cbtweaker.singleblock.SingleBlockMachineTileEntity
import st.evening.mc.cbtweaker.singleblock.SingleBlockManager
import st.evening.mc.cbtweaker.structure.StructureMatcherType
import st.evening.mc.cbtweaker.structure.VisualizationRenderer
import st.evening.mc.cbtweaker.structure.VisualizationToolItem
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcherType
import st.evening.mc.cbtweaker.structure.block.impl.AirStructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.impl.BlockTypeStructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.impl.HatchStructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.impl.OreDictionaryStructureBlockMatcher
import st.evening.mc.cbtweaker.structure.impl.LinearStructureMatcher
import st.evening.mc.cbtweaker.structure.impl.SimpleStructureMatcher
import st.evening.mc.cbtweaker.template.TemplateManager
import st.evening.mc.cbtweaker.util.EventRegistry
import st.evening.mc.cbtweaker.util.capability.CapabilityMerger
import st.evening.mc.cbtweaker.world.RoiTracker
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.PreludeMod
import st.evening.mc.prelude.api.config.json.JsonConfig
import st.evening.mc.prelude.api.config.json.defaultInit
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.newModLogger
import st.evening.mc.prelude.api.registration.BlockRegistrar
import st.evening.mc.prelude.api.registration.ContainerType
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.TileEntityType
import st.evening.mc.prelude.api.registration.c2sPacket
import st.evening.mc.prelude.api.registration.container
import st.evening.mc.prelude.api.registration.creativeTab
import st.evening.mc.prelude.api.registration.item
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.registration.s2cPacket
import st.evening.mc.prelude.api.registration.soundEvent
import st.evening.mc.prelude.api.registration.tileEntity
import st.evening.mc.prelude.api.util.game.onPhysicalClient
import st.evening.mc.prelude.loader.PreludeLoader

@PreludeLoader.Load(CbtConsts.MOD_ID)
object CbTweaker : PreludeMod<CbtDefinitions> {
    override val modId: String = CbtConsts.MOD_ID
    private val configData: JsonConfig = JsonConfig(CbtConsts.MOD_ID)
    val config: CbtConfig = CbtConfig(configData.root)
    val defns: CbtDefinitions by PreludeLoader.inject()
    val logger: Logger = newModLogger()

    override fun register(reg: ModRegistrar): CbtDefinitions {
        configData.defaultInit(reg)
        return CbtDefinitions(reg)
    }
}

class CbtDefinitions(reg: ModRegistrar) {
    // REGISTRIES ======================================================================================================

    val bufferTypes: BufferTypeRegistry = BufferTypeRegistry()
    val machineBehaviours: EventRegistry.Simple<MachineBehaviour<*>> = EventRegistry.Simple("machine behaviour")
    val structureBlockMatchers: EventRegistry.Simple<StructureBlockMatcherType> =
        EventRegistry.Simple("structure block matcher")
    val structureMatchers: EventRegistry.Simple<StructureMatcherType> = EventRegistry.Simple("structure matcher")

    init {
        val builtInsConfig = CbTweaker.config.builtIns
        if (builtInsConfig.loadBuiltInBuffers) {
            reg.on<CbtRegistrationEvent<BufferType<*, *, *, *>>> { event ->
                event.register(ItemStackBuffer.Type)
                event.register(ItemComponentBuffer.Type)
                event.register(FluidBuffer.Type)
                event.register(ForgeEnergyBuffer.Type)
            }
            reg.on<CbtIngredientHandlerRegistrationEvent<ItemStackBuffer.Accumulator, ItemStackBuffer.JeiAccumulator>> { event ->
                when (event.bufferType) {
                    ItemStackBuffer.Type -> {
                        event.registerMatcherType(ItemStackBuffer.ItemMatcher.Type)
                        event.registerMatcherType(ItemStackBuffer.OreDictionaryMatcher.Type)
                        event.registerProviderType(ItemStackBuffer.ItemProvider.Type)
                    }
                }
            }
            reg.on<CbtIngredientHandlerRegistrationEvent<FluidBuffer.Accumulator, FluidBuffer.JeiAccumulator>> { event ->
                when (event.bufferType) {
                    FluidBuffer.Type -> {
                        event.registerMatcherType(FluidBuffer.FluidMatcher.Type)
                        event.registerMatcherType(FluidBuffer.FluidRateMatcher.Type)
                        event.registerProviderType(FluidBuffer.FluidProvider.Type)
                        event.registerProviderType(FluidBuffer.FluidRateProvider.Type)
                    }
                }
            }
            reg.on<CbtIngredientHandlerRegistrationEvent<ForgeEnergyBuffer.Accumulator, ForgeEnergyBuffer.JeiAccumulator>> { event ->
                when (event.bufferType) {
                    ForgeEnergyBuffer.Type -> {
                        event.registerMatcherType(ForgeEnergyBuffer.EnergyMatcher.Type)
                        event.registerMatcherType(ForgeEnergyBuffer.PowerMatcher.Type)
                        event.registerProviderType(ForgeEnergyBuffer.EnergyProvider.Type)
                        event.registerProviderType(ForgeEnergyBuffer.PowerProvider.Type)
                    }
                }
            }
        }
        if (builtInsConfig.loadBuiltInMachineBehaviours) {
            reg.on<CbtRegistrationEvent<MachineBehaviour<*>>> { event ->
                event.register(SimpleCraftingBehaviour)
            }
        }
        if (builtInsConfig.loadBuiltInStructureBlockMatchers) {
            reg.on<CbtRegistrationEvent<StructureBlockMatcherType>> { event ->
                event.register(HatchStructureBlockMatcher.Type)
                event.register(AirStructureBlockMatcher.Type)
                event.register(BlockTypeStructureBlockMatcher.Type)
                event.register(OreDictionaryStructureBlockMatcher.Type)
            }
        }
        if (builtInsConfig.loadBuiltInStructureMatchers) {
            reg.on<CbtRegistrationEvent<StructureMatcherType>> { event ->
                event.register(SimpleStructureMatcher.Type)
                event.register(LinearStructureMatcher.Type)
            }
        }
    }

    // DATA MANAGERS ===================================================================================================

    val templates: TemplateManager
    val hatches: HatchManager
    val singleBlocks: SingleBlockManager
    val multiBlocks: MultiBlockManager
    val recipeSets: RecipeSetManager

    init {
        val configDir = Loader.instance().configDir.toPath().resolve(CbtConsts.MOD_ID)
        templates = TemplateManager(configDir.resolve("templates"))
        hatches = HatchManager(configDir.resolve("hatches"), reg)
        singleBlocks = SingleBlockManager(configDir.resolve("singleblocks"), reg)
        multiBlocks = MultiBlockManager(configDir.resolve("multiblocks"), reg)
        recipeSets = RecipeSetManager(configDir.resolve("recipes"))
    }

    // GAME STATE ======================================================================================================

    val roiTracker: RoiTracker = RoiTracker(reg)

    // REGISTRY OBJECTS ================================================================================================

    val soundConfigCopy: SoundEvent by reg.soundEvent("notification.config_copy")
    val soundConfigPaste: SoundEvent by reg.soundEvent("notification.config_paste")
    val soundGasTransfer: SoundEvent by reg.soundEvent("material.gas.transfer")

    val tileEntityHatch: TileEntityType<HatchTileEntity> by reg.tileEntity("hatch")
    val tileEntitySingleBlockMachine: TileEntityType<SingleBlockMachineTileEntity> by reg.tileEntity("sb_machine")
    val tileEntityMultiBlockController: TileEntityType<MultiBlockControllerTileEntity> by reg.tileEntity("mb_ctrl")

    val creativeTab: CreativeTabs = reg.creativeTab { ItemStack(itemVisualizationTool) }
    val itemVisualizationTool: VisualizationToolItem by reg.item("vis_tool") {
        VisualizationToolItem().also {
            it.creativeTab = creativeTab
        }
    }
    val itemConfigCopier: ConfigCopierItem by reg.item("config_copier") {
        ConfigCopierItem().also {
            it.creativeTab = creativeTab
        }
    }

    val containerHatch: ContainerType by reg.container(HatchContainer.Factory)
    val containerSingleBlockMachine: ContainerType by reg.container(SingleBlockMachineContainer.Factory)
    val containerMultiBlockController: ContainerType by reg.container(MultiBlockControllerContainer.Factory)

    val c2sVisualizationLevel: PacketType.C2S<C2SVisualizationLevel>
        by reg.c2sPacket(C2SVisualizationLevel.Serializer, C2SVisualizationLevel.Handler)
    val c2sSetHatchAutoExporting: PacketType.C2S<C2SSetHatchAutoExporting>
        by reg.c2sPacket(C2SSetHatchAutoExporting.Serializer, C2SSetHatchAutoExporting.Handler)
    val c2sSetBufferAutoExporting: PacketType.C2S<C2SSetBufferAutoExporting>
        by reg.c2sPacket(C2SSetBufferAutoExporting.Serializer, C2SSetBufferAutoExporting.Handler)
    val c2sSetBufferSideEnabled: PacketType.C2S<C2SSetBufferSideEnabled>
        by reg.c2sPacket(C2SSetBufferSideEnabled.Serializer, C2SSetBufferSideEnabled.Handler)
    val c2sSetRedstoneBehaviour: PacketType.C2S<C2SSetRedstoneBehaviour>
        by reg.c2sPacket(C2SSetRedstoneBehaviour.Serializer, C2SSetRedstoneBehaviour.Handler)
    val c2sInteractTankTransfer: PacketType.C2S<C2SInteractTankTransfer>
        by reg.c2sPacket(C2SInteractTankTransfer.Serializer, C2SInteractTankTransfer.Handler)

    val s2cClientEffect: PacketType.S2C<S2CClientEffect>
        by reg.s2cPacket(S2CClientEffect.Serializer, S2CClientEffect.Handler)
    val s2cBindMultiBlockAssembly: PacketType.S2C<S2CBindMultiBlockAssembly>
        by reg.s2cPacket(S2CBindMultiBlockAssembly.Serializer, S2CBindMultiBlockAssembly.Handler)

    init {
        @OptIn(PreludeInternal::class)
        reg.getState(BlockRegistrar.TARGET) // ensure the listeners are registered before the registry events are fired
        // we want this to run after pre-init but before proper block registration
        reg.on<RegistryEvent.Register<Block>>(priority = EventPriority.HIGHEST) {
            bufferTypes.init()
            machineBehaviours.init()
            structureBlockMatchers.init()
            structureMatchers.init()
            templates.loadPreInit()
            hatches.loadAll()
            singleBlocks.preloadAll()
            multiBlocks.preloadAll()

            onPhysicalClient {
                if (CbTweaker.config.dataGen.genBlockModels) {
                    val mcDir = Minecraft.getMinecraft().gameDir.toPath()
                    val resourceDir = mcDir.resolve(CbTweaker.config.dataGen.resourceDir)
                    if (!resourceDir.startsWith(mcDir)) {
                        throw IllegalArgumentException("Resource directory must be in the game directory: $resourceDir")
                    }
                    hatches.dataGenBlockModels(resourceDir)
                    singleBlocks.dataGenBlockModels(resourceDir)
                    multiBlocks.dataGenBlockModels(resourceDir)
                }
            }
        }
        reg.on<FMLInitializationEvent> {
            CapabilityMerger.init()
            templates.loadInit()
            singleBlocks.loadAll()
            multiBlocks.loadAll()
            recipeSets.loadRecipes()
        }
        onPhysicalClient {
            reg.on<RenderWorldLastEvent> { event ->
                VisualizationRenderer.renderInWorldVisualization(event.partialTicks)
            }
        }
        CbtCompat.init(reg)
    }
}
