package st.evening.mc.cbtweaker.util

import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
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

    fun scaleConsumeInt(amount: Int, scaleFactor: Float, determMode: Boolean, random: Random = cbtRandom): Int {
        val f = amount * scaleFactor
        if (f >= 1) return MathHelper.ceil(f)
        if (f <= 0) return 0
        return if (determMode || random.nextFloat() < f) 1 else 0
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

fun Rotation.rotate(vec: Vec3i, mirror: Boolean): Vec3i = when (this) {
    Rotation.NONE -> if (mirror) Vec3i(-vec.x, vec.y, vec.z) else vec
    Rotation.CLOCKWISE_90 -> if (mirror) Vec3i(vec.z, vec.y, vec.x) else Vec3i(-vec.z, vec.y, vec.x)
    Rotation.CLOCKWISE_180 -> if (mirror) Vec3i(vec.x, vec.y, -vec.z) else Vec3i(-vec.x, vec.y, -vec.z)
    Rotation.COUNTERCLOCKWISE_90 -> if (mirror) Vec3i(-vec.z, vec.y, -vec.x) else Vec3i(vec.z, vec.y, -vec.x)
}

fun Rotation.rotate(pos: BlockPos, axisPos: BlockPos, mirror: Boolean): BlockPos =
    axisPos + rotate(pos - axisPos, mirror)

fun TileEntity.isInInteractionRange(entity: Entity): Boolean = entity.getDistanceSqToCenter(pos) <= 64.0
