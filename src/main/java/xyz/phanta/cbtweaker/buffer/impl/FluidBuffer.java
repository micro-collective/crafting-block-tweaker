package xyz.phanta.cbtweaker.buffer.impl;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.TriBool;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import io.github.phantamanta44.libnine.util.gameobject.FluidIdentity;
import io.github.phantamanta44.libnine.util.helper.FluidHandlerUtils;
import io.github.phantamanta44.libnine.util.render.FluidRenderUtils;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
import xyz.phanta.cbtweaker.gui.component.InteractiveFluidTankGuiComponent;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiFluidIngredient;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.TickModulator;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class FluidBuffer implements IFluidTank, ISerializable {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("fluid");

    private final World world;
    private final BlockPos bufPos;

    private final int capacity;
    @Nullable
    private FluidStack storedFluid = null;
    private final IFluidTank restrictedTank;
    private final boolean allowInsert, allowExtract, allowAutoExport;
    @Nullable
    private final BufferObserver observer;

    private final Positioning uiPosition;
    private final TextureRegion bgTexture;
    private final int barOffsetX, barOffsetY;

    public FluidBuffer(World world, BlockPos bufPos,
                       int capacity, boolean allowInsert, boolean allowExtract, boolean allowAutoExport,
                       Positioning uiPosition, TextureRegion bgTexture, int barOffsetX, int barOffsetY,
                       @Nullable BufferObserver observer) {
        this.world = world;
        this.bufPos = bufPos;
        this.capacity = capacity;
        this.restrictedTank = FluidHandlerUtils.restrict(this, allowInsert, allowExtract);
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
        this.allowAutoExport = allowAutoExport;
        this.observer = observer;
        this.uiPosition = uiPosition;
        this.bgTexture = bgTexture;
        this.barOffsetX = barOffsetX;
        this.barOffsetY = barOffsetY;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPosition() {
        return bufPos;
    }

    public IFluidTank getRestrictedTank() {
        return restrictedTank;
    }

    public boolean allowsInsert() {
        return allowInsert;
    }

    public boolean allowsExtract() {
        return allowExtract;
    }

    public boolean allowsAutoExport() {
        return allowAutoExport;
    }

    @Nullable
    public Fluid getFluidType() {
        return storedFluid != null ? storedFluid.getFluid() : null;
    }

    @Nullable
    @Override
    public FluidStack getFluid() {
        return storedFluid;
    }

    public void setFluid(@Nullable FluidStack fluid) {
        if (fluid == null || fluid.amount <= 0) {
            if (storedFluid == null) {
                return;
            }
            storedFluid = null;
        } else {
            if (storedFluid != null && storedFluid.isFluidStackIdentical(fluid)) {
                return;
            }
            storedFluid = fluid;
        }
        if (observer != null) {
            observer.onIngredientsChanged();
        }
    }

    @Override
    public int getFluidAmount() {
        return storedFluid != null ? storedFluid.amount : 0;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(storedFluid, capacity);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource.amount <= 0) {
            return 0;
        }
        int storedAmount;
        if (storedFluid == null) {
            storedAmount = 0;
        } else if (storedFluid.isFluidEqual(resource)) {
            storedAmount = storedFluid.amount;
        } else {
            return 0;
        }
        int toTransfer = Math.min(resource.amount, capacity - storedAmount);
        if (toTransfer <= 0) {
            return 0;
        }
        if (doFill) {
            setFluid(FluidHandlerUtils.copyStackWithAmount(resource, storedAmount + toTransfer));
        }
        return toTransfer;
    }

    @Nullable
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0 || storedFluid == null) {
            return null;
        }
        int toTransfer = Math.min(maxDrain, storedFluid.amount); // must be positive
        FluidStack transferred = FluidHandlerUtils.copyStackWithAmount(storedFluid, toTransfer);
        if (doDrain) {
            setFluid(FluidHandlerUtils.copyStackWithAmount(storedFluid, storedFluid.amount - toTransfer));
        }
        return transferred;
    }

    public Positioning getUiPosition() {
        return uiPosition;
    }

    public TextureRegion getBackgroundTexture() {
        return bgTexture;
    }

    public int getBarOffsetX() {
        return barOffsetX;
    }

    public int getBarOffsetY() {
        return barOffsetY;
    }

    public FluidBuffer copy(@Nullable BufferObserver observer) {
        FluidBuffer buf = new FluidBuffer(world, bufPos, capacity, allowInsert, allowExtract, allowAutoExport,
                uiPosition, bgTexture, barOffsetX, barOffsetY, observer);
        buf.storedFluid = storedFluid;
        return buf;
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        if (storedFluid != null) {
            storedFluid.writeToNBT(tag);
        }
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        if (tag.isEmpty()) {
            storedFluid = null;
        } else {
            storedFluid = FluidStack.loadFluidStackFromNBT(tag);
        }
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        if (storedFluid == null) {
            data.writeBool(false);
        } else {
            data.writeBool(true).writeFluidStack(storedFluid);
        }
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        if (data.readBool()) {
            storedFluid = data.readFluidStack();
        } else {
            storedFluid = null;
        }
    }

    public static class Accumulator {

        private final List<FluidBuffer> buffers = new ArrayList<>();
        private final Map<FluidIdentity, MultiTank> tanks = new HashMap<>();

        private void accumulate(FluidBuffer buffer) {
            buffers.add(buffer);
            tanks.computeIfAbsent(FluidIdentity.getForStack(buffer.getFluid()), MultiTank::new).addTank(buffer);
        }

        public Set<? extends Map.Entry<FluidIdentity, ? extends IFluidTank>> getEntries() {
            return tanks.entrySet();
        }

        public IFluidTank getTank(FluidIdentity ident) {
            IFluidTank store = tanks.get(ident);
            return store != null ? store : EmptyFluidHandler.INSTANCE;
        }

        public int insert(@Nullable FluidStack stack, boolean doFill) {
            if (stack == null || stack.amount <= 0) {
                return 0;
            }
            int leftToFill = stack.amount;
            IFluidTank tank = tanks.get(FluidIdentity.getForStack(stack));
            if (tank != null) {
                leftToFill -= tank.fill(stack, doFill);
            }
            if (leftToFill <= 0) {
                return stack.amount;
            }
            tank = tanks.get(FluidIdentity.EMPTY);
            if (tank != null) {
                FluidStack fluidLeft = stack.copy();
                fluidLeft.amount = leftToFill;
                leftToFill -= tank.fill(fluidLeft, doFill);
            }
            return leftToFill <= 0 ? stack.amount : (stack.amount - leftToFill);
        }

        public Accumulator copy() {
            Accumulator acc = new Accumulator();
            for (FluidBuffer buffer : buffers) {
                acc.accumulate(buffer.copy(null));
            }
            return acc;
        }

        private static class MultiTank implements IFluidTank {

            private final FluidIdentity ident;
            private final List<IFluidTank> tanks = new ArrayList<>();

            public MultiTank(FluidIdentity ident) {
                this.ident = ident;
            }

            private void addTank(IFluidTank store) {
                tanks.add(store);
            }

            @Nullable
            @Override
            public FluidStack getFluid() {
                return ident.createStack(getFluidAmount());
            }

            @Override
            public int getFluidAmount() {
                return tanks.stream()
                        .filter(s -> ident.matches(s.getFluid()))
                        .mapToInt(IFluidTank::getFluidAmount)
                        .sum();
            }

            @Override
            public int getCapacity() {
                return tanks.stream().mapToInt(IFluidTank::getCapacity).sum();
            }

            @Override
            public FluidTankInfo getInfo() {
                return new FluidTankInfo(this);
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                int leftToFill = resource.amount;
                for (IFluidTank tank : tanks) {
                    FluidStack fluidLeft = resource.copy();
                    fluidLeft.amount = leftToFill;
                    leftToFill -= tank.fill(fluidLeft, doFill);
                    if (leftToFill <= 0) {
                        return resource.amount;
                    }
                }
                return resource.amount - leftToFill;
            }

            @Nullable
            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                int remAmount = maxDrain;
                for (IFluidTank tank : tanks) {
                    if (ident.matches(tank.getFluid())) {
                        FluidStack drained = tank.drain(remAmount, doDrain);
                        if (drained != null) {
                            remAmount -= drained.amount;
                            if (remAmount <= 0) {
                                return ident.createStack(maxDrain);
                            }
                        }
                    }
                }
                return remAmount >= maxDrain ? null : ident.createStack(maxDrain - remAmount);
            }

        }

    }

    public static class JeiBuffer {

        private final int capacity;
        @Nullable
        private JeiFluidIngredient contents = null;
        private final Positioning uiPosition;
        private final TextureRegion bgTexture;
        private final int barOffsetX, barOffsetY;

        public JeiBuffer(int capacity,
                         Positioning uiPosition, TextureRegion bgTexture, int barOffsetX, int barOffsetY) {
            this.capacity = capacity;
            this.uiPosition = uiPosition;
            this.bgTexture = bgTexture;
            this.barOffsetX = barOffsetX;
            this.barOffsetY = barOffsetY;
        }

        public boolean isEmpty() {
            return contents == null;
        }

        public void setContents(FluidStack fluid, JeiIngredient.Role role) {
            contents = new JeiFluidIngredient(fluid, role);
        }

        public JeiUiElement<?> createJeiUiElement(ScreenRegion contRegion) {
            ScreenRegion region = uiPosition.computeRegion(bgTexture.getWidth(), bgTexture.getHeight(), contRegion);
            ScreenRegion barRegion = new ScreenRegion(region.getX() + barOffsetX, region.getY() + barOffsetY,
                    region.getWidth() - barOffsetX * 2, region.getHeight() - barOffsetY * 2);
            return new JeiUiElement<FluidStack>() {
                @Nullable
                @Override
                public JeiIngredient<FluidStack> getIngredient() {
                    return contents;
                }

                @Override
                public IIngredientType<FluidStack> getJeiIngredientType() {
                    return VanillaTypes.FLUID;
                }

                @Override
                public ScreenRegion getIngredientRegion() {
                    return barRegion;
                }

                @Override
                public void render(@Nullable FluidStack ingredient) {
                    bgTexture.draw(region.getX(), region.getY());
                    if (ingredient != null) {
                        FluidRenderUtils.renderFluidIntoGuiCleanly(
                                barRegion.getX(), barRegion.getY(), barRegion.getWidth(), barRegion.getHeight(),
                                ingredient, Math.min(capacity, ingredient.amount * 2)); // ensure fluid is visible
                    }
                }
            };
        }

    }

    public static class JeiAccumulator {

        private final Deque<JeiBuffer> buffers = new LinkedList<>();

        public void accumulate(JeiBuffer buffer) {
            if (buffer.isEmpty()) {
                buffers.offer(buffer);
            }
        }

        public boolean addIngredient(FluidStack fluid, JeiIngredient.Role role) {
            while (true) {
                JeiBuffer buffer = buffers.poll();
                if (buffer == null) {
                    return false;
                }
                if (buffer.isEmpty()) {
                    buffer.setContents(fluid, role);
                    return true;
                }
            }
        }

    }

    public static final BufferType<FluidBuffer, Accumulator, JeiBuffer, JeiAccumulator> TYPE
            = new BufferType<FluidBuffer, Accumulator, JeiBuffer, JeiAccumulator>() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public Class<FluidBuffer> getBufferClass() {
            return FluidBuffer.class;
        }

        @Override
        public Class<Accumulator> getAccumulatorClass() {
            return Accumulator.class;
        }

        @Override
        public BufferFactory<FluidBuffer, JeiBuffer> loadBufferFactory(JsonObject config) {
            int capacity = config.get("capacity").getAsInt();
            if (capacity <= 0) {
                throw new ConfigException(String.format("Expected positive capacity but got %d", capacity));
            }
            boolean allowInsert = !config.has("allow_insert") || config.get("allow_insert").getAsBoolean();
            boolean allowExtract = !config.has("allow_extract") || config.get("allow_extract").getAsBoolean();
            boolean allowAutoExport = config.has("allow_auto_export") && config.get("allow_auto_export").getAsBoolean();
            Positioning uiPosition = config.has("position")
                    ? Positioning.fromJson(config.get("position")) : Positioning.FromCenter.CENTER;
            TextureRegion bgTexture = config.has("bar_bg")
                    ? DataLoadUtils.loadTextureRegion(config.get("bar_bg").getAsJsonObject())
                    : CbtTextureResources.FLUID_SLOT;
            int barOffsetX = config.has("bar_offset_x") ? config.get("bar_offset_x").getAsInt() : 1;
            int barOffsetY = config.has("bar_offset_y") ? config.get("bar_offset_y").getAsInt() : 1;
            return new BufferFactory<FluidBuffer, JeiBuffer>() {
                @Override
                public FluidBuffer createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer) {
                    return new FluidBuffer(world, pos, capacity, allowInsert, allowExtract, allowAutoExport,
                            uiPosition, bgTexture, barOffsetX, barOffsetY, observer);
                }

                @Override
                public JeiBuffer createJeiBuffer() {
                    return new JeiBuffer(capacity, uiPosition, bgTexture, barOffsetX, barOffsetY);
                }
            };
        }

        @Override
        public void attachCapabilities(ICapabilityInstanceConsumer target, FluidBuffer buffer) {
            target.accept(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, FluidHandlerUtils.asFluidHandler(
                    buffer.getRestrictedTank(), buffer.allowsInsert(), buffer.allowsExtract()));
        }

        @Override
        public TriBool getDefaultExportState(FluidBuffer buffer) {
            return buffer.allowsAutoExport() ? TriBool.FALSE : TriBool.NONE;
        }

        @Override
        public void doExport(FluidBuffer buffer, Set<EnumFacing> faces,
                             TickModulator ticker) {
            if (faces.isEmpty()) {
                ticker.increaseIntervalUntil(20, 60);
                return;
            }
            FluidStack remainingFluid = buffer.getFluid();
            if (remainingFluid == null || remainingFluid.amount <= 0) {
                ticker.increaseIntervalUntil(8, 60);
                return;
            }
            IFluidHandler tankFh = FluidHandlerUtils.asFluidHandler(buffer);
            World world = buffer.getWorld();
            BlockPos pos = buffer.getPosition();
            boolean workDone = false;
            for (EnumFacing face : faces) {
                TileEntity adjTile = world.getTileEntity(pos.offset(face));
                if (adjTile == null || !adjTile.hasCapability(
                        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite())) {
                    continue;
                }
                IFluidHandler adjFh = Objects.requireNonNull(
                        adjTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face.getOpposite()));
                FluidStack transferred = FluidUtil.tryFluidTransfer(adjFh, tankFh, remainingFluid, true);
                if (transferred != null && transferred.amount >= 0) {
                    workDone = true;
                    remainingFluid.amount -= transferred.amount;
                    if (remainingFluid.amount <= 0) {
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
        public boolean handleInteraction(FluidBuffer buffer, IBlockState state,
                                         EntityPlayer player, EnumHand hand, EnumFacing face) {
            ItemStack stack = player.getHeldItem(hand);
            if (stack.isEmpty()) {
                return false;
            }
            IFluidHandler tankFh = FluidHandlerUtils.asFluidHandler(buffer);
            FluidActionResult result = FluidUtil.tryFillContainer(stack, tankFh, Integer.MAX_VALUE, player, true);
            if (result.isSuccess()) {
                deductHeldAndGiveItem(player, stack, result.getResult(), hand);
                return true;
            }
            result = FluidUtil.tryEmptyContainer(stack, tankFh, Integer.MAX_VALUE, player, true);
            if (result.isSuccess()) {
                deductHeldAndGiveItem(player, stack, result.getResult(), hand);
                return true;
            }
            return false;
        }

        private void deductHeldAndGiveItem(EntityPlayer player, ItemStack heldStack, ItemStack toGiveStack,
                                           EnumHand hand) {
            if (!player.capabilities.isCreativeMode) {
                heldStack.shrink(1);
                if (heldStack.isEmpty()) {
                    player.setHeldItem(hand, toGiveStack);
                } else if (!player.inventory.addItemStackToInventory(toGiveStack)) {
                    player.dropItem(toGiveStack, false, false);
                }
            }
        }

        @Override
        public Accumulator createAccumulator() {
            return new Accumulator();
        }

        @Override
        public void accumulate(Accumulator acc, FluidBuffer buffer) {
            acc.accumulate(buffer);
        }

        @Override
        public Accumulator copyAccumulator(Accumulator acc) {
            return acc.copy();
        }

        @Override
        public UiElement createUiElement(FluidBuffer buffer) {
            return new UiElement() {
                @Override
                public ScreenRegion addToGui(ComponentAcceptor gui, int index, ScreenRegion contRegion) {
                    TextureRegion bgTexture = buffer.getBackgroundTexture();
                    ScreenRegion region = buffer.getUiPosition().computeRegion(
                            bgTexture.getWidth(), bgTexture.getHeight(), contRegion);
                    gui.addComponent(new InteractiveFluidTankGuiComponent(index, region.getX(), region.getY(),
                            bgTexture, buffer.getBarOffsetX(), buffer.getBarOffsetY(), buffer));
                    return region;
                }

                @Override
                public void onInteraction(byte[] data, EntityPlayerMP player) {
                    if (data.length != 1) {
                        return;
                    }
                    ItemStack heldStack = player.inventory.getItemStack();
                    if (heldStack.isEmpty()) {
                        return;
                    }

                    FluidActionResult result;
                    if (data[0] == 0) {
                        result = FluidUtil.tryEmptyContainer(
                                heldStack, FluidHandlerUtils.asFluidHandler(buffer), Integer.MAX_VALUE, player, true);
                    } else {
                        FluidStack fluid = buffer.getFluid();
                        if (fluid == null || fluid.amount <= 0) {
                            return;
                        }
                        result = FluidUtil.tryFillContainer(
                                heldStack, FluidHandlerUtils.asFluidHandler(buffer), Integer.MAX_VALUE, player, true);
                    }
                    if (!result.isSuccess()) {
                        return;
                    }

                    heldStack.shrink(1);
                    player.inventory.setItemStack(heldStack);
                    ItemStack resultStack = result.getResult();
                    if (!resultStack.isEmpty()) {
                        if (heldStack.isEmpty()) {
                            player.inventory.setItemStack(resultStack);
                        } else if (!player.inventory.addItemStackToInventory(resultStack)) {
                            player.dropItem(resultStack, false, false);
                        }
                    }
                    player.updateHeldItem();
                }
            };
        }

        @Override
        public void serializeBufferNbt(NBTTagCompound tag, FluidBuffer buffer) {
            buffer.serNBT(tag);
        }

        @Override
        public void serializeBufferBytes(ByteUtils.Writer stream, FluidBuffer buffer) {
            buffer.serBytes(stream);
        }

        @Override
        public void deserializeBufferNbt(NBTTagCompound tag, FluidBuffer buffer) {
            buffer.deserNBT(tag);
        }

        @Override
        public void deserializeBufferBytes(ByteUtils.Reader stream, FluidBuffer buffer) {
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
            return Collections.singletonList(buffer.createJeiUiElement(contRegion));
        }
    };

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void onRegisterBufferTypes(CbtRegistrationEvent<BufferType> event) {
        event.register(TYPE);
    }

    public static class FluidIdentityMatcher implements IngredientMatcher<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("fluid");

        private final FluidIdentity ident;
        private final int amount;
        private final boolean doConsume;

        public FluidIdentityMatcher(FluidIdentity ident, int amount, boolean doConsume) {
            this.ident = ident;
            this.amount = amount;
            this.doConsume = doConsume;
        }

        @Override
        public boolean consumeInitial(Supplier<Accumulator> acc, float consumeFactor) {
            if (!doConsume) {
                return true;
            }
            int newAmount = Math.round(consumeFactor * amount);
            if (newAmount <= 0) {
                return true;
            }
            FluidStack drained = acc.get().getTank(ident).drain(newAmount, true);
            return drained != null && drained.amount >= newAmount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(Objects.requireNonNull(ident.createStack(amount)), JeiIngredient.Role.INPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiFluidIngredient(
                    Objects.requireNonNull(ident.createStack(amount)), JeiIngredient.Role.INPUT));
        }

        public static final IngredientMatcherType<Accumulator, JeiAccumulator> TYPE
                = new IngredientMatcherType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientMatcher<Accumulator, JeiAccumulator> loadMatcher(JsonObject config) {
                return new FluidIdentityMatcher(
                        DataLoadUtils.loadFluidIdentity(config),
                        config.has("amount") ? config.get("amount").getAsInt() : 1000,
                        !config.has("consume") || config.get("consume").getAsBoolean());
            }
        };

    }

    public static class FluidIdentityProvider implements IngredientProvider<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("fluid");

        private final FluidIdentity ident;
        private final int amount;

        public FluidIdentityProvider(FluidIdentity ident, int amount) {
            this.ident = ident;
            this.amount = amount;
        }

        @Override
        public boolean insertFinal(Supplier<Accumulator> acc) {
            return acc.get().insert(ident.createStack(amount), true) >= amount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(Objects.requireNonNull(ident.createStack(amount)), JeiIngredient.Role.OUTPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiFluidIngredient(
                    Objects.requireNonNull(ident.createStack(amount)), JeiIngredient.Role.OUTPUT));
        }

        public static final IngredientProviderType<Accumulator, JeiAccumulator> TYPE
                = new IngredientProviderType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientProvider<Accumulator, JeiAccumulator> loadProvider(JsonObject config) {
                return new FluidIdentityProvider(DataLoadUtils.loadFluidIdentity(config),
                        config.has("amount") ? config.get("amount").getAsInt() : 1000);
            }
        };

    }

    @SubscribeEvent
    public static void onRegisterIngredients(CbtIngredientHandlerRegistrationEvent<Accumulator, JeiAccumulator> event) {
        if (event.getBufferType() == TYPE) {
            event.registerMatcherType(FluidIdentityMatcher.TYPE);
            event.registerProviderType(FluidIdentityProvider.TYPE);
        }
    }

}
