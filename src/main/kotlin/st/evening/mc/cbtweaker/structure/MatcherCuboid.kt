package st.evening.mc.cbtweaker.structure

import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatch
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.util.rotate
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath

class MatcherCuboid private constructor(
    val posMatcherTable: Map<Vec3i, StructureBlockMatcher>,
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val minZ: Int,
    val maxZ: Int
) {
    companion object {
        context(_: JsonPath)
        fun load(
            blueprint: Array<Array<CharArray>>,
            origin: Vec3i,
            palette: Char2ObjectMap<StructureBlockMatcher>
        ): MatcherCuboid {
            val posMatcherTable = mutableMapOf<Vec3i, StructureBlockMatcher>()
            val originX = origin.x
            val originY = origin.y
            val originZ = origin.z
            var minX = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            var minY = Int.MAX_VALUE
            var maxY = Int.MIN_VALUE
            var minZ = Int.MAX_VALUE
            var maxZ = Int.MIN_VALUE
            for (arrY in blueprint.indices) {
                val ySlice = blueprint[arrY]
                for (arrZ in ySlice.indices) {
                    val zSlice = ySlice[arrZ]
                    for (arrX in zSlice.indices) {
                        val c = zSlice[arrX]
                        if (c == ' ') continue
                        val matcher = palette[c]
                            ?: throw SerializationException.withPath("Character not in palette: $c")
                        val x = arrX - originX
                        val y = arrY - originY
                        val z = arrZ - originZ
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                        if (z < minZ) minZ = z
                        if (z > maxZ) maxZ = z
                        posMatcherTable[Vec3i(x, y, z)] = matcher
                    }
                }
            }
            return MatcherCuboid(posMatcherTable, minX, maxX, minY, maxY, minZ, maxZ)
        }

        private fun computeOffsetPos(basePos: BlockPos, offset: Vec3i, rotation: Rotation, mirror: Boolean): BlockPos =
            basePos.add(rotation.rotate(offset, mirror))
    }

    val sizeX: Int
        get() = maxX - minX + 1
    val sizeY: Int
        get() = maxY - minY + 1
    val sizeZ: Int
        get() = maxZ - minZ + 1

    fun computePositions(originPos: BlockPos, rotation: Rotation, mirror: Boolean): Sequence<BlockPos> =
        posMatcherTable.keys.asSequence().map { computeOffsetPos(originPos, it, rotation, mirror) }

    fun tryMatch(world: World, originPos: BlockPos, rotation: Rotation, mirror: Boolean): StructureMatch? {
        val structure = StructureMatch()
        posMatcherTable.forEach { (offset, matcher) ->
            val pos = computeOffsetPos(originPos, offset, rotation, mirror)
            val match = matcher.matchBlock(world, pos, rotation) ?: return null
            structure.addPosition(pos)
            when (match) {
                is StructureBlockMatch.Component -> structure.addComponent(match.componentId)
                is StructureBlockMatch.Hatch -> structure.addHatch(match.groupId, match.hatch)
                else -> {}
            }
        }
        return structure
    }
}
