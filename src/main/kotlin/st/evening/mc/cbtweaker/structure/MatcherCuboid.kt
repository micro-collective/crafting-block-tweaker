package st.evening.mc.cbtweaker.structure

import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatch
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.util.invOffsetWithRotation
import st.evening.mc.cbtweaker.util.offsetWithRotation
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
    }

    val sizeX: Int
        get() = maxX - minX + 1
    val sizeY: Int
        get() = maxY - minY + 1
    val sizeZ: Int
        get() = maxZ - minZ + 1

    fun computePositions(originPos: BlockPos, rotation: Rotation, mirrorX: Boolean): Sequence<BlockPos> =
        originPos.offsetWithRotation(posMatcherTable.keys.asSequence(), rotation, mirrorX)

    fun tryMatch(world: World, originPos: BlockPos, rotation: Rotation, mirrorX: Boolean): List<StructureBlockMatch>? {
        return posMatcherTable.map { (offset, matcher) ->
            matcher.matchBlock(world, originPos.offsetWithRotation(offset, rotation, mirrorX), rotation, mirrorX)
                ?: return null
        }
    }

    class IncrementalMatcher(
        val matchRegion: MatcherCuboid,
        val world: World,
        val originPos: BlockPos,
        val rotation: Rotation,
        val mirrorX: Boolean
    ) {
        private val toCheck: MutableSet<BlockPos> =
            matchRegion.computePositions(originPos, rotation, mirrorX).toMutableSet()
        private val failedChecks: MutableSet<BlockPos> = mutableSetOf()
        private val cachedMatch: MutableMap<BlockPos, StructureBlockMatch> = mutableMapOf()

        var lastMatchSuccess: Boolean? = null
            private set

        val dirtyCount: Int
            get() = toCheck.size + failedChecks.size

        // returns garbage if lastMatchSuccess != true
        fun getLastMatch(): Map<BlockPos, StructureBlockMatch> = cachedMatch

        fun markDirty(pos: BlockPos) {
            toCheck += pos
            failedChecks -= pos
            cachedMatch -= pos
            lastMatchSuccess = null
        }

        fun markDirty(positions: Collection<BlockPos>) {
            toCheck.addAll(positions)
            val positionSet = positions.toSet()
            failedChecks.removeAll(positionSet)
            cachedMatch -= positionSet
            lastMatchSuccess = null
        }

        fun tryMatch(): Map<BlockPos, StructureBlockMatch>? {
            toCheck.forEach { pos ->
                val matcher = matchRegion.posMatcherTable[pos.invOffsetWithRotation(originPos, rotation, mirrorX)]
                    ?: return@forEach
                val match = matcher.matchBlock(world, pos, rotation, mirrorX)
                if (match != null) {
                    cachedMatch[pos] = match
                } else {
                    failedChecks += pos
                }
            }
            toCheck.clear()
            if (failedChecks.isEmpty()) {
                lastMatchSuccess = true
                // could copy this, but it's unlikely that the old result will be needed after a new tryMatch call
                return cachedMatch
            } else {
                lastMatchSuccess = false
                return null
            }
        }

        fun computePositions(): Sequence<BlockPos> = matchRegion.computePositions(originPos, rotation, mirrorX)
    }
}
