package xyz.phanta.cbtweaker.integration.mekanism.buffer;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.event.ModDependentEventBusSubscriber;
import io.github.phantamanta44.libnine.util.TriBool;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasItem;
import mekanism.client.jei.MekanismJEI;
import mekanism.common.base.target.GasHandlerTarget;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.util.EmitUtils;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.recipe.IIngredientType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.integration.mekanism.MekanismIntegration;
import xyz.phanta.cbtweaker.integration.mekanism.RestrictedGasTank;
import xyz.phanta.cbtweaker.integration.mekanism.SingleGasTank;
import xyz.phanta.cbtweaker.integration.mekanism.gui.InteractiveGasTankGuiComponent;
import xyz.phanta.cbtweaker.integration.mekanism.jei.JeiMekanismGasIngredient;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.TickModulator;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

@ModDependentEventBusSubscriber(dependencies = MekanismIntegration.MOD_ID)
public class MekanismGasBuffer implements SingleGasTank, ISerializable {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("mekanism_gas");

    private final World world;
    private final BlockPos bufPos;

    private final int capacity;
    @Nullable
    private GasStack storedGas = null;
    private final SingleGasTank restrictedTank;
    private final boolean allowInsert, allowExtract, allowAutoExport;
    @Nullable
    private final BufferObserver observer;

    private final Positioning uiPosition;
    private final TextureRegion bgTexture;
    private final int barOffsetX, barOffsetY;

    public MekanismGasBuffer(World world, BlockPos bufPos,
                             int capacity, boolean allowInsert, boolean allowExtract, boolean allowAutoExport,
                             Positioning uiPosition, TextureRegion bgTexture, int barOffsetX, int barOffsetY,
                             @Nullable BufferObserver observer) {
        this.world = world;
        this.bufPos = bufPos;
        this.capacity = capacity;
        this.restrictedTank = new RestrictedGasTank(this, allowInsert, allowExtract);
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

    public SingleGasTank getRestrictedTank() {
        return restrictedTank;
    }

    public boolean allowsAutoExport() {
        return allowAutoExport;
    }

    @Nullable
    @Override
    public Gas getGasType() {
        return storedGas != null ? storedGas.getGas() : null;
    }

    @Nullable
    @Override
    public GasStack getGas() {
        return storedGas;
    }

    public void setGas(@Nullable GasStack gas) {
        if (gas == null || gas.amount <= 0) {
            if (storedGas == null) {
                return;
            }
            storedGas = null;
        } else {
            if (storedGas != null && storedGas.isGasEqual(gas) && storedGas.amount == gas.amount) {
                return;
            }
            storedGas = gas;
        }
        if (observer != null) {
            observer.onIngredientsChanged();
        }
    }

    @Override
    public int getStored() {
        return storedGas != null ? storedGas.amount : 0;
    }

    @Override
    public int getMaxGas() {
        return capacity;
    }

    @Override
    public int receiveGas(EnumFacing face, GasStack gasStack, boolean commit) {
        if (gasStack.amount <= 0) {
            return 0;
        }
        int storedAmount;
        if (storedGas == null) {
            storedAmount = 0;
        } else if (storedGas.isGasEqual(gasStack)) {
            storedAmount = storedGas.amount;
        } else {
            return 0;
        }
        int toTransfer = Math.min(gasStack.amount, capacity - storedAmount);
        if (toTransfer <= 0) {
            return 0;
        }
        if (commit) {
            setGas(gasStack.copy().withAmount(storedAmount + toTransfer));
        }
        return toTransfer;
    }

    @Nullable
    @Override
    public GasStack drawGas(EnumFacing face, int amount, boolean commit) {
        if (amount <= 0 || storedGas == null) {
            return null;
        }
        int toTransfer = Math.min(amount, storedGas.amount); // must be positive
        GasStack transferred = storedGas.copy().withAmount(toTransfer);
        if (commit) {
            setGas(storedGas.copy().withAmount(storedGas.amount - toTransfer));
        }
        return transferred;
    }

    @Override
    public boolean canReceiveGas(EnumFacing face, Gas gas) {
        return storedGas == null || storedGas.getGas() == gas;
    }

    @Override
    public boolean canDrawGas(EnumFacing face, Gas gas) {
        return storedGas != null && storedGas.getGas() == gas;
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

    public MekanismGasBuffer copy(@Nullable BufferObserver observer) {
        MekanismGasBuffer buf = new MekanismGasBuffer(world, bufPos, capacity, allowInsert, allowExtract, allowAutoExport,
                uiPosition, bgTexture, barOffsetX, barOffsetY, observer);
        buf.storedGas = storedGas;
        return buf;
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        if (storedGas != null) {
            storedGas.write(tag);
        }
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        if (tag.isEmpty()) {
            storedGas = null;
        } else {
            storedGas = GasStack.readFromNBT(tag);
        }
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        if (storedGas == null) {
            data.writeInt(0);
        } else {
            data.writeInt(storedGas.amount);
            data.writeVarPrecision(storedGas.getGas().getID());
        }
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        int amount = data.readInt();
        if (amount == 0) {
            storedGas = null;
        } else {
            Gas gas = GasRegistry.getGas(data.readVarPrecision());
            if (gas != null && amount > 0) {
                storedGas = new GasStack(gas, amount);
            } else {
                storedGas = null;
            }
        }
    }

    public static class Accumulator {

        private final List<MekanismGasBuffer> buffers = new ArrayList<>();
        private final Map<Gas, MultiTank> tanks = new HashMap<>();

        private void accumulate(MekanismGasBuffer buffer) {
            buffers.add(buffer);
            tanks.computeIfAbsent(buffer.getGasType(), MultiTank::new).addTank(buffer);
        }

        public Set<? extends Map.Entry<Gas, ? extends SingleGasTank>> getEntries() {
            return tanks.entrySet();
        }

        public SingleGasTank getTank(Gas gas) {
            SingleGasTank store = tanks.get(gas);
            return store != null ? store : SingleGasTank.Empty.INSTANCE;
        }

        public int insert(@Nullable GasStack stack, boolean commit) {
            if (stack == null || stack.amount <= 0) {
                return 0;
            }
            int leftToFill = stack.amount;
            SingleGasTank tank = tanks.get(stack.getGas());
            if (tank != null) {
                leftToFill -= tank.receiveGas(EnumFacing.NORTH, stack, commit);
            }
            if (leftToFill <= 0) {
                return stack.amount;
            }
            tank = tanks.get(null);
            if (tank != null) {
                leftToFill -= tank.receiveGas(EnumFacing.NORTH, stack.copy().withAmount(leftToFill), commit);
            }
            return leftToFill <= 0 ? stack.amount : (stack.amount - leftToFill);
        }

        public Accumulator copy() {
            Accumulator acc = new Accumulator();
            for (MekanismGasBuffer buffer : buffers) {
                acc.accumulate(buffer.copy(null));
            }
            return acc;
        }

        private static class MultiTank implements SingleGasTank {

            @Nullable
            private final Gas gas;
            private final List<SingleGasTank> tanks = new ArrayList<>();

            public MultiTank(@Nullable Gas gas) {
                this.gas = gas;
            }

            private void addTank(SingleGasTank store) {
                tanks.add(store);
            }

            @Nullable
            @Override
            public Gas getGasType() {
                return getStored() > 0 ? gas : null;
            }

            @Nullable
            @Override
            public GasStack getGas() {
                int amount = getStored();
                return amount > 0 ? new GasStack(gas, amount) : null;
            }

            @Override
            public int getStored() {
                return tanks.stream()
                        .filter(s -> s.getGasType() == gas)
                        .mapToInt(SingleGasTank::getStored)
                        .sum();
            }

            @Override
            public int getMaxGas() {
                return tanks.stream().mapToInt(SingleGasTank::getMaxGas).sum();
            }

            @Override
            public int receiveGas(EnumFacing face, GasStack resource, boolean commit) {
                int leftToFill = resource.amount;
                for (SingleGasTank tank : tanks) {
                    GasStack gasLeft = resource.copy();
                    gasLeft.amount = leftToFill;
                    leftToFill -= tank.receiveGas(face, gasLeft, commit);
                    if (leftToFill <= 0) {
                        return resource.amount;
                    }
                }
                return resource.amount - leftToFill;
            }

            @Override
            public boolean canReceiveGas(EnumFacing enumFacing, Gas gas) {
                return this.gas == null || this.gas == gas;
            }

            @Override
            public boolean canDrawGas(EnumFacing enumFacing, Gas gas) {
                return this.gas == gas;
            }

            @Nullable
            @Override
            public GasStack drawGas(EnumFacing face, int maxDrain, boolean commit) {
                if (maxDrain <= 0) {
                    return null;
                }
                int remAmount = maxDrain;
                for (SingleGasTank tank : tanks) {
                    if (tank.getGasType() == gas) {
                        GasStack drained = tank.drawGas(face, remAmount, commit);
                        if (drained != null) {
                            remAmount -= drained.amount;
                            if (remAmount <= 0) {
                                return new GasStack(gas, maxDrain);
                            }
                        }
                    }
                }
                int totalDrained = maxDrain - remAmount;
                return totalDrained > 0 ? new GasStack(gas, totalDrained) : null;
            }

        }

    }

    public static class JeiBuffer {

        private final int capacity;
        @Nullable
        private JeiMekanismGasIngredient contents = null;
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

        public void setContents(GasStack gas, JeiIngredient.Role role) {
            contents = new JeiMekanismGasIngredient(gas, role);
        }

        public JeiUiElement<?> createJeiUiElement(ScreenRegion contRegion) {
            ScreenRegion region = uiPosition.computeRegion(bgTexture.getWidth(), bgTexture.getHeight(), contRegion);
            ScreenRegion barRegion = new ScreenRegion(region.getX() + barOffsetX, region.getY() + barOffsetY,
                    region.getWidth() - barOffsetX * 2, region.getHeight() - barOffsetY * 2);
            return new JeiUiElement<GasStack>() {
                @Nullable
                @Override
                public JeiIngredient<GasStack> getIngredient() {
                    return contents;
                }

                @Override
                public IIngredientType<GasStack> getJeiIngredientType() {
                    return MekanismJEI.TYPE_GAS;
                }

                @Override
                public ScreenRegion getIngredientRegion() {
                    return barRegion;
                }

                @Override
                public void render(@Nullable GasStack ingredient) {
                    bgTexture.draw(region.getX(), region.getY());
                    if (ingredient != null) {
                        // cap capacity by amount * 2 to ensure the gas is visible
                        InteractiveGasTankGuiComponent.renderGas(ingredient, Math.min(capacity, ingredient.amount * 2),
                                barRegion.getX(), barRegion.getY(), barRegion.getWidth(), barRegion.getHeight());
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

        public boolean addIngredient(GasStack gas, JeiIngredient.Role role) {
            while (true) {
                JeiBuffer buffer = buffers.poll();
                if (buffer == null) {
                    return false;
                }
                if (buffer.isEmpty()) {
                    buffer.setContents(gas, role);
                    return true;
                }
            }
        }

    }

    public static final BufferType<MekanismGasBuffer, Accumulator, JeiBuffer, JeiAccumulator> TYPE
            = new BufferType<MekanismGasBuffer, Accumulator, JeiBuffer, JeiAccumulator>() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public Class<MekanismGasBuffer> getBufferClass() {
            return MekanismGasBuffer.class;
        }

        @Override
        public Class<Accumulator> getAccumulatorClass() {
            return Accumulator.class;
        }

        @Override
        public BufferFactory<MekanismGasBuffer, JeiBuffer> loadBufferFactory(JsonObject config) {
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
            return new BufferFactory<MekanismGasBuffer, JeiBuffer>() {
                @Override
                public MekanismGasBuffer createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer) {
                    return new MekanismGasBuffer(world, pos, capacity, allowInsert, allowExtract, allowAutoExport,
                            uiPosition, bgTexture, barOffsetX, barOffsetY, observer);
                }

                @Override
                public JeiBuffer createJeiBuffer() {
                    return new JeiBuffer(capacity, uiPosition, bgTexture, barOffsetX, barOffsetY);
                }
            };
        }

        @Override
        public void attachCapabilities(ICapabilityInstanceConsumer target, MekanismGasBuffer buffer) {
            target.accept(Capabilities.GAS_HANDLER_CAPABILITY, buffer.getRestrictedTank());
        }

        @Override
        public TriBool getDefaultExportState(MekanismGasBuffer buffer) {
            return buffer.allowsAutoExport() ? TriBool.FALSE : TriBool.NONE;
        }

        @Override
        public void doExport(MekanismGasBuffer buffer, Set<EnumFacing> faces,
                             TickModulator ticker) {
            if (faces.isEmpty()) {
                ticker.increaseIntervalUntil(20, 60);
                return;
            }
            GasStack remainingGas = buffer.getGas();
            if (remainingGas == null || remainingGas.amount <= 0) {
                ticker.increaseIntervalUntil(8, 60);
                return;
            }
            GasHandlerTarget target = new GasHandlerTarget(remainingGas);
            World world = buffer.getWorld();
            BlockPos pos = buffer.getPosition();
            for (EnumFacing face : faces) {
                TileEntity adjTile = world.getTileEntity(pos.offset(face));
                if (adjTile == null || !adjTile.hasCapability(
                        Capabilities.GAS_HANDLER_CAPABILITY, face.getOpposite())) {
                    continue;
                }
                target.addHandler(face, Objects.requireNonNull(
                        adjTile.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, face.getOpposite())));
            }
            int handlerCount = target.getHandlers().size();
            if (handlerCount <= 0) {
                ticker.increaseIntervalUntil(8, 60);
                return;
            }
            int transferred = EmitUtils.sendToAcceptors(
                    Collections.singleton(target), handlerCount, remainingGas.amount, remainingGas);
            if (transferred <= 0) {
                ticker.increaseIntervalUntil(8, 60);
                return;
            }
            buffer.drawGas(EnumFacing.NORTH, transferred, true);
            ticker.setInterval(8);
        }

        @Override
        public boolean handleInteraction(MekanismGasBuffer buffer, IBlockState state,
                                         EntityPlayer player, EnumHand hand, EnumFacing face) {
            ItemStack stack = player.getHeldItem(hand);
            if (stack.isEmpty()) {
                return false;
            }
            Item item = stack.getItem();
            if (!(item instanceof IGasItem)) {
                return false;
            }
            IGasItem gasItem = (IGasItem)item;
            GasStack stackGas = gasItem.getGas(stack);
            if (stackGas != null && stackGas.amount > 0 && buffer.canReceiveGas(face, stackGas.getGas())) {
                stackGas = gasItem.removeGas(stack, buffer.getMaxGas() - buffer.getStored());
                if (stackGas != null && stackGas.amount > 0) {
                    stackGas.amount -= buffer.receiveGas(face, stackGas, true);
                    if (stackGas.amount > 0) {
                        gasItem.addGas(stack, stackGas);
                    }
                    return true;
                }
            }
            if (stackGas != null) {
                if (buffer.canDrawGas(face, stackGas.getGas())) {
                    GasStack available = buffer.drawGas(face, gasItem.getMaxGas(stack) - stackGas.amount, false);
                    int transferred = gasItem.addGas(stack, available);
                    if (transferred > 0) {
                        buffer.drawGas(face, transferred, true);
                        return true;
                    }
                }
            } else {
                GasStack available = buffer.drawGas(face, gasItem.getMaxGas(stack), false);
                if (available == null || available.amount <= 0) {
                    return false;
                }
                int transferred = gasItem.addGas(stack, available);
                if (transferred > 0) {
                    buffer.drawGas(face, transferred, true);
                    return true;
                }
            }
            return false;
        }

        @Override
        public Accumulator createAccumulator() {
            return new Accumulator();
        }

        @Override
        public void accumulate(Accumulator acc, MekanismGasBuffer buffer) {
            acc.accumulate(buffer);
        }

        @Override
        public Accumulator copyAccumulator(Accumulator acc) {
            return acc.copy();
        }

        @Override
        public UiElement createUiElement(MekanismGasBuffer buffer) {
            return new UiElement() {
                @Override
                public ScreenRegion addToGui(ComponentAcceptor gui, int index, ScreenRegion contRegion) {
                    ScreenRegion region = buffer.getUiPosition().computeRegion(18, 18, contRegion);
                    gui.addComponent(new InteractiveGasTankGuiComponent(index, region.getX(), region.getY(),
                            buffer.getBackgroundTexture(), buffer.getBarOffsetX(), buffer.getBarOffsetY(), buffer));
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
                    Item item = heldStack.getItem();
                    if (!(item instanceof IGasItem)) {
                        return;
                    }
                    IGasItem gasItem = (IGasItem)item;
                    if (data[0] == 0) {
                        GasStack gasStack = gasItem.removeGas(heldStack, buffer.getMaxGas() - buffer.getStored());
                        if (gasStack == null || gasStack.amount <= 0) {
                            return;
                        }
                        gasStack.amount -= buffer.receiveGas(EnumFacing.NORTH, gasStack, true);
                        if (gasStack.amount > 0) {
                            gasItem.addGas(heldStack, gasStack);
                        }
                        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                                SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.5F));
                    } else {
                        GasStack available = buffer.drawGas(EnumFacing.NORTH, gasItem.getMaxGas(heldStack), false);
                        if (available == null) {
                            return;
                        }
                        int transferred = gasItem.addGas(heldStack, available);
                        if (transferred <= 0) {
                            return;
                        }
                        buffer.drawGas(EnumFacing.NORTH, transferred, true);
                        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                                SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.75F));
                    }
                    player.updateHeldItem();
                }
            };
        }

        @Override
        public void serializeBufferNbt(NBTTagCompound tag, MekanismGasBuffer buffer) {
            buffer.serNBT(tag);
        }

        @Override
        public void serializeBufferBytes(ByteUtils.Writer stream, MekanismGasBuffer buffer) {
            buffer.serBytes(stream);
        }

        @Override
        public void deserializeBufferNbt(NBTTagCompound tag, MekanismGasBuffer buffer) {
            buffer.deserNBT(tag);
        }

        @Override
        public void deserializeBufferBytes(ByteUtils.Reader stream, MekanismGasBuffer buffer) {
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

    public static class GasTypeMatcher implements IngredientMatcher<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("gas");

        private final Gas gas;
        private final int amount;
        private final boolean doConsume;

        public GasTypeMatcher(Gas gas, int amount, boolean doConsume) {
            this.gas = gas;
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
            GasStack drained = acc.get().getTank(gas).drawGas(EnumFacing.NORTH, newAmount, true);
            return drained != null && drained.amount >= newAmount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(new GasStack(gas, amount), JeiIngredient.Role.INPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(
                    new JeiMekanismGasIngredient(new GasStack(gas, amount), JeiIngredient.Role.INPUT));
        }

        public static final IngredientMatcherType<Accumulator, JeiAccumulator> TYPE
                = new IngredientMatcherType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientMatcher<Accumulator, JeiAccumulator> loadMatcher(JsonObject config) {
                return new GasTypeMatcher(
                        GasRegistry.getGas(config.get("gas").getAsString()),
                        config.has("amount") ? config.get("amount").getAsInt() : 1000,
                        !config.has("consume") || config.get("consume").getAsBoolean());
            }
        };

    }

    public static class GasTypeProvider implements IngredientProvider<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("gas");

        private final Gas gas;
        private final int amount;

        public GasTypeProvider(Gas gas, int amount) {
            this.gas = gas;
            this.amount = amount;
        }

        @Override
        public boolean insertFinal(Supplier<Accumulator> acc) {
            return acc.get().insert(new GasStack(gas, amount), true) >= amount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(new GasStack(gas, amount), JeiIngredient.Role.OUTPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(
                    new JeiMekanismGasIngredient(new GasStack(gas, amount), JeiIngredient.Role.OUTPUT));
        }

        public static final IngredientProviderType<Accumulator, JeiAccumulator> TYPE = new IngredientProviderType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientProvider<Accumulator, JeiAccumulator> loadProvider(JsonObject config) {
                return new GasTypeProvider(
                        GasRegistry.getGas(config.get("gas").getAsString()),
                        config.has("amount") ? config.get("amount").getAsInt() : 1000);
            }
        };

    }

    @SubscribeEvent
    public static void onRegisterIngredients(CbtIngredientHandlerRegistrationEvent<Accumulator, JeiAccumulator> event) {
        if (event.getBufferType() == TYPE) {
            event.registerMatcherType(GasTypeMatcher.TYPE);
            event.registerProviderType(GasTypeProvider.TYPE);
        }
    }

}
