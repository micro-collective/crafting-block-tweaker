package xyz.phanta.cbtweaker;

import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import xyz.phanta.cbtweaker.buffer.BufferTypeRegistry;
import xyz.phanta.cbtweaker.hatch.HatchManager;
import xyz.phanta.cbtweaker.multiblock.MultiBlockManager;
import xyz.phanta.cbtweaker.network.CPacketUiElementInteraction;
import xyz.phanta.cbtweaker.network.CPacketVisualizationLevel;
import xyz.phanta.cbtweaker.recipe.RecipeLogicRegistry;
import xyz.phanta.cbtweaker.singleblock.SingleBlockManager;
import xyz.phanta.cbtweaker.structure.StructureMatcherRegistry;
import xyz.phanta.cbtweaker.structure.VisualizationToolItem;
import xyz.phanta.cbtweaker.template.TemplateManager;
import xyz.phanta.cbtweaker.world.RoiTracker;

import java.nio.file.Path;

public class CbtCommonProxy {

    private final ItemBlockRegistrar registrar = createRegistrar();
    private final BufferTypeRegistry bufTypeRegistry = new BufferTypeRegistry();
    private final RecipeLogicRegistry recipeLogicRegistry = new RecipeLogicRegistry();
    private final StructureMatcherRegistry structMatcherRegistry = new StructureMatcherRegistry();
    @SuppressWarnings("NotNullFieldNotInitialized")
    private RoiTracker roiTracker;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private TemplateManager templateManager;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private HatchManager hatchManager;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private SingleBlockManager sbManager;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private MultiBlockManager mbManager;

    public void onPreInit(FMLPreInitializationEvent event) {
        Path configDir = event.getModConfigurationDirectory().toPath().resolve(CbtMod.MOD_ID);
        roiTracker = new RoiTracker();
        templateManager = new TemplateManager(configDir.resolve("templates"));
        hatchManager = new HatchManager(configDir.resolve("hatches"), registrar);
        sbManager = new SingleBlockManager(configDir.resolve("singleblocks"), registrar);
        mbManager = new MultiBlockManager(configDir.resolve("multiblocks"), registrar);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(registrar);

        registrar.addItemRegistration("vis_tool", new VisualizationToolItem());

        bufTypeRegistry.init();
        recipeLogicRegistry.init();
        structMatcherRegistry.init();
        templateManager.loadPreInit();
        hatchManager.loadAll();
        sbManager.preloadAll();
        mbManager.preloadAll();

        SimpleNetworkWrapper netHandler = CbtMod.INSTANCE.getNetworkHandler();
        netHandler.registerMessage(
                CPacketUiElementInteraction.Handler.class, CPacketUiElementInteraction.class, 0, Side.SERVER);
        netHandler.registerMessage(
                CPacketVisualizationLevel.Handler.class, CPacketVisualizationLevel.class, 1, Side.SERVER);
    }

    public BufferTypeRegistry getBufferTypes() {
        return bufTypeRegistry;
    }

    public RecipeLogicRegistry getRecipeLogics() {
        return recipeLogicRegistry;
    }

    public StructureMatcherRegistry getStructureMatchers() {
        return structMatcherRegistry;
    }

    public RoiTracker getRoiTracker() {
        return roiTracker;
    }

    public TemplateManager getTemplates() {
        return templateManager;
    }

    public HatchManager getHatches() {
        return hatchManager;
    }

    public SingleBlockManager getSingleBlocks() {
        return sbManager;
    }

    public MultiBlockManager getMultiBlocks() {
        return mbManager;
    }

    protected ItemBlockRegistrar createRegistrar() {
        return new ItemBlockRegistrar();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRegisterRecipes(RegistryEvent.Register<IRecipe> event) {
        templateManager.loadInit();
        sbManager.loadAll();
        mbManager.loadAll();
    }

    public void onInit(FMLInitializationEvent event) {
        // NO-OP
    }

    public void onPostInit(FMLPostInitializationEvent event) {
        // NO-OP
    }

}
