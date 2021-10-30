package xyz.phanta.cbtweaker;

import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.util.item.CustomModelItem;

import java.util.ArrayList;
import java.util.List;

public class ClientItemBlockRegistrar extends ItemBlockRegistrar {

    private final List<Pair<Item, String>> itemModelRegQueue = new ArrayList<>();

    @Override
    public void addItemRegistration(String name, Item item) {
        super.addItemRegistration(name, item);
        itemModelRegQueue.add(Pair.of(item, name));
    }

    @SubscribeEvent
    public void onRegisterModels(ModelRegistryEvent event) {
        for (Pair<Item, String> entry : itemModelRegQueue) {
            Item item = entry.getLeft();
            if (item instanceof CustomModelItem) {
                ((CustomModelItem)item).collectItemModels((m, p, v) -> ModelLoader.setCustomModelResourceLocation(
                        item, m, CbtMod.INSTANCE.newModelResourceLocation(p, v)));
            } else {
                ModelLoader.setCustomModelResourceLocation(
                        item, 0, CbtMod.INSTANCE.newModelResourceLocation(entry.getRight(), "inventory"));
            }
        }
    }

}
