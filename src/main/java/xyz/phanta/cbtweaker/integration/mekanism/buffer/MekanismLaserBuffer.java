package xyz.phanta.cbtweaker.integration.mekanism.buffer;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.event.ModDependentEventBusSubscriber;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import io.github.phantamanta44.libnine.util.world.BlockSide;
import mekanism.api.lasers.ILaserReceptor;
import mekanism.common.capabilities.Capabilities;
import mezz.jei.api.IJeiHelpers;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.buffer.BufferFactory;
import xyz.phanta.cbtweaker.buffer.BufferObserver;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcher;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcherType;
import xyz.phanta.cbtweaker.event.CbtIngredientHandlerRegistrationEvent;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.component.DoubleEnergyBarGuiComponent;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.integration.mekanism.MekanismIntegration;
import xyz.phanta.cbtweaker.integration.mekanism.jei.JeiMekanismJoulesIngredient;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.block.MachineSideHandler;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

@ModDependentEventBusSubscriber(dependencies = MekanismIntegration.MOD_ID)
public class MekanismLaserBuffer implements ILaserReceptor, ISerializable {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("mekanism_laser");

    private final double capacity;
    private double energyStored = 0D;
    @Nullable
    private final BufferObserver observer;

    private final Positioning uiPosition;
    private final TextureRegion bgTexture, fgTexture;
    private final int barOffsetX, barOffsetY;
    private final DrawOrientation barOrientation;

    public MekanismLaserBuffer(double capacity,
                               Positioning uiPosition, TextureRegion bgTexture, TextureRegion fgTexture,
                               int barOffsetX, int barOffsetY, DrawOrientation barOrientation,
                               @Nullable BufferObserver observer) {
        this.capacity = capacity;
        this.observer = observer;
        this.uiPosition = uiPosition;
        this.bgTexture = bgTexture;
        this.fgTexture = fgTexture;
        this.barOffsetX = barOffsetX;
        this.barOffsetY = barOffsetY;
        this.barOrientation = barOrientation;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getEnergyStored() {
        return energyStored;
    }

    public void setEnergyStored(double energyStored) {
        if (this.energyStored == energyStored) {
            return;
        }
        this.energyStored = energyStored;
        if (observer != null) {
            observer.onIngredientsChanged();
        }
    }

    public double insert(double amount, boolean commit) {
        double toTransfer = Math.min(amount, capacity - energyStored);
        if (toTransfer <= 0) {
            return 0;
        }
        if (commit) {
            setEnergyStored(energyStored + toTransfer);
        }
        return toTransfer;
    }

    public double extract(double amount, boolean commit) {
        if (amount <= 0 || energyStored <= 0) {
            return 0;
        }
        double toTransfer = Math.min(amount, energyStored); // must be positive
        if (commit) {
            setEnergyStored(energyStored - toTransfer);
        }
        return toTransfer;
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

    @Override
    public void receiveLaserEnergy(double energy, EnumFacing face) {
        setEnergyStored(Math.min(energyStored + energy, capacity));
    }

    @Override
    public boolean canLasersDig() {
        return false;
    }

    public MekanismLaserBuffer copy(@Nullable BufferObserver observer) {
        MekanismLaserBuffer buf = new MekanismLaserBuffer(capacity,
                uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation, observer);
        buf.energyStored = energyStored;
        return buf;
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        data.writeDouble(energyStored);
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        energyStored = data.readDouble();
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        tag.setDouble("EnergyStored", energyStored);
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        energyStored = tag.getDouble("EnergyStored");
    }

    public static class Accumulator {

        private final List<MekanismLaserBuffer> buffers = new ArrayList<>();

        private void accumulate(MekanismLaserBuffer buffer) {
            buffers.add(buffer);
        }

        public double getStored() {
            return buffers.stream().mapToDouble(MekanismLaserBuffer::getEnergyStored).sum();
        }

        public double getCapacity() {
            return buffers.stream().mapToDouble(MekanismLaserBuffer::getCapacity).sum();
        }

        public double insert(double amount, boolean commit) {
            double remaining = amount;
            for (MekanismLaserBuffer buffer : buffers) {
                remaining -= buffer.insert(remaining, commit);
                if (remaining <= 0) {
                    return amount;
                }
            }
            return amount - remaining;
        }

        public double extract(double amount, boolean commit) {
            double remaining = amount;
            for (MekanismLaserBuffer buffer : buffers) {
                remaining -= buffer.extract(remaining, commit);
                if (remaining <= 0) {
                    return amount;
                }
            }
            return amount - remaining;
        }

        public Accumulator copy() {
            Accumulator acc = new Accumulator();
            for (MekanismLaserBuffer buffer : buffers) {
                acc.accumulate(buffer.copy(null));
            }
            return acc;
        }

    }

    public static class JeiBuffer {

        private final double capacity;
        @Nullable
        private JeiMekanismJoulesIngredient contents = null;
        private final Positioning uiPosition;
        private final TextureRegion bgTexture, fgTexture;
        private final int barOffsetX, barOffsetY;
        private final DrawOrientation barOrientation;

        public JeiBuffer(double capacity,
                         Positioning uiPosition, TextureRegion bgTexture, TextureRegion fgTexture,
                         int barOffsetX, int barOffsetY, DrawOrientation barOrientation) {
            this.capacity = capacity;
            this.uiPosition = uiPosition;
            this.bgTexture = bgTexture;
            this.fgTexture = fgTexture;
            this.barOffsetX = barOffsetX;
            this.barOffsetY = barOffsetY;
            this.barOrientation = barOrientation;
        }

        public boolean isEmpty() {
            return contents == null;
        }

        public void setContents(double amount, boolean isRate, JeiIngredient.Role role) {
            contents = new JeiMekanismJoulesIngredient(
                    amount, (isRate ? "J/t " : "J ") + I18n.format(CbtLang.MEKANISM_LASER), role);
        }

        public JeiUiElement<?> createJeiUiElement(ScreenRegion contRegion) {
            ScreenRegion region = uiPosition.computeRegion(bgTexture.getWidth(), bgTexture.getHeight(), contRegion);
            ScreenRegion barRegion = new ScreenRegion(region.getX() + barOffsetX, region.getY() + barOffsetY,
                    region.getWidth() - barOffsetX * 2, region.getHeight() - barOffsetY * 2);
            return new JeiUiElement<Double>() {
                @Nullable
                @Override
                public JeiIngredient<Double> getIngredient() {
                    return contents;
                }

                @Override
                public ScreenRegion getIngredientRegion() {
                    return barRegion;
                }

                @Override
                public void render(@Nullable Double ingredient) {
                    bgTexture.draw(region.getX(), region.getY());
                    if (ingredient != null) {
                        barOrientation.draw(fgTexture, barRegion.getX(), barRegion.getY(),
                                Double.valueOf(ingredient / capacity).floatValue());
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

        public boolean addIngredient(double amount, boolean isRate, JeiIngredient.Role role) {
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

    public static final BufferType<MekanismLaserBuffer, Accumulator, JeiBuffer, JeiAccumulator> TYPE
            = new BufferType<MekanismLaserBuffer, Accumulator, JeiBuffer, JeiAccumulator>() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public Class<MekanismLaserBuffer> getBufferClass() {
            return MekanismLaserBuffer.class;
        }

        @Override
        public Class<Accumulator> getAccumulatorClass() {
            return Accumulator.class;
        }

        @Override
        public BufferFactory<MekanismLaserBuffer, JeiBuffer> loadBufferFactory(JsonObject config) {
            double capacity = config.get("capacity").getAsDouble();
            if (capacity <= 0) {
                throw new ConfigException(String.format("Expected positive capacity but got %f", capacity));
            }
            Positioning uiPosition = config.has("position")
                    ? Positioning.fromJson(config.get("position")) : Positioning.FromCenter.CENTER;
            TextureRegion bgTexture = config.has("bar_bg")
                    ? DataLoadUtils.loadTextureRegion(config.get("bar_bg").getAsJsonObject())
                    : CbtTextureResources.MEKANISM_LASER_BAR_BG;
            TextureRegion fgTexture = config.has("bar_fg")
                    ? DataLoadUtils.loadTextureRegion(config.get("bar_fg").getAsJsonObject())
                    : CbtTextureResources.MEKANISM_LASER_BAR_FG;
            int barOffsetX = config.has("bar_offset_x") ? config.get("bar_offset_x").getAsInt() : 1;
            int barOffsetY = config.has("bar_offset_y") ? config.get("bar_offset_y").getAsInt() : 1;
            DrawOrientation barOrientation = config.has("bar_orientation")
                    ? DataLoadUtils.loadDrawOrientation(config.get("bar_orientation").getAsString())
                    : DrawOrientation.BOTTOM_TO_TOP;
            return new BufferFactory<MekanismLaserBuffer, JeiBuffer>() {
                @Override
                public MekanismLaserBuffer createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer) {
                    return new MekanismLaserBuffer(capacity,
                            uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation, observer);
                }

                @Override
                public JeiBuffer createJeiBuffer() {
                    return new JeiBuffer(capacity,
                            uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation);
                }
            };
        }

        @Override
        public void configureDefaultSides(MachineSideHandler.SideConfig<MekanismLaserBuffer> sideConfig) {
            for (BlockSide side : BlockSide.VALUES) {
                sideConfig.setEnabled(side, true);
            }
        }

        @Override
        public void attachCapabilities(ICapabilityInstanceConsumer target, MekanismLaserBuffer buffer) {
            target.accept(Capabilities.LASER_RECEPTOR_CAPABILITY, buffer);
        }

        @Override
        public Accumulator createAccumulator() {
            return new Accumulator();
        }

        @Override
        public void accumulate(Accumulator acc, MekanismLaserBuffer buffer) {
            acc.accumulate(buffer);
        }

        @Override
        public Accumulator copyAccumulator(Accumulator acc) {
            return acc.copy();
        }

        @Override
        public UiElement createUiElement(MekanismLaserBuffer buffer) {
            return (gui, index, contRegion) -> {
                TextureRegion bgTex = buffer.getBackgroundTexture();
                ScreenRegion region = buffer.getUiPosition()
                        .computeRegion(bgTex.getWidth(), bgTex.getHeight(), contRegion);
                gui.addComponent(new DoubleEnergyBarGuiComponent(region.getX(), region.getY(),
                        buffer.getBackgroundTexture(), buffer.getForegroundTexture(),
                        buffer.getBarOffsetX(), buffer.getBarOffsetY(), buffer.getBarOrientation(),
                        buffer::getEnergyStored, buffer.getCapacity(), "J"));
                return region;
            };
        }

        @Override
        public void serializeBufferNbt(NBTTagCompound tag, MekanismLaserBuffer buffer) {
            buffer.serNBT(tag);
        }

        @Override
        public void serializeBufferBytes(ByteUtils.Writer stream, MekanismLaserBuffer buffer) {
            buffer.serBytes(stream);
        }

        @Override
        public void deserializeBufferNbt(NBTTagCompound tag, MekanismLaserBuffer buffer) {
            buffer.deserNBT(tag);
        }

        @Override
        public void deserializeBufferBytes(ByteUtils.Reader stream, MekanismLaserBuffer buffer) {
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

        private final double amount;
        private final boolean doConsume;

        public EnergyMatcher(double amount, boolean doConsume) {
            this.amount = amount;
            this.doConsume = doConsume;
        }

        @Override
        public boolean consumeInitial(Supplier<Accumulator> acc, float consumeFactor) {
            if (!doConsume) {
                return true;
            }
            double newAmount = consumeFactor * amount;
            if (newAmount <= 0) {
                return true;
            }
            return acc.get().extract(newAmount, true) >= newAmount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(amount, false, JeiIngredient.Role.INPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiMekanismJoulesIngredient(
                    amount, "J " + I18n.format(CbtLang.MEKANISM_LASER), JeiIngredient.Role.INPUT));
        }

        public static final IngredientMatcherType<Accumulator, JeiAccumulator> TYPE
                = new IngredientMatcherType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientMatcher<Accumulator, JeiAccumulator> loadMatcher(JsonObject config) {
                return new EnergyMatcher(config.get("amount").getAsDouble(),
                        !config.has("consume") || config.get("consume").getAsBoolean());
            }
        };

    }

    public static class PowerMatcher implements IngredientMatcher<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("power");

        private final double rate;

        public PowerMatcher(double rate) {
            this.rate = rate;
        }

        @Override
        public boolean consumeInitial(Supplier<Accumulator> acc, float consumeFactor) {
            double newAmount = consumeFactor * rate;
            return newAmount <= 0 || acc.get().getStored() >= newAmount;
        }

        @Override
        public boolean consumePeriodic(Supplier<Accumulator> acc, float consumeFactor) {
            double newAmount = consumeFactor * rate;
            if (newAmount <= 0) {
                return true;
            }
            return acc.get().extract(newAmount, true) >= newAmount;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(rate, true, JeiIngredient.Role.INPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiMekanismJoulesIngredient(
                    rate, "J/t " + I18n.format(CbtLang.MEKANISM_LASER), JeiIngredient.Role.INPUT));
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

    @SubscribeEvent
    public static void onRegisterIngredients(CbtIngredientHandlerRegistrationEvent<Accumulator, JeiAccumulator> event) {
        if (event.getBufferType() == TYPE) {
            event.registerMatcherType(EnergyMatcher.TYPE);
            event.registerMatcherType(PowerMatcher.TYPE);
        }
    }

}
