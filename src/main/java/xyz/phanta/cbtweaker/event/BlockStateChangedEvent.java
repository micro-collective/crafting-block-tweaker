package xyz.phanta.cbtweaker.event;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BlockStateChangedEvent extends Event {

    private final World world;
    private final BlockPos pos;
    private final IBlockState oldState, newState;

    public BlockStateChangedEvent(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        this.world = world;
        this.pos = pos;
        this.oldState = oldState;
        this.newState = newState;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public IBlockState getOldState() {
        return oldState;
    }

    public IBlockState getNewState() {
        return newState;
    }

}
