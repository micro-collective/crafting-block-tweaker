package xyz.phanta.cbtweaker.buffer;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.Unit;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import mezz.jei.api.IJeiHelpers;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

public abstract class VirtualBufferType<B> implements BufferType<B, Unit, Unit, Unit> {

    @Override
    public Class<Unit> getAccumulatorClass() {
        return Unit.class;
    }

    @Override
    public BufferFactory<B, Unit> loadBufferFactory(JsonObject config) {
        VirtualBufferFactory<B> bufFactory = loadVirtualBufferFactory(config);
        return new BufferFactory<B, Unit>() {
            @Override
            public B createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer) {
                return bufFactory.createBuffer(world, pos, observer);
            }

            @Override
            public Unit createJeiBuffer() {
                return Unit.INSTANCE;
            }
        };
    }

    protected abstract VirtualBufferFactory<B> loadVirtualBufferFactory(JsonObject config);

    @Override
    public boolean isCapabilitySided(B buffer) {
        return false;
    }

    @Override
    public void attachCapabilities(ICapabilityInstanceConsumer target, B buffer) {
        // NO-OP
    }

    @Override
    public Unit createAccumulator() {
        return Unit.INSTANCE;
    }

    @Override
    public void accumulate(Unit acc, B buffer) {
        // NO-OP
    }

    @Override
    public Unit copyAccumulator(Unit acc) {
        return Unit.INSTANCE;
    }

    @Override
    public Unit createJeiAccumulator() {
        return Unit.INSTANCE;
    }

    @Override
    public void jeiAccumulate(Unit acc, Unit buffer) {
        // NO-OP
    }

    @Override
    public Collection<JeiUiElement<?>> createJeiUiElements(Unit buffer, ScreenRegion contRegion,
                                                           IJeiHelpers jeiHelpers) {
        return Collections.emptyList();
    }

    @FunctionalInterface
    protected interface VirtualBufferFactory<B> {

        B createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer);

    }

}
