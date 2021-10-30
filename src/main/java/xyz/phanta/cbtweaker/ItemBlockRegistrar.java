package xyz.phanta.cbtweaker;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import xyz.phanta.cbtweaker.common.CbtCustomBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ItemBlockRegistrar {

    private final List<Triple<String, Block, ? extends Function<?, Item>>> blockRegQueue = new ArrayList<>();
    private final List<Pair<String, Item>> itemRegQueue = new ArrayList<>();

    public <B extends Block> void addBlockRegistration(String name, B block,
                                                       Function<? super B, Item> blockItemFactory) {
        if (block instanceof CbtCustomBlock) {
            ((CbtCustomBlock)block).lateInit();
        }
        blockRegQueue.add(Triple.of(name, block, blockItemFactory));
    }

    public void addBlockRegistration(String name, Block block) {
        addBlockRegistration(name, block, ItemBlock::new);
    }

    public void addItemRegistration(String name, Item item) {
        itemRegQueue.add(Pair.of(name, item));
    }

    @SuppressWarnings("unchecked")
    private static <B> Item applyUnchecked(Function<B, Item> factory, Block block) {
        return factory.apply((B)block);
    }

    @SubscribeEvent
    public void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        for (Triple<String, Block, ? extends Function<?, Item>> entry : blockRegQueue) {
            String name = entry.getLeft();
            Block block = entry.getMiddle();
            block.setRegistryName(name);
            block.setTranslationKey(CbtMod.INSTANCE.prefix(name));
            block.setCreativeTab(Objects.requireNonNull(CbtMod.INSTANCE.getDefaultCreativeTab()));
            registry.register(block);
            addItemRegistration(name, applyUnchecked(entry.getRight(), block));
        }
        blockRegQueue.clear();
    }

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        for (Pair<String, Item> entry : itemRegQueue) {
            String name = entry.getLeft();
            Item item = entry.getRight();
            item.setRegistryName(name);
            item.setTranslationKey(CbtMod.INSTANCE.prefix(name));
            item.setCreativeTab(Objects.requireNonNull(CbtMod.INSTANCE.getDefaultCreativeTab()));
            registry.register(item);
        }
        itemRegQueue.clear();
    }

}
