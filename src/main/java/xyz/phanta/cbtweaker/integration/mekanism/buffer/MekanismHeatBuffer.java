package xyz.phanta.cbtweaker.integration.mekanism.buffer;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.event.ModDependentEventBusSubscriber;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import io.github.phantamanta44.libnine.util.world.BlockSide;
import mekanism.api.IHeatTransfer;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.util.HeatUtils;
import mezz.jei.api.IJeiHelpers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
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
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.integration.jei.ingredient.JeiIngredient;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.integration.mekanism.MekanismIntegration;
import xyz.phanta.cbtweaker.integration.mekanism.gui.HeatBarGuiComponent;
import xyz.phanta.cbtweaker.integration.mekanism.jei.JeiMekanismHeatIngredient;
import xyz.phanta.cbtweaker.util.Positioning;
import xyz.phanta.cbtweaker.util.block.MachineSideHandler;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

@ModDependentEventBusSubscriber(dependencies = MekanismIntegration.MOD_ID)
public class MekanismHeatBuffer implements IHeatTransfer, ISerializable {

    public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("mekanism_heat");

    private final World world;
    private final BlockPos bufPos;

    private double temperature = 0D;
    private final double heatCapacity, invCondCoeff, insulCoeff;
    private final boolean spreadHeat;

    @Nullable
    private final BufferObserver observer;

    private double bufferedHeat = 0D;

    private final Positioning uiPosition;
    private final TextureRegion bgTexture, fgTexture;
    private final int barOffsetX, barOffsetY;
    private final DrawOrientation barOrientation;
    private final double barMaxTemp;

    public MekanismHeatBuffer(World world, BlockPos bufPos,
                              double heatCapacity, double invCondCoeff, double insulCoeff, boolean spreadHeat,
                              Positioning uiPosition, TextureRegion bgTexture, TextureRegion fgTexture,
                              int barOffsetX, int barOffsetY, DrawOrientation barOrientation, double barMaxTemp,
                              @Nullable BufferObserver observer) {
        this.world = world;
        this.bufPos = bufPos;
        this.heatCapacity = heatCapacity;
        this.invCondCoeff = invCondCoeff;
        this.insulCoeff = insulCoeff;
        this.spreadHeat = spreadHeat;
        this.observer = observer;
        this.uiPosition = uiPosition;
        this.bgTexture = bgTexture;
        this.fgTexture = fgTexture;
        this.barOffsetX = barOffsetX;
        this.barOffsetY = barOffsetY;
        this.barOrientation = barOrientation;
        this.barMaxTemp = barMaxTemp;
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

    public double getBarMaxTemperature() {
        return barMaxTemp;
    }

    @Override
    public double getTemp() {
        return temperature;
    }

    public void setTemp(double temperature) {
        if (temperature < 0) {
            temperature = 0;
        }
        if (this.temperature == temperature) {
            return;
        }
        this.temperature = temperature;
        if (observer != null) {
            observer.onIngredientsChanged();
        }
    }

    @Override
    public double getInverseConductionCoefficient() {
        return invCondCoeff;
    }

    @Override
    public double getInsulationCoefficient(EnumFacing face) {
        return insulCoeff;
    }

    @Override
    public void transferHeatTo(double heat) {
        bufferedHeat += heat;
    }

    @Override
    public double[] simulateHeat() {
        return HeatUtils.simulate(this);
    }

    @Override
    public double applyTemperatureChange() {
        setTemp(temperature + bufferedHeat / heatCapacity);
        bufferedHeat = 0D;
        return temperature;
    }

    @Override
    public boolean canConnectHeat(EnumFacing face) {
        return true;
    }

    @Nullable
    @Override
    public IHeatTransfer getAdjacent(EnumFacing face) {
        if (!spreadHeat) {
            return null;
        }
        TileEntity adj = world.getTileEntity(bufPos.offset(face));
        if (adj == null) {
            return null;
        }
        EnumFacing adjFace = face.getOpposite();
        if (!adj.hasCapability(Capabilities.HEAT_TRANSFER_CAPABILITY, adjFace)) {
            return null;
        }
        return adj.getCapability(Capabilities.HEAT_TRANSFER_CAPABILITY, adjFace);
    }

    public MekanismHeatBuffer copy(@Nullable BufferObserver observer) {
        MekanismHeatBuffer buf = new MekanismHeatBuffer(world, bufPos, heatCapacity, invCondCoeff, insulCoeff, spreadHeat,
                uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation, barMaxTemp, observer);
        buf.temperature = temperature;
        return buf;
    }

    public void tick() {
        if (world.isRemote) {
            return;
        }
        simulateHeat();
        applyTemperatureChange();
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        data.writeDouble(temperature);
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        temperature = data.readDouble();
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        tag.setDouble("Temp", temperature);
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        temperature = tag.getDouble("Temp");
    }

    public static class Accumulator {

        private final List<MekanismHeatBuffer> buffers = new ArrayList<>();

        private void accumulate(MekanismHeatBuffer buffer) {
            buffers.add(buffer);
        }

        public double getTemperature() {
            return buffers.stream().mapToDouble(MekanismHeatBuffer::getTemp).max().orElse(0D);
        }

        public void insert(double amount) {
            double amountPerBuffer = amount / buffers.size();
            for (MekanismHeatBuffer buffer : buffers) {
                buffer.setTemp(buffer.getTemp() + amountPerBuffer);
            }
        }

        public Accumulator copy() {
            Accumulator acc = new Accumulator();
            for (MekanismHeatBuffer buffer : buffers) {
                acc.accumulate(buffer.copy(null));
            }
            return acc;
        }

    }

    public static class JeiBuffer {

        @Nullable
        private JeiMekanismHeatIngredient contents = null;
        private final Positioning uiPosition;
        private final TextureRegion bgTexture, fgTexture;
        private final int barOffsetX, barOffsetY;
        private final DrawOrientation barOrientation;
        private final double barMaxTemp;

        public JeiBuffer(Positioning uiPosition, TextureRegion bgTexture, TextureRegion fgTexture,
                         int barOffsetX, int barOffsetY, DrawOrientation barOrientation, double barMaxTemp) {
            this.uiPosition = uiPosition;
            this.bgTexture = bgTexture;
            this.fgTexture = fgTexture;
            this.barOffsetX = barOffsetX;
            this.barOffsetY = barOffsetY;
            this.barOrientation = barOrientation;
            this.barMaxTemp = barMaxTemp;
        }

        public boolean isEmpty() {
            return contents == null;
        }

        public void setContents(double amount, boolean isRate, JeiIngredient.Role role) {
            contents = new JeiMekanismHeatIngredient(amount, isRate, role);
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
                                Double.valueOf(Math.min(ingredient / barMaxTemp, 1D)).floatValue());
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

    public static final BufferType<MekanismHeatBuffer, Accumulator, JeiBuffer, JeiAccumulator> TYPE
            = new BufferType<MekanismHeatBuffer, Accumulator, JeiBuffer, JeiAccumulator>() {
        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public Class<MekanismHeatBuffer> getBufferClass() {
            return MekanismHeatBuffer.class;
        }

        @Override
        public Class<Accumulator> getAccumulatorClass() {
            return Accumulator.class;
        }

        @Override
        public BufferFactory<MekanismHeatBuffer, JeiBuffer> loadBufferFactory(JsonObject config) {
            double heatCapacity = config.has("heat_capacity") ? config.get("heat_capacity").getAsDouble() : 4.184D;
            double invCondCoeff = config.has("inverse_conduction_coeff")
                    ? config.get("inverse_conduction_coeff").getAsDouble() : 5D;
            double insulCoeff = config.has("insulation_coeff") ? config.get("insulation_coeff").getAsDouble() : 1000D;
            boolean spreadHeat = config.has("spread_heat") && config.get("spread_heat").getAsBoolean();
            Positioning uiPosition = config.has("position")
                    ? Positioning.fromJson(config.get("position")) : Positioning.FromCenter.CENTER;
            TextureRegion bgTexture = config.has("bar_bg")
                    ? DataLoadUtils.loadTextureRegion(config.get("bar_bg").getAsJsonObject())
                    : CbtTextureResources.MEKANISM_HEAT_BAR_BG;
            TextureRegion fgTexture = config.has("bar_fg")
                    ? DataLoadUtils.loadTextureRegion(config.get("bar_fg").getAsJsonObject())
                    : CbtTextureResources.MEKANISM_HEAT_BAR_FG;
            int barOffsetX = config.has("bar_offset_x") ? config.get("bar_offset_x").getAsInt() : 1;
            int barOffsetY = config.has("bar_offset_y") ? config.get("bar_offset_y").getAsInt() : 1;
            DrawOrientation barOrientation = config.has("bar_orientation")
                    ? DataLoadUtils.loadDrawOrientation(config.get("bar_orientation").getAsString())
                    : DrawOrientation.BOTTOM_TO_TOP;
            double barMaxTemp = config.has("bar_max_temp") ? config.get("bar_max_temp").getAsDouble() : 3000D;
            return new BufferFactory<MekanismHeatBuffer, JeiBuffer>() {
                @Override
                public MekanismHeatBuffer createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer) {
                    return new MekanismHeatBuffer(world, pos, heatCapacity, invCondCoeff, insulCoeff, spreadHeat,
                            uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation, barMaxTemp,
                            observer);
                }

                @Override
                public JeiBuffer createJeiBuffer() {
                    return new JeiBuffer(
                            uiPosition, bgTexture, fgTexture, barOffsetX, barOffsetY, barOrientation, barMaxTemp);
                }
            };
        }

        @Override
        public void configureDefaultSides(MachineSideHandler.SideConfig<MekanismHeatBuffer> sideConfig) {
            for (BlockSide side : BlockSide.VALUES) {
                sideConfig.setEnabled(side, true);
            }
        }

        @Override
        public void attachCapabilities(ICapabilityInstanceConsumer target, MekanismHeatBuffer buffer) {
            target.accept(Capabilities.HEAT_TRANSFER_CAPABILITY, buffer);
        }

        @Override
        public void tick(MekanismHeatBuffer buffer) {
            buffer.tick();
        }

        @Override
        public Accumulator createAccumulator() {
            return new Accumulator();
        }

        @Override
        public void accumulate(Accumulator acc, MekanismHeatBuffer buffer) {
            acc.accumulate(buffer);
        }

        @Override
        public Accumulator copyAccumulator(Accumulator acc) {
            return acc.copy();
        }

        @Override
        public UiElement createUiElement(MekanismHeatBuffer buffer) {
            return (gui, index, contRegion) -> {
                TextureRegion bgTex = buffer.getBackgroundTexture();
                ScreenRegion region = buffer.getUiPosition()
                        .computeRegion(bgTex.getWidth(), bgTex.getHeight(), contRegion);
                gui.addComponent(new HeatBarGuiComponent(region.getX(), region.getY(),
                        buffer.getBackgroundTexture(), buffer.getForegroundTexture(),
                        buffer.getBarOffsetX(), buffer.getBarOffsetY(), buffer.getBarOrientation(),
                        buffer::getTemp, buffer.getBarMaxTemperature(), "K"));
                return region;
            };
        }

        @Override
        public void serializeBufferNbt(NBTTagCompound tag, MekanismHeatBuffer buffer) {
            buffer.serNBT(tag);
        }

        @Override
        public void serializeBufferBytes(ByteUtils.Writer stream, MekanismHeatBuffer buffer) {
            buffer.serBytes(stream);
        }

        @Override
        public void deserializeBufferNbt(NBTTagCompound tag, MekanismHeatBuffer buffer) {
            buffer.deserNBT(tag);
        }

        @Override
        public void deserializeBufferBytes(ByteUtils.Reader stream, MekanismHeatBuffer buffer) {
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

    public static class TemperatureMatcher implements IngredientMatcher<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("temperature");

        private final double temp;

        public TemperatureMatcher(double temp) {
            this.temp = temp;
        }

        @Override
        public boolean consumeInitial(Supplier<Accumulator> acc, float consumeFactor) {
            return acc.get().getTemperature() >= temp;
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(temp, false, JeiIngredient.Role.INPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiMekanismHeatIngredient(temp, false, JeiIngredient.Role.INPUT));
        }

        public static final IngredientMatcherType<Accumulator, JeiAccumulator> TYPE
                = new IngredientMatcherType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientMatcher<Accumulator, JeiAccumulator> loadMatcher(JsonObject config) {
                return new TemperatureMatcher(config.get("temperature").getAsDouble());
            }
        };

    }

    public static class HeatProvider implements IngredientProvider<Accumulator, JeiAccumulator> {

        public static final ResourceLocation ID = CbtMod.INSTANCE.newResourceLocation("heat");

        private final int rate;

        public HeatProvider(int rate) {
            this.rate = rate;
        }

        @Override
        public void insertPeriodic(Supplier<Accumulator> acc) {
            acc.get().insert(rate);
        }

        @Override
        public boolean populateJei(JeiAccumulator acc) {
            return acc.addIngredient(rate, true, JeiIngredient.Role.OUTPUT);
        }

        @Override
        public Collection<JeiIngredient<?>> getJeiIngredients() {
            return Collections.singletonList(new JeiMekanismHeatIngredient(rate, true, JeiIngredient.Role.OUTPUT));
        }

        public static final IngredientProviderType<Accumulator, JeiAccumulator> TYPE
                = new IngredientProviderType<Accumulator, JeiAccumulator>() {
            @Override
            public ResourceLocation getId() {
                return ID;
            }

            @Override
            public IngredientProvider<Accumulator, JeiAccumulator> loadProvider(JsonObject config) {
                return new HeatProvider(config.get("heat").getAsInt());
            }
        };

    }

    @SubscribeEvent
    public static void onRegisterIngredients(CbtIngredientHandlerRegistrationEvent<Accumulator, JeiAccumulator> event) {
        if (event.getBufferType() == TYPE) {
            event.registerMatcherType(TemperatureMatcher.TYPE);
            event.registerProviderType(HeatProvider.TYPE);
        }
    }

}
