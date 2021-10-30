package xyz.phanta.cbtweaker.buffer;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public interface BufferFactory<B, JB> {

    B createBuffer(World world, BlockPos pos, @Nullable BufferObserver observer);

    JB createJeiBuffer();

}
