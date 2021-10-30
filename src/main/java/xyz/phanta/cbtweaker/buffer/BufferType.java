package xyz.phanta.cbtweaker.buffer;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.TriBool;
import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.function.ICapabilityInstanceConsumer;
import mezz.jei.api.IJeiHelpers;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.integration.jei.render.JeiUiElement;
import xyz.phanta.cbtweaker.recipe.ComponentSet;
import xyz.phanta.cbtweaker.util.TickModulator;
import xyz.phanta.cbtweaker.util.block.MachineSideHandler;

import java.util.Collection;
import java.util.Set;

public interface BufferType<B, A, JB, JA> {

    ResourceLocation getId();

    Class<B> getBufferClass();

    Class<A> getAccumulatorClass();

    BufferFactory<B, JB> loadBufferFactory(JsonObject config);

    default boolean isCapabilitySided(B buffer) {
        return true;
    }

    default void configureDefaultSides(MachineSideHandler.SideConfig<B> sideConfig) {
        // NO-OP
    }

    default void attachCapabilities(ICapabilityInstanceConsumer target, B buffer) {
        // NO-OP
    }

    default void collectComponents(ComponentSet components, B buffer) {
        // NO-OP
    }

    default void tick(B buffer) {
        // NO-OP
    }

    default TriBool getDefaultExportState(B buffer) {
        return TriBool.NONE;
    }

    default void doExport(B buffer, Set<EnumFacing> faces, TickModulator ticker) {
        throw new UnsupportedOperationException();
    }

    default boolean handleInteraction(B buffer, IBlockState state,
                                      EntityPlayer player, EnumHand hand, EnumFacing face) {
        return false;
    }

    default void dropContents(B buffer) {
        // NO-OP
    }

    A createAccumulator();

    void accumulate(A acc, B buffer);

    A copyAccumulator(A acc);

    UiElement createUiElement(B buffer);

    void serializeBufferNbt(NBTTagCompound tag, B buffer);

    void serializeBufferBytes(ByteUtils.Writer stream, B buffer);

    void deserializeBufferNbt(NBTTagCompound tag, B buffer);

    void deserializeBufferBytes(ByteUtils.Reader stream, B buffer);

    JA createJeiAccumulator();

    void jeiAccumulate(JA acc, JB buffer);

    Collection<JeiUiElement<?>> createJeiUiElements(JB buffer, ScreenRegion contRegion, IJeiHelpers jeiHelpers);

}
