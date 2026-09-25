package st.evening.mc.cbtweaker.util

import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Mirror
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3i
import st.evening.mc.prelude.api.util.math.MathsHelper
import st.evening.mc.prelude.api.util.math.Vec2i
import st.evening.mc.prelude.api.util.math.ceilDivPos
import st.evening.mc.prelude.api.util.math.minus
import st.evening.mc.prelude.api.util.math.plus
import st.evening.mc.prelude.api.util.world.BlockSide
import java.util.Random

object CbtMathHelper {
    val cbtRandom: Random = Random()

    fun scaleConsumeInt(amount: Int, scaleFactor: Float, checkMode: Boolean, random: Random = cbtRandom): Int {
        val f = amount * scaleFactor
        if (f >= 1) return MathHelper.ceil(f)
        if (f <= 0) return 0
        return if (checkMode || random.nextFloat() < f) 1 else 0
    }

    fun rollProduce(probability: Float, checkMode: Boolean, random: Random = cbtRandom): Boolean = when {
        probability <= 0 -> false
        checkMode || probability >= 1 -> true
        else -> random.nextFloat() <= probability
    }

    // automatically figure out how to lay out slots in a variable-size group!
    // first, we compute the smallest square number that is >= the slot count; this will be the square size
    // next, we want to minimize the number of rows used, then minimize the difference in count between rows
    fun layOutSlotGroup(slotWidth: Int, slotHeight: Int, slotPosList: Array<in Vec2i>): Vec2i {
        val slotCount = slotPosList.size
        val rowLength = MathsHelper.ceilSquare(slotCount)
        val rowCount = slotCount ceilDivPos rowLength
        val longRowCount = if (rowCount == 1) {
            1 // edge case, should only happen for slotCount = 1 or 2
        } else {
            (slotCount % rowCount).let { if (it > 0) it else rowLength }
        }
        var i = 0
        var y = 0
        while (y < longRowCount) {
            val slotY = y * slotHeight
            for (x in 0..<rowLength) {
                slotPosList[i++] = Vec2i(x * slotWidth, slotY)
            }
            y++
        }
        val shortRowOffset = slotWidth / 2
        while (y < rowCount) {
            val slotY = y * slotHeight
            for (x in 0..<rowLength - 1) {
                slotPosList[i++] = Vec2i(shortRowOffset + x * slotWidth, slotY)
            }
            y++
        }
        return Vec2i(rowLength * slotWidth, rowCount * slotHeight)
    }
}

fun BlockSide.getRotationFromNorth(): Rotation = when (this) {
    BlockSide.NORTH -> Rotation.NONE
    BlockSide.EAST -> Rotation.CLOCKWISE_90
    BlockSide.SOUTH -> Rotation.CLOCKWISE_180
    BlockSide.WEST -> Rotation.COUNTERCLOCKWISE_90
}

fun Rotation.rotate(vec: Vec3i, mirrorX: Boolean): Vec3i = when (this) {
    Rotation.NONE -> if (mirrorX) Vec3i(-vec.x, vec.y, vec.z) /* reflect across z axis */ else vec
    Rotation.CLOCKWISE_90 ->
        if (mirrorX) Vec3i(-vec.z, vec.y, -vec.x) /* reflect across negative diag */ else Vec3i(-vec.z, vec.y, vec.x)
    Rotation.CLOCKWISE_180 ->
        if (mirrorX) Vec3i(vec.x, vec.y, -vec.z) /* reflect across x axis */ else Vec3i(-vec.x, vec.y, -vec.z)
    Rotation.COUNTERCLOCKWISE_90 ->
        if (mirrorX) Vec3i(vec.z, vec.y, vec.x) /* reflect across positive diag */ else Vec3i(vec.z, vec.y, -vec.x)
}

fun Rotation.unrotate(vec: Vec3i, mirrorX: Boolean): Vec3i = when (this) { // note 6 of these are reflections
    Rotation.NONE -> if (mirrorX) Vec3i(-vec.x, vec.y, vec.z) else vec
    Rotation.CLOCKWISE_90 -> if (mirrorX) Vec3i(-vec.z, vec.y, -vec.x) else Vec3i(vec.z, vec.y, -vec.x) // ccw 90
    Rotation.CLOCKWISE_180 -> if (mirrorX) Vec3i(vec.x, vec.y, -vec.z) else Vec3i(-vec.x, vec.y, -vec.z)
    Rotation.COUNTERCLOCKWISE_90 -> if (mirrorX) Vec3i(vec.z, vec.y, vec.x) else Vec3i(-vec.z, vec.y, vec.x) // cw 90
}

fun IBlockState.withMirrorX(mirrorX: Boolean): IBlockState = if (mirrorX) withMirror(Mirror.FRONT_BACK) else this

fun BlockPos.offsetWithRotation(offset: Vec3i, rotation: Rotation, mirrorX: Boolean): BlockPos =
    this + rotation.rotate(offset, mirrorX)

fun BlockPos.offsetWithRotation(offsets: Sequence<Vec3i>, rotation: Rotation, mirrorX: Boolean): Sequence<BlockPos> =
    offsets.map { offsetWithRotation(it, rotation, mirrorX) }

fun BlockPos.invOffsetWithRotation(originPos: BlockPos, rotation: Rotation, mirrorX: Boolean): Vec3i =
    rotation.unrotate(this - originPos, mirrorX)

fun Rotation.rotate(pos: BlockPos, axisPos: BlockPos, mirrorX: Boolean): BlockPos =
    axisPos.offsetWithRotation(pos - axisPos, this, mirrorX)

fun TileEntity.isInInteractionRange(entity: Entity): Boolean = entity.getDistanceSqToCenter(pos) <= 64.0
