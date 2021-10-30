package xyz.phanta.cbtweaker.buffer.impl;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.TriBool;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import io.github.phantamanta44.libnine.util.helper.EnergyUtils;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import io.github.phantamanta44.libnine.util.world.BlockSide;
import mezz.jei.api.IJeiHelpers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
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
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.component.EnergyBarGuiComponent;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiForgeEnergyIngredient;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.TickModulator;
import xyz.phanta.cbtweaker.util.block.MachineSideHandler;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID)
public class ForgeEnergyBuffer implements IEnergyStorage, ISerializable {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("forge_energy");

    private final World world;
    private final BlockPos bufPos;

    private final int capacity;
    private int energyStored = 0;
    private final IEnergyStorage restrictedEnergyBuffer;
    private final int insertRate, extractRate;
    @Nullable
    private final BufferObserver observer;

    private final Positioning uiPosition;
    private final TextureRegion bgTexture, fgTexture;
    private final int barOffsetX, barOffsetY;
    private final DrawOrientation barOrientation;
    private final String energyUnitName;

    public ForgeEnergyBuffer(World world, BlockPos bufPos, int capacity, int insertRate, int extractRate,
                             Positioning uiPosition, TextureRegion bgTexture, TextureRegion fgTexture,
                             int barOffsetX, int barOffsetY, DrawOrientation barOrientation,
                             String energyUnitName, @Nullable BufferObserver observer) {
        this.world = world;
        this.bufPos = bufPos;
        this.capacity = capacity;
        this.restrictedEnergyBuffer = EnergyUtils.restrict(this, insertRate, extractRate);
        this.insertRate = insertRate;
        this.extractRate = extractRate;
        this.observer = observer;
        this.uiPosition = uiPosition;
        this.bgTexture = bgTexture;
        this.fgTexture = fgTexture;
        this.barOffsetX = barOffsetX;
        this.barOffsetY = barOffsetY;
        this.barOrientation = barOrientation;
        this.energyUnitName = energyUnitName;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPosition() {
        return bufPos;
    }

    public IEnergyStorage getRestrictedEnergyBuffer() {
        return restrictedEnergyBuffer;
    }

    public int getExtractRate() {
        return extractRate;
    }

    @Override
    public int getEnergyStored() {
        return energyStored;
    }

    public void setEnergyStored(int energyStored) {
        if (this.energyStored == energyStored) {
            return;
        }
        this.energyStored = energyStored;
        if (observer != null) {
            observer.onIngredientsChanged();
        }
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int toTransfer = Math.min(maxReceive, capacity - energyStored);
        if (toTransfer <= 0) {
            return 0;
        }
        if (!simulate) {
            setEnergyStored(energyStored + toTransfer);
        }
        return toTransfer;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0 || energyStored <= 0) {
            return 0;
        }
        int toTransfer = Math.min(maxExtract, energyStored); // must be positive
        if (!simulate) {
            setEnergyStored(energyStored - toTransfer);
        }
        return toTransfer;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    public Positioning getUiPosition() {
        return uiPosition;
    }

    public TextureRegion getBackgroundTexture() {
        return bgTexture;
    }

    public TextureRegion getForegroundTexture() {
        return fgTexture;
    }

    public int getBarOffsetX() {
        return barOffsetX;
    }

    public int getBarOffsetY() {
        return barOffsetY;
    }

    public DrawOrientation getBarOrientation() {
        return barOrientation;
    }

    public String getEnergyUnitName() {
        return energyUnitName;
    }

    public ForgeEnergyBuffer copy(@Nullable BufferObserver observer) {
        ForgeEnergyBuffer buf = new ForgeEnergyBuffer(world, bufPos, capacity, insertRate, extractRate,
                uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation, energyUnitName, observer);
        buf.energyStored = energyStored;
        return buf;
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        data.writeInt(energyStored);
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        energyStored = data.readInt();
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        tag.setInteger("EnergyStored", energyStored);
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        energyStored = tag.getInteger("EnergyStored");
    }

    public static class Accumulator {

        private final List<ForgeEnergyBuffer> buffers = new ArrayList<>();
        private final IEnergyStorage energyStorage = EnergyUtils.join(buffers);

        private void accumulate(ForgeEnergyBuffer buffer) {
            buffers.add(buffer);
        }

        public int getStored() {
            return energyStorage.getEnergyStored();
        }

        public int getCapacity() {
            return energyStorage.getMaxEnergyStored();
        }

        public int insert(int maxReceive, boolean simulate) {
            return energyStorage.receiveEnergy(maxReceive, simulate);
        }

        public int extract(int maxExtract, boolean simulate) {
            return energyStorage.extractEnergy(maxExtract, simulate);
        }

        public Accumulator copy() {
            Accumulator acc = new Accumulator();
            for (ForgeEnergyBuffer buffer : buffers) {
                acc.accumulate(buffer.copy(null));
            }
            return acc;
        }

    }

    public static class JeiBuffer {

        private final int capacity;
        @Nullable
        private JeiForgeEnergyIngredient contents = null;
        private final Positioning uiPosition;
        private final TextureRegion bgTexture, fgTexture;
        private final int barOffsetX, barOffsetY;
        private final DrawOrientation barOrientation;
        private final String energyUnitName;

        public JeiBuffer(int capacity,
                         Positioning uiPosition, TextureRegion bgTexture, TextureRegion fgTexture,
                         int barOffsetX, int barOffsetY, DrawOrientation barOrientation,
                         String energyUnitName) {
            this.capacity = capacity;
            this.uiPosition = uiPosition;
            this.bgTexture = bgTexture;
            this.fgTexture = fgTexture;
            this.barOffsetX = barOffsetX;
            this.barOffsetY = barOffsetY;
            this.barOrientation = barOrientation;
            this.energyUnitName = energyUnitName;
        }

        public boolean isEmpty() {
            return contents == null;
        }

        public void setContents(int amount, boolean isRate, JeiIngredient.Role role) {
            contents = new JeiForgeEnergyIngredient(amount, isRate ? (energyUnitName + "/t") : energyUnitName, role);
        }

        public JeiUiElement<?> createJeiUiElement(ScreenRegion contRegion) {
            ScreenRegion region = uiPosition.computeRegion(bgTexture.getWidth(), bgTexture.getHeight(), contRegion);
            ScreenRegion barRegion = new ScreenRegion(region.getX() + barOffsetX, region.getY() + barOffsetY,
                    region.getWidth() - barOffsetX * 2, region.getHeight() - barOffsetY * 2);
            return new JeiUiElement<Integer>() {
                @Nullable
                @Override
                public JeiIngredient<Integer> getIngredient() {
                    return contents;
                }

                @Override
                public ScreenRegion getIngredientRegion() {
                    return barRegion;
                }

                @Override
                public void render(@Nullable Integer ingredient) {
                    bgTexture.draw(region.getX(), region.getY());
                    if (ingredient != null) {
                        barOrientation.draw(
                                fgTexture, barRegion.getX(), barRegion.getY(), ingredient / (float)capacity);
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

        public boolean addIngredient(int amount, boolean isRate, JeiIngredient.Role role) {
            while (true) {
                JeiBuffer buffer = buffers.poll();
                if (buffer == null) {
                    return false;
                }
                if (buffer.isEmpty()) {
                    buffer.setContents(amount, isRate, role);
                    return true;
                }
            }
        }

    }

    public static final BufferType<ForgeEnergyBuffer, Accumulator, JeiBuffer, JeiAccumulator> TYPE
            = new BufferType<ForgeEnergyBuffer, Accumulator, JeiBuffer, JeiAccumulator>() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public Class<ForgeEnergyBuffer> getBufferClass() {
            return ForgeEnergyBuffer.class;
        }

        @Override
        public Class<Accumulator> getAccumulatorClass() {
            return Accumulator.class;
        }

        @Override
        public BufferFactory<ForgeEnergyBuffer, JeiBuffer> loadBufferFactory(JsonObject config) {
            int capacity = config.get("capacity").getAsInt();
            if (capacity <= 0) {
                throw new ConfigException(String.format("Expected positive capacity but got %d", capacity));
            }
            int insertRate = config.has("insert_rate") ? config.get("insert_rate").getAsInt() : capacity / 20;
            int extractRate = config.has("extract_rate") ? config.get("extract_rate").getAsInt() : capacity / 20;
            Positioning uiPosition = config.has("position")
                    ? Positioning.fromJson(config.get("position")) : Positioning.FromCenter.CENTER;
            TextureRegion bgTexture = config.has("bar_bg")
                    ? DataLoadUtils.loadTextureRegion(config.get("bar_bg").getAsJsonObject())
                    : CbtTextureResources.ENERGY_BAR_BG;
            TextureRegion fgTexture = config.has("bar_fg")
                    ? DataLoadUtils.loadTextureRegion(config.get("bar_fg").getAsJsonObject())
                    : CbtTextureResources.ENERGY_BAR_FG;
            int barOffsetX = config.has("bar_offset_x") ? config.get("bar_offset_x").getAsInt() : 1;
            int barOffsetY = config.has("bar_offset_y") ? config.get("bar_offset_y").getAsInt() : 1;
            DrawOrientation barOrientation = config.has("bar_orientation")
                    ? DataLoadUtils.loadDrawOrientation(config.get("bar_orientation").getAsString())
                    : DrawOrientation.BOTTOM_TO_TOP;
            String energyUnitName = config.has("energy_unit_name")
                    ? config.get("energy_unit_name").getAsString() : "FE";
            return new BufferFactory<ForgeEnergyBuffer, JeiBuffer>() {
                @Override
                public ForgeEnergyBuffer createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer) {
                    return new ForgeEnergyBuffer(world, pos, capacity, insertRate, extractRate,
                            uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation, energyUnitName,
                            observer);
                }

                @Override
                public JeiBuffer createJeiBuffer() {
                    return new JeiBuffer(capacity,
                            uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation, energyUnitName);
                }
            };
        }

        @Override
        public void configureDefaultSides(MachineSideHandler.SideConfig<ForgeEnergyBuffer> sideConfig) {
            for (BlockSide side : BlockSide.VALUES) {
                sideConfig.setEnabled(side, true);
            }
        }

        @Override
        public void attachCapabilities(ICapabilityInstanceConsumer target, ForgeEnergyBuffer buffer) {
            target.accept(CapabilityEnergy.ENERGY, buffer.getRestrictedEnergyBuffer());
        }

        @Override
        public TriBool getDefaultExportState(ForgeEnergyBuffer buffer) {
            return buffer.getExtractRate() > 0 ? TriBool.TRUE : TriBool.NONE;
        }

        @Override
        public void doExport(ForgeEnergyBuffer buffer, Set<EnumFacing> faces,
                             TickModulator ticker) {
            if (faces.isEmpty()) {
                ticker.setInterval(20);
                return;
            }
            World world = buffer.getWorld();
            BlockPos pos = buffer.getPosition();
            List<IEnergyStorage> receivers = faces.stream()
                    .map(f -> {
                        TileEntity tile = world.getTileEntity(pos.offset(f));
                        EnumFacing fromFace = f.getOpposite();
                        if (tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, fromFace)) {
                            return Objects.requireNonNull(tile.getCapability(CapabilityEnergy.ENERGY, fromFace));
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (receivers.isEmpty()) {
                ticker.setInterval(20);
                return;
            }
            int distributed = EnergyUtils.distribute(
                    Math.min(buffer.getEnergyStored(), buffer.getExtractRate()), receivers);
            if (distributed <= 0) {
                ticker.increaseIntervalUntil(1, 20);
                return;
            }
            buffer.extractEnergy(distributed, false);
            ticker.setInterval(1);
        }

        @Override
        public Accumulator createAccumulator() {
            return new Accumulator();
        }

        @Override
        public void accumulate(Accumulator acc, ForgeEnergyBuffer buffer) {
            acc.accumulate(buffer);
        }

        @Override
        public Accumulator copyAccumulator(Accumulator acc) {
            return acc.copy();
        }

        @Override
        public UiElement createUiElement(ForgeEnergyBuffer buffer) {
            return (gui, index, contRegion) -> {
                TextureRegion bgTex = buffer.getBackgroundTexture();
                ScreenRegion region = buffer.getUiPosition()
                        .computeRegion(bgTex.getWidth(), bgTex.getHeight(), contRegion);
                gui.addComponent(new EnergyBarGuiComponent(region.getX(), region.getY(),
                        buffer.getBackgroundTexture(), buffer.getForegroundTexture(),
                        buffer.getBarOffsetX(), buffer.getBarOffsetY(), buffer.getBarOrientation(),
                        buffer, buffer.getEnergyUnitName()));
                return region;
            };
        }

        @Override
        public void serializeBufferNbt(NBTTagCompound tag, ForgeEnergyBuffer buffer) {
            buffer.serNBT(tag);
        }

        @Override
        public void serializeBufferBytes(ByteUtils.Writer stream, ForgeEnergyBuffer buffer) {
            buffer.serBytes(stream);
        }

        @Override
        public void deserializeBufferNbt(NBTTagCompound tag, ForgeEnergyBuffer buffer) {
            buffer.deserNBT(tag);
        }

        @Override
        public void deserializeBufferBytes(ByteUtils.Reader stream, ForgeEnergyBuffer buffer) {
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

    public static class EnergyMatcher implements IngredientMatcher<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("energy");

        private final int amount;
        private final boolean doConsume;

        public EnergyMatcher(int amount, boolean doConsume) {
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
            return acc.get().extract(newAmount, false) >= newAmount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(amount, false, JeiIngredient.Role.INPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiForgeEnergyIngredient(amount, "FE", JeiIngredient.Role.INPUT));
        }

        public static final IngredientMatcherType<Accumulator, JeiAccumulator> TYPE
                = new IngredientMatcherType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientMatcher<Accumulator, JeiAccumulator> loadMatcher(JsonObject config) {
                return new EnergyMatcher(config.get("amount").getAsInt(),
                        !config.has("consume") || config.get("consume").getAsBoolean());
            }
        };

    }

    public static class PowerMatcher implements IngredientMatcher<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("power");

        private final int rate;

        public PowerMatcher(int rate) {
            this.rate = rate;
        }

        @Override
        public boolean consumeInitial(Supplier<Accumulator> acc, float consumeFactor) {
            int newAmount = Math.round(consumeFactor * rate);
            return newAmount <= 0 || acc.get().getStored() >= newAmount;
        }

        @Override
        public boolean consumePeriodic(Supplier<Accumulator> acc, float consumeFactor) {
            int newAmount = Math.round(consumeFactor * rate);
            if (newAmount <= 0) {
                return true;
            }
            return acc.get().extract(newAmount, false) >= newAmount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(rate, true, JeiIngredient.Role.INPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiForgeEnergyIngredient(rate, "FE/t", JeiIngredient.Role.INPUT));
        }

        public static final IngredientMatcherType<Accumulator, JeiAccumulator> TYPE
                = new IngredientMatcherType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientMatcher<Accumulator, JeiAccumulator> loadMatcher(JsonObject config) {
                return new PowerMatcher(config.get("rate").getAsInt());
            }
        };

    }

    public static class EnergyProvider implements IngredientProvider<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("energy");

        private final int amount;

        public EnergyProvider(int amount) {
            this.amount = amount;
        }

        @Override
        public boolean insertFinal(Supplier<Accumulator> acc) {
            return acc.get().insert(amount, false) >= amount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(amount, false, JeiIngredient.Role.OUTPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiForgeEnergyIngredient(amount, "FE", JeiIngredient.Role.OUTPUT));
        }

        public static final IngredientProviderType<Accumulator, JeiAccumulator> TYPE
                = new IngredientProviderType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientProvider<Accumulator, JeiAccumulator> loadProvider(JsonObject config) {
                return new EnergyProvider(config.get("amount").getAsInt());
            }
        };

    }

    public static class PowerProvider implements IngredientProvider<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("power");

        private final int rate;

        public PowerProvider(int rate) {
            this.rate = rate;
        }

        @Override
        public void insertPeriodic(Supplier<Accumulator> acc) {
            acc.get().insert(rate, false);
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(rate, true, JeiIngredient.Role.OUTPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiForgeEnergyIngredient(rate, "FE/t", JeiIngredient.Role.OUTPUT));
        }

        public static final IngredientProviderType<Accumulator, JeiAccumulator> TYPE
                = new IngredientProviderType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientProvider<Accumulator, JeiAccumulator> loadProvider(JsonObject config) {
                return new PowerProvider(config.get("rate").getAsInt());
            }
        };

    }

    @SubscribeEvent
    public static void onRegisterIngredients(CbtIngredientHandlerRegistrationEvent<Accumulator, JeiAccumulator> event) {
        if (event.getBufferType() == TYPE) {
            event.registerMatcherType(EnergyMatcher.TYPE);
            event.registerMatcherType(PowerMatcher.TYPE);
            event.registerProviderType(EnergyProvider.TYPE);
            event.registerProviderType(PowerProvider.TYPE);
        }
    }

}
