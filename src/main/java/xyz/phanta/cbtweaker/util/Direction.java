package xyz.phanta.cbtweaker.util;

import com.google.common.collect.ImmutableList;
import io.github.phantamanta44.libnine.util.ImpossibilityRealizedException;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

// all relative uses of directions are with respect to north
public enum Direction implements IStringSerializable {

    NORTH(EnumFacing.NORTH),
    EAST(EnumFacing.EAST),
    SOUTH(EnumFacing.SOUTH),
    WEST(EnumFacing.WEST);

    public static final ImmutableList<Direction> VALUES = ImmutableList.copyOf(values());

    public static Direction getFromFace(EnumFacing face) {
        switch (face) {
            case NORTH:
                return NORTH;
            case EAST:
                return EAST;
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
        }
        throw new IllegalArgumentException("Not a horizontal face: " + face);
    }

    public final EnumFacing face;

    Direction(EnumFacing face) {
        this.face = face;
    }

    @Override
    public String getName() {
        return face.getName();
    }

    public Rotation getRotationFromNorth() {
        switch (this) {
            case NORTH:
                return Rotation.NONE;
            case EAST:
                return Rotation.CLOCKWISE_90;
            case SOUTH:
                return Rotation.CLOCKWISE_180;
            case WEST:
                return Rotation.COUNTERCLOCKWISE_90;
        }
        throw new ImpossibilityRealizedException();
    }

    private Direction cycle(int indices) {
        return VALUES.get((ordinal() + indices) % VALUES.size());
    }

    public Direction rotateCw() {
        return cycle(1);
    }

    public Direction getOpposite() {
        return cycle(2);
    }

    public Direction rotateCcw() {
        return cycle(3);
    }

    public Direction rotate(Rotation rot) {
        switch (rot) {
            case NONE:
                return this;
            case CLOCKWISE_90:
                return rotateCw();
            case CLOCKWISE_180:
                return getOpposite();
            case COUNTERCLOCKWISE_90:
                return rotateCcw();
        }
        throw new ImpossibilityRealizedException();
    }

    public Vec3i transform(Vec3i vec, boolean mirror) {
        switch (this) {
            case NORTH:
                return mirror ? new Vec3i(-vec.getX(), vec.getY(), vec.getZ()) : vec;
            case EAST:
                return mirror ? new Vec3i(vec.getZ(), vec.getY(), vec.getX())
                        : new Vec3i(-vec.getZ(), vec.getY(), vec.getX());
            case SOUTH:
                return mirror ? new Vec3i(vec.getX(), vec.getY(), -vec.getZ())
                        : new Vec3i(-vec.getX(), vec.getY(), -vec.getZ());
            case WEST:
                return mirror ? new Vec3i(-vec.getZ(), vec.getY(), -vec.getX())
                        : new Vec3i(vec.getZ(), vec.getY(), -vec.getX());
        }
        throw new ImpossibilityRealizedException();
    }

    public BlockPos transform(BlockPos pos, BlockPos axisPos, boolean mirror) {
        return axisPos.add(transform(pos.subtract(axisPos), mirror));
    }

}
