package xyz.phanta.cbtweaker.coremod;

import io.github.phantamanta44.libnine.util.nullity.Reflected;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import xyz.phanta.cbtweaker.event.BlockStateChangedEvent;

// methods not actually reflected, but invoked via injected hooks
// see CbtCoreMod
public class CbtCoreHooks {

    @Reflected
    public static void onBlockStateChanged(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        MinecraftForge.EVENT_BUS.post(new BlockStateChangedEvent(world, pos, oldState, newState));
    }

}
