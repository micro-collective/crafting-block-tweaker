package xyz.phanta.cbtweaker.buffer.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.gameobject.ItemIdentity;
import io.github.phantamanta44.libnine.util.helper.OreDictUtils;
import io.github.phantamanta44.libnine.util.math.Vec2i;
import io.github.phantamanta44.libnine.util.world.WorldUtils;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.buffer.BufferObserver;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.VirtualBufferType;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.gui.ComponentAcceptor;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.gui.renderable.RenderableTextureRegion;
import xyz.phanta.cbtweaker.recipe.ComponentSet;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class ItemComponentBuffer implements IItemHandlerModifiable, ISerializable {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("item_component");

    private final World world;
    private final BlockPos bufPos;

    private final int maxStackSize;
    @Nullable
    private final Predicate<ItemStack> filter;
    private ItemStack storedStack = ItemStack.EMPTY;
    private final String componentId;
    @Nullable
    private final BufferObserver observer;

    private final Positioning uiPosition;
    private final boolean drawSlotBg;

    public ItemComponentBuffer(World world, BlockPos bufPos,
                               int maxStackSize, @Nullable Predicate<ItemStack> filter, String componentId,
                               Positioning uiPosition, boolean drawSlotBg,
                               @Nullable BufferObserver observer) {
        this.world = world;
        this.bufPos = bufPos;
        this.maxStackSize = maxStackSize;
        this.filter = filter;
        this.componentId = componentId;
        this.observer = observer;
        this.uiPosition = uiPosition;
        this.drawSlotBg = drawSlotBg;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPosition() {
        return bufPos;
    }

    public String getComponentId() {
        return componentId;
    }

    private void checkSlotIndex(int slotIndex) {
        if (slotIndex != 0) {
            throw new IndexOutOfBoundsException(
                    String.format("Component buffer has only one slot, so %d is out of bounds!", slotIndex));
        }
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        checkSlotIndex(slot);
        return storedStack;
    }

    private void setStoredStack(ItemStack stack) {
        storedStack = stack;
        if (observer != null) {
            observer.onComponentsChanged();
        }
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        checkSlotIndex(slot);
        setStoredStack(stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        checkSlotIndex(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if ((filter != null && !filter.test(stack))
                || (!storedStack.isEmpty() && !ItemHandlerHelper.canItemStacksStack(storedStack, stack))) {
            return stack;
        }
        int stackCount = stack.getCount(), storedCount = storedStack.getCount();
        int toTransfer = Math.min(stackCount, Math.min(maxStackSize, stack.getMaxStackSize()) - storedCount);
        if (toTransfer <= 0) {
            return stack;
        }
        if (!simulate) {
            setStoredStack(ItemHandlerHelper.copyStackWithSize(stack, storedCount + toTransfer));
        }
        return ItemHandlerHelper.copyStackWithSize(stack, stackCount - toTransfer);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        checkSlotIndex(slot);
        if (amount <= 0 || storedStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int storedCount = storedStack.getCount();
        int toTransfer = Math.min(amount, storedCount); // must be positive
        if (simulate) {
            return ItemHandlerHelper.copyStackWithSize(storedStack, toTransfer);
        }
        ItemStack transferredStack = ItemHandlerHelper.copyStackWithSize(storedStack, toTransfer);
        setStoredStack(ItemHandlerHelper.copyStackWithSize(storedStack, storedCount - toTransfer));
        return transferredStack;
    }

    @Override
    public int getSlotLimit(int slot) {
        return maxStackSize;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        checkSlotIndex(slot);
        return filter == null || filter.test(stack);
    }

    public Positioning getUiPosition() {
        return uiPosition;
    }

    public boolean shouldDrawSlotBackground() {
        return drawSlotBg;
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        if (!storedStack.isEmpty()) {
            storedStack.writeToNBT(tag);
        }
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        if (tag.isEmpty()) {
            storedStack = ItemStack.EMPTY;
        } else {
            storedStack = new ItemStack(tag);
        }
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        data.writeItemStack(storedStack);
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        storedStack = data.readItemStack();
    }

    public static final VirtualBufferType<ItemComponentBuffer> TYPE = new VirtualBufferType<ItemComponentBuffer>() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public Class<ItemComponentBuffer> getBufferClass() {
            return ItemComponentBuffer.class;
        }

        @Override
        public VirtualBufferFactory<ItemComponentBuffer> loadVirtualBufferFactory(JsonObject config) {
            int maxStackSize = config.has("stack_size") ? config.get("stack_size").getAsInt() : 64;
            Predicate<ItemStack> filter;
            if (config.has("filter")) {
                JsonElement filterDto = config.get("filter");
                if (filterDto.isJsonObject()) {
                    ItemIdentity ident = DataLoadUtils.loadItemIdentity(filterDto.getAsJsonObject());
                    filter = ident::matches;
                } else {
                    filter = OreDictUtils.matchesOredict(filterDto.getAsString());
                }
            } else {
                filter = null;
            }
            String componentId = config.get("comp").getAsString();
            Positioning uiPosition = config.has("position")
                    ? Positioning.fromJson(config.get("position")) : Positioning.FromCenter.CENTER;
            boolean drawSlotBg = !config.has("slot_bg") || config.get("slot_bg").getAsBoolean();
            return (w, p, o) -> new ItemComponentBuffer(
                    w, p, maxStackSize, filter, componentId, uiPosition, drawSlotBg, o);
        }

        @Override
        public void collectComponents(ComponentSet components, ItemComponentBuffer buffer) {
            components.put(buffer.getComponentId(), buffer.getStackInSlot(0).getCount());
        }

        @Override
        public void dropContents(ItemComponentBuffer buffer) {
            ItemStack stack = buffer.getStackInSlot(0);
            if (!stack.isEmpty()) {
                WorldUtils.dropItem(buffer.getWorld(), buffer.getPosition(), stack);
            }
        }

        @Override
        public UiElement createUiElement(ItemComponentBuffer buffer) {
            return new UiElement() {
                @Override
                public void addToContainer(Consumer<Slot> slotAdder, int index,
                                           ScreenRegion contRegion) {
                    Vec2i pos = buffer.getUiPosition().computePosition(18, 18, contRegion);
                    slotAdder.accept(new SlotItemHandler(buffer, 0, pos.getX() + 1, pos.getY() + 1));
                }

                @Override
                public ScreenRegion addToGui(ComponentAcceptor gui, int index, ScreenRegion contRegion) {
                    ScreenRegion region = buffer.getUiPosition().computeRegion(18, 18, contRegion);
                    if (buffer.shouldDrawSlotBackground()) {
                        gui.addBackgroundRender(new RenderableTextureRegion(
                                region.getX(), region.getY(), CbtTextureResources.ITEM_SLOT));
                    }
                    return region;
                }
            };
        }

        @Override
        public void serializeBufferNbt(NBTTagCompound tag, ItemComponentBuffer buffer) {
            buffer.serNBT(tag);
        }

        @Override
        public void serializeBufferBytes(ByteUtils.Writer stream, ItemComponentBuffer buffer) {
            buffer.serBytes(stream);
        }

        @Override
        public void deserializeBufferNbt(NBTTagCompound tag, ItemComponentBuffer buffer) {
            buffer.deserNBT(tag);
        }

        @Override
        public void deserializeBufferBytes(ByteUtils.Reader stream, ItemComponentBuffer buffer) {
            buffer.deserBytes(stream);
        }
    };

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void onRegisterBufferTypes(CbtRegistrationEvent<BufferType> event) {
        event.register(TYPE);
    }

}
