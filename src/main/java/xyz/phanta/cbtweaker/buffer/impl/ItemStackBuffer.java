package xyz.phanta.cbtweaker.buffer.impl;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.ImpossibilityRealizedException;
import io.github.phantamanta44.libnine.util.TriBool;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import io.github.phantamanta44.libnine.util.gameobject.ItemIdentity;
import io.github.phantamanta44.libnine.util.helper.InventoryUtils;
import io.github.phantamanta44.libnine.util.math.Vec2i;
import io.github.phantamanta44.libnine.util.world.WorldUtils;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.*;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.buffer.BufferFactory;
import xyz.phanta.cbtweaker.buffer.BufferObserver;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcher;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcherType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientProvider;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientProviderType;
import xyz.phanta.cbtweaker.event.CbtIngredientHandlerRegistrationEvent;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.gui.ComponentAcceptor;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.gui.renderable.RenderableTextureRegion;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiItemIngredient;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiOreDictIngredient;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.ConsumeBehaviour;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.TickModulator;
import xyz.phanta.cbtweaker.util.helper.CbtMathUtils;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;
import xyz.phanta.cbtweaker.util.item.ItemHandlerSlot;
import xyz.phanta.cbtweaker.util.item.ItemStore;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class ItemStackBuffer implements IItemHandlerModifiable, ISerializable {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("item_stack");

    private static final Random rand = new Random();

    private final World world;
    private final BlockPos bufPos;

    private final ItemStack[] inventory;
    private final int maxStackSize;
    private final IItemHandler restrictedInventory;
    private final boolean allowInsert, allowExtract, allowAutoExport;
    @Nullable
    private final BufferObserver observer;

    private final Positioning uiPosition;
    private final boolean drawSlotBg;

    private ItemStackBuffer(World world, BlockPos bufPos, ItemStack[] inventory, int maxStackSize,
                            boolean allowInsert, boolean allowExtract, boolean allowAutoExport,
                            Positioning uiPosition, boolean drawSlotBg,
                            @Nullable BufferObserver observer) {
        this.world = world;
        this.bufPos = bufPos;
        this.inventory = inventory;
        this.maxStackSize = maxStackSize;
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
        this.restrictedInventory = InventoryUtils.restrict(this, allowInsert, allowExtract);
        this.allowAutoExport = allowAutoExport;
        this.observer = observer;
        this.uiPosition = uiPosition;
        this.drawSlotBg = drawSlotBg;
    }

    public ItemStackBuffer(World world, BlockPos pos, int size, int maxStackSize,
                           boolean allowInsert, boolean allowExtract, boolean allowAutoExport,
                           Positioning uiPosition, boolean drawSlotBg,
                           @Nullable BufferObserver observer) {
        this(world, pos, new ItemStack[size], maxStackSize, allowInsert, allowExtract, allowAutoExport,
                uiPosition, drawSlotBg, observer);
        Arrays.fill(inventory, ItemStack.EMPTY);
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPosition() {
        return bufPos;
    }

    public IItemHandler getRestrictedInventory() {
        return restrictedInventory;
    }

    public boolean allowsAutoExport() {
        return allowAutoExport;
    }

    @Override
    public int getSlots() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (ItemStack.areItemStacksEqual(inventory[slot], stack)) {
            return;
        }
        inventory[slot] = stack;
        if (observer != null) {
            observer.onIngredientsChanged();
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = inventory[slot];
        if (!slotStack.isEmpty() && !ItemHandlerHelper.canItemStacksStack(slotStack, stack)) {
            return stack;
        }
        int stackCount = stack.getCount(), slotCount = slotStack.getCount();
        int toTransfer = Math.min(stackCount, Math.min(maxStackSize, stack.getMaxStackSize()) - slotCount);
        if (toTransfer <= 0) {
            return stack;
        }
        if (!simulate) {
            setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(stack, slotCount + toTransfer));
        }
        return ItemHandlerHelper.copyStackWithSize(stack, stackCount - toTransfer);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = inventory[slot];
        if (slotStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int slotCount = slotStack.getCount();
        int toTransfer = Math.min(amount, slotCount); // must be positive
        if (!simulate) {
            setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(slotStack, slotCount - toTransfer));
        }
        return ItemHandlerHelper.copyStackWithSize(slotStack, toTransfer);
    }

    @Override
    public int getSlotLimit(int slot) {
        return maxStackSize;
    }

    public Positioning getUiPosition() {
        return uiPosition;
    }

    public boolean shouldDrawSlotBackground() {
        return drawSlotBg;
    }

    public ItemStackBuffer copy(@Nullable BufferObserver observer) {
        return new ItemStackBuffer(world, bufPos,
                inventory.clone(), maxStackSize, allowInsert, allowExtract, allowAutoExport,
                uiPosition, drawSlotBg, observer);
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        NBTTagList itemsTag = new NBTTagList();
        for (ItemStack stack : inventory) {
            itemsTag.appendTag(stack.isEmpty() ? new NBTTagCompound() : stack.serializeNBT());
        }
        tag.setTag("Items", itemsTag);
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        NBTTagList itemsTag = tag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        int limit = Math.min(inventory.length, itemsTag.tagCount());
        for (int i = 0; i < limit; i++) {
            NBTTagCompound stackTag = itemsTag.getCompoundTagAt(i);
            inventory[i] = stackTag.isEmpty() ? ItemStack.EMPTY : new ItemStack(stackTag);
        }
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        // no sync required because container slots automatically do it
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        // ditto ^
    }

    public static class Accumulator {

        private final List<ItemStackBuffer> buffers = new ArrayList<>();
        private final Map<ItemIdentity, MultiStore> stores = new HashMap<>();

        private void accumulate(ItemStackBuffer buffer) {
            buffers.add(buffer);
            for (int i = 0; i < buffer.getSlots(); i++) {
                stores.computeIfAbsent(ItemIdentity.getForStack(buffer.getStackInSlot(i)), MultiStore::new)
                        .addStore(new ItemHandlerSlot(buffer, i));
            }
        }

        public Set<? extends Map.Entry<ItemIdentity, ? extends ItemStore>> getEntries() {
            return stores.entrySet();
        }

        public ItemStore getStore(ItemIdentity ident) {
            ItemStore store = stores.get(ident);
            return store != null ? store : ItemStore.EMPTY;
        }

        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStore store = stores.get(ItemIdentity.getForStack(stack));
            if (store != null) {
                stack = store.insert(stack, simulate);
            }
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            store = stores.get(ItemIdentity.AIR);
            return store != null ? store.insert(stack, simulate) : stack;
        }

        public Accumulator copy() {
            Accumulator acc = new Accumulator();
            for (ItemStackBuffer buffer : buffers) {
                acc.accumulate(buffer.copy(null));
            }
            return acc;
        }

        private static class MultiStore implements ItemStore {

            private final ItemIdentity ident;
            private final List<ItemStore> stores = new ArrayList<>();

            public MultiStore(ItemIdentity ident) {
                this.ident = ident;
            }

            private void addStore(ItemStore store) {
                stores.add(store);
            }

            @Override
            public ItemIdentity getStoredItem() {
                return ident;
            }

            @Override
            public boolean setStoredItem(ItemIdentity ident) {
                boolean success = false;
                for (ItemStore store : stores) {
                    if (store.setStoredItem(ident)) {
                        success = true;
                    }
                }
                return success;
            }

            @Override
            public int getCount() {
                return stores.stream().filter(s -> s.getStoredItem().equals(ident)).mapToInt(ItemStore::getCount).sum();
            }

            @Override
            public ItemStack insert(ItemStack stack, boolean simulate) {
                for (ItemStore store : stores) {
                    stack = store.insert(stack, simulate);
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
                return stack;
            }

            @Override
            public ItemStack extract(int amount, boolean simulate) {
                int remAmount = amount;
                for (ItemStore store : stores) {
                    if (store.getStoredItem().equals(ident)) {
                        remAmount -= store.extract(remAmount, simulate).getCount();
                        if (remAmount <= 0) {
                            return ident.createStack(amount);
                        }
                    }
                }
                return remAmount >= amount ? ItemStack.EMPTY : ident.createStack(amount - remAmount);
            }

        }

    }

    public static class JeiBuffer {

        private final JeiIngredient<ItemStack>[] contents;
        private int nextEmptyIndex = 0;
        private final Positioning uiPosition;
        private final boolean drawSlotBg;

        @SuppressWarnings("unchecked")
        public JeiBuffer(int slotCount, Positioning uiPosition, boolean drawSlotBg) {
            this.contents = new JeiIngredient[slotCount];
            this.uiPosition = uiPosition;
            this.drawSlotBg = drawSlotBg;
        }

        public boolean hasRemainingSlots() {
            return nextEmptyIndex < contents.length;
        }

        public void setContents(JeiIngredient<ItemStack> ingredient) {
            contents[nextEmptyIndex++] = ingredient;
        }

        public Collection<JeiUiElement<?>> createJeiUiElements(ScreenRegion contRegion) {
            List<JeiUiElement<?>> elems = new ArrayList<>();
            Vec2i[] slotPosList = new Vec2i[contents.length];
            Vec2i dims = CbtMathUtils.layOutSlotGroup(18, 18, slotPosList);
            ScreenRegion region = uiPosition.computeRegion(dims.getX(), dims.getY(), contRegion);
            for (int i = 0; i < contents.length; i++) {
                Vec2i slotOffset = slotPosList[i];
                ScreenRegion slotRegion = new ScreenRegion(
                        region.getX() + slotOffset.getX(), region.getY() + slotOffset.getY(), 18, 18);
                ScreenRegion itemRegion = new ScreenRegion(slotRegion.getX() + 1, slotRegion.getY() + 1, 16, 16);
                JeiIngredient<ItemStack> slotContents = contents[i];
                elems.add(new JeiUiElement<ItemStack>() {
                    @Nullable
                    @Override
                    public JeiIngredient<ItemStack> getIngredient() {
                        return slotContents;
                    }

                    @Override
                    public IIngredientType<ItemStack> getJeiIngredientType() {
                        return VanillaTypes.ITEM;
                    }

                    @Override
                    public ScreenRegion getIngredientRegion() {
                        return itemRegion;
                    }

                    @Override
                    public void render(@Nullable ItemStack ingredient) {
                        if (drawSlotBg) {
                            CbtTextureResources.ITEM_SLOT.draw(slotRegion.getX(), slotRegion.getY());
                        }
                        if (ingredient != null) {
                            Objects.requireNonNull(slotContents)
                                    .renderIcon(itemRegion.getX(), itemRegion.getY(), ingredient);
                        }
                    }
                });
            }
            return elems;
        }

    }

    public static class JeiAccumulator {

        private final List<JeiBuffer> buffers = new LinkedList<>();

        public void accumulate(JeiBuffer buffer) {
            if (buffer.hasRemainingSlots()) {
                buffers.add(buffer);
            }
        }

        public boolean addIngredient(JeiIngredient<ItemStack> ingredient) {
            Iterator<JeiBuffer> iter = buffers.iterator();
            while (iter.hasNext()) {
                JeiBuffer buffer = iter.next();
                if (!buffer.hasRemainingSlots()) {
                    iter.remove();
                    continue;
                }
                buffer.setContents(ingredient);
                if (!buffer.hasRemainingSlots()) {
                    iter.remove();
                }
                return true;
            }
            return false;
        }

    }

    public static final BufferType<ItemStackBuffer, Accumulator, JeiBuffer, JeiAccumulator> TYPE
            = new BufferType<ItemStackBuffer, Accumulator, JeiBuffer, JeiAccumulator>() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public Class<ItemStackBuffer> getBufferClass() {
            return ItemStackBuffer.class;
        }

        @Override
        public Class<Accumulator> getAccumulatorClass() {
            return Accumulator.class;
        }

        @Override
        public BufferFactory<ItemStackBuffer, JeiBuffer> loadBufferFactory(JsonObject config) {
            int size = config.get("slots").getAsInt();
            if (size <= 0) {
                throw new ConfigException(String.format("Expected positive slot count but got %d", size));
            }
            int maxStackSize = config.has("stack_size") ? config.get("stack_size").getAsInt() : 64;
            boolean allowInsert = !config.has("allow_insert") || config.get("allow_insert").getAsBoolean();
            boolean allowExtract = !config.has("allow_extract") || config.get("allow_extract").getAsBoolean();
            boolean allowAutoExport = config.has("allow_auto_export") && config.get("allow_auto_export").getAsBoolean();
            Positioning uiPosition = config.has("position")
                    ? Positioning.fromJson(config.get("position")) : Positioning.FromCenter.CENTER;
            boolean drawSlotBg = !config.has("slot_bg") || config.get("slot_bg").getAsBoolean();
            return new BufferFactory<ItemStackBuffer, JeiBuffer>() {
                @Override
                public ItemStackBuffer createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer) {
                    return new ItemStackBuffer(world, pos,
                            size, maxStackSize, allowInsert, allowExtract, allowAutoExport,
                            uiPosition, drawSlotBg, observer);
                }

                @Override
                public JeiBuffer createJeiBuffer() {
                    return new JeiBuffer(size, uiPosition, drawSlotBg);
                }
            };
        }

        @Override
        public void attachCapabilities(ICapabilityInstanceConsumer target, ItemStackBuffer buffer) {
            target.accept(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, buffer.getRestrictedInventory());
        }

        @Override
        public TriBool getDefaultExportState(ItemStackBuffer buffer) {
            return buffer.allowsAutoExport() ? TriBool.FALSE : TriBool.NONE;
        }

        @Override
        public void doExport(ItemStackBuffer buffer, Set<EnumFacing> faces,
                             TickModulator ticker) {
            if (faces.isEmpty()) {
                ticker.increaseIntervalUntil(20, 60);
                return;
            }
            int slotIndex = 0;
            while (buffer.getStackInSlot(slotIndex).isEmpty()) {
                if (++slotIndex >= buffer.getSlots()) {
                    ticker.increaseIntervalUntil(8, 60);
                    return;
                }
            }
            World world = buffer.getWorld();
            BlockPos pos = buffer.getPosition();
            boolean workDone = false;
            for (EnumFacing face : faces) {
                TileEntity adjTile = world.getTileEntity(pos.offset(face));
                if (adjTile == null || !adjTile.hasCapability(
                        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite())) {
                    continue;
                }
                ItemStack toExport = buffer.getStackInSlot(slotIndex);
                IItemHandler adjInv = Objects.requireNonNull(
                        adjTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite()));
                ItemStack remaining = ItemHandlerHelper.insertItem(adjInv, toExport, false);
                if (toExport.getCount() != remaining.getCount()) {
                    buffer.setStackInSlot(slotIndex, remaining);
                    workDone = true;
                    if (remaining.isEmpty() && ++slotIndex >= buffer.getSlots()) {
                        break;
                    }
                }
            }
            if (workDone) {
                ticker.setInterval(8);
            } else {
                ticker.increaseIntervalUntil(8, 60);
            }
        }

        @Override
        public void dropContents(ItemStackBuffer buffer) {
            World world = buffer.getWorld();
            BlockPos pos = buffer.getPosition();
            for (int i = 0; i < buffer.getSlots(); i++) {
                ItemStack stack = buffer.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    WorldUtils.dropItem(world, pos, stack);
                }
            }
        }

        @Override
        public Accumulator createAccumulator() {
            return new Accumulator();
        }

        @Override
        public void accumulate(Accumulator acc, ItemStackBuffer buffer) {
            acc.accumulate(buffer);
        }

        @Override
        public Accumulator copyAccumulator(Accumulator acc) {
            return acc.copy();
        }

        @Override
        public UiElement createUiElement(ItemStackBuffer buffer) {
            Vec2i[] slotPosList = new Vec2i[buffer.getSlots()];
            Vec2i dims = CbtMathUtils.layOutSlotGroup(18, 18, slotPosList);
            return new UiElement() {
                @Override
                public void addToContainer(Consumer<Slot> slotAdder, int index,
                                           ScreenRegion contRegion) {
                    Vec2i slotBasePos = buffer.getUiPosition().computePosition(dims.getX(), dims.getY(), contRegion);
                    int basePosX = slotBasePos.getX() + 1, basePosY = slotBasePos.getY() + 1;
                    for (int i = 0; i < slotPosList.length; i++) {
                        Vec2i slotPos = slotPosList[i];
                        slotAdder.accept(
                                new SlotItemHandler(buffer, i, basePosX + slotPos.getX(), basePosY + slotPos.getY()));
                    }
                }

                @Override
                public ScreenRegion addToGui(ComponentAcceptor gui, int index, ScreenRegion contRegion) {
                    ScreenRegion region = buffer.getUiPosition().computeRegion(dims.getX(), dims.getY(), contRegion);
                    if (buffer.shouldDrawSlotBackground()) {
                        for (Vec2i slotPos : slotPosList) {
                            gui.addBackgroundRender(new RenderableTextureRegion(
                                    region.getX() + slotPos.getX(), region.getY() + slotPos.getY(),
                                    CbtTextureResources.ITEM_SLOT));
                        }
                    }
                    return region;
                }
            };
        }

        @Override
        public void serializeBufferNbt(NBTTagCompound tag, ItemStackBuffer buffer) {
            buffer.serNBT(tag);
        }

        @Override
        public void serializeBufferBytes(ByteUtils.Writer stream, ItemStackBuffer buffer) {
            buffer.serBytes(stream);
        }

        @Override
        public void deserializeBufferNbt(NBTTagCompound tag, ItemStackBuffer buffer) {
            buffer.deserNBT(tag);
        }

        @Override
        public void deserializeBufferBytes(ByteUtils.Reader stream, ItemStackBuffer buffer) {
            buffer.deserBytes(stream);
        }

        @Override
        public JeiAccumulator createJeiAccumulator() {
            return new JeiAccumulator();
        }

        @Override
        public void jeiAccumulate(JeiAccumulator acc, JeiBuffer buffer) {
            acc.accumulate(buffer);
        }

        @Override
        public Collection<JeiUiElement<?>> createJeiUiElements(JeiBuffer buffer, ScreenRegion contRegion,
                                                               IJeiHelpers jeiHelpers) {
            return buffer.createJeiUiElements(contRegion);
        }
    };

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void onRegisterBufferTypes(CbtRegistrationEvent<BufferType> event) {
        event.register(TYPE);
    }

    public static class ItemIdentityMatcher implements IngredientMatcher<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("item");

        private final ItemIdentity ident;
        private final int count;
        private final ConsumeBehaviour consumeType;

        public ItemIdentityMatcher(ItemIdentity ident, int count, ConsumeBehaviour consumeType) {
            this.ident = ident;
            this.count = count;
            this.consumeType = consumeType;
        }

        @Override
        public boolean consumeInitial(Supplier<Accumulator> acc, float consumeFactor) {
            switch (consumeType) {
                case CONSUME: {
                    int newCount = Math.round(consumeFactor * count);
                    if (newCount <= 0) {
                        return true;
                    }
                    return acc.get().getStore(ident).extract(newCount, false).getCount() >= newCount;
                }
                case DAMAGE: {
                    int damage = computeItemDamage(consumeFactor);
                    if (damage <= 0) {
                        return true;
                    }
                    ItemStore store = acc.get().getStore(ident);
                    if (store.isEmpty()) {
                        return false;
                    }
                    store.setStoredItem(store.getStoredItem().mutate(s -> {
                        if (s.attemptDamageItem(damage, rand, null)) {
                            s.setCount(0);
                        }
                    }));
                    return true;
                }
                case KEEP:
                    return true;
            }
            throw new ImpossibilityRealizedException();
        }

        public static int computeItemDamage(float consumeFactor) {
            int truncated = (int)Math.floor(consumeFactor);
            if (rand.nextFloat() < (consumeFactor - truncated)) {
                return truncated + 1;
            } else {
                return truncated;
            }
        }

        private JeiItemIngredient createIngredient() {
            return new JeiItemIngredient(ident.createStack(count), JeiIngredient.Role.INPUT);
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(createIngredient());
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(createIngredient());
        }

        public static final IngredientMatcherType<Accumulator, JeiAccumulator> TYPE
                = new IngredientMatcherType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientMatcher<Accumulator, JeiAccumulator> loadMatcher(JsonObject config) {
                return new ItemIdentityMatcher(
                        DataLoadUtils.loadItemIdentity(config),
                        config.has("count") ? config.get("count").getAsInt() : 1,
                        config.has("consume") ? ConsumeBehaviour.fromString(config.get("consume").getAsString())
                                : ConsumeBehaviour.CONSUME);
            }
        };

    }

    public static class OreDictionaryMatcher implements IngredientMatcher<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("ore_dict");

        private final String oreName;
        private final int count;
        private final ConsumeBehaviour consumeType;

        public OreDictionaryMatcher(String oreName, int count, ConsumeBehaviour consumeType) {
            this.oreName = oreName;
            this.count = count;
            this.consumeType = consumeType;
        }

        @Override
        public boolean consumeInitial(Supplier<Accumulator> acc, float consumeFactor) {
            switch (consumeType) {
                case CONSUME: {
                    int remAmount = Math.round(consumeFactor * count);
                    if (remAmount <= 0) {
                        return true;
                    }
                    for (Map.Entry<ItemIdentity, ? extends ItemStore> entry : acc.get().getEntries()) {
                        ItemStore store = entry.getValue();
                        if (store.getStoredItem().matchesOreDict(oreName)) {
                            remAmount -= store.extract(remAmount, false).getCount();
                            if (remAmount <= 0) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
                case DAMAGE: {
                    int damage = ItemIdentityMatcher.computeItemDamage(consumeFactor);
                    if (damage <= 0) {
                        return true;
                    }
                    boolean didWork = false;
                    for (Map.Entry<ItemIdentity, ? extends ItemStore> entry : acc.get().getEntries()) {
                        ItemStore store = entry.getValue();
                        if (store.getStoredItem().matchesOreDict(oreName)) {
                            store.setStoredItem(store.getStoredItem().mutate(s -> {
                                if (s.attemptDamageItem(damage, rand, null)) {
                                    s.setCount(0);
                                }
                            }));
                            didWork = true;
                        }
                    }
                    return didWork;
                }
                case KEEP:
                    return true;
            }
            throw new ImpossibilityRealizedException();
        }

        private JeiOreDictIngredient createIngredient() {
            return new JeiOreDictIngredient(oreName, JeiIngredient.Role.INPUT);
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(createIngredient());
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(createIngredient());
        }

        public static final IngredientMatcherType<Accumulator, JeiAccumulator> TYPE
                = new IngredientMatcherType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientMatcher<Accumulator, JeiAccumulator> loadMatcher(JsonObject config) {
                String oreName = config.get("ore").getAsString();
                if (oreName == null) {
                    throw new ConfigException("Ore dictionary matcher must define an ore name!");
                }
                return new OreDictionaryMatcher(
                        oreName,
                        config.has("count") ? config.get("count").getAsInt() : 1,
                        config.has("consume") ? ConsumeBehaviour.fromString(config.get("consume").getAsString())
                                : ConsumeBehaviour.CONSUME);
            }
        };

    }

    public static class ItemIdentityProvider implements IngredientProvider<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("item");

        private final ItemIdentity ident;
        private final int count;

        public ItemIdentityProvider(ItemIdentity ident, int count) {
            this.ident = ident;
            this.count = count;
        }

        @Override
        public boolean insertFinal(Supplier<Accumulator> acc) {
            return acc.get().insert(ident.createStack(count), false).isEmpty();
        }

        private JeiItemIngredient createIngredient() {
            return new JeiItemIngredient(ident.createStack(count), JeiIngredient.Role.OUTPUT);
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(createIngredient());
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(createIngredient());
        }

        public static final IngredientProviderType<Accumulator, JeiAccumulator> TYPE
                = new IngredientProviderType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientProvider<Accumulator, JeiAccumulator> loadProvider(JsonObject config) {
                return new ItemIdentityProvider(DataLoadUtils.loadItemIdentity(config),
                        config.has("count") ? config.get("count").getAsInt() : 1);
            }
        };

    }

    @SubscribeEvent
    public static void onRegisterIngredients(CbtIngredientHandlerRegistrationEvent<Accumulator, JeiAccumulator> event) {
        if (event.getBufferType() == TYPE) {
            event.registerMatcherType(ItemIdentityMatcher.TYPE);
            event.registerMatcherType(OreDictionaryMatcher.TYPE);
            event.registerProviderType(ItemIdentityProvider.TYPE);
        }
    }

}
