package xyz.phanta.cbtweaker;

import io.github.phantamanta44.libnine.Virtue;
import io.github.phantamanta44.libnine.util.L9CreativeTab;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;
import xyz.phanta.cbtweaker.structure.VisualizationToolItem;

@Mod(modid = CbtMod.MOD_ID, version = CbtMod.VERSION, useMetadata = true)
public class CbtMod extends Virtue {

    public static final String MOD_ID = "cbtweaker";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MOD_ID)
    public static CbtMod INSTANCE;

    @SidedProxy(
            clientSide = "xyz.phanta.cbtweaker.CbtClientProxy",
            serverSide = "xyz.phanta.cbtweaker.CbtCommonProxy")
    public static CbtCommonProxy PROXY;

    @SuppressWarnings("NotNullFieldNotInitialized")
    public static Logger LOGGER;

    public CbtMod() {
        super(MOD_ID, new L9CreativeTab(MOD_ID, () -> new ItemStack(VisualizationToolItem.ITEM)));
    }

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        PROXY.onPreInit(event);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        PROXY.onInit(event);
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        PROXY.onPostInit(event);
    }

}
