package st.evening.mc.cbtweaker.structure.impl

import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.multiblock.MultiBlockType
import st.evening.mc.cbtweaker.structure.MatcherCuboid
import st.evening.mc.cbtweaker.structure.StructureMatch
import st.evening.mc.cbtweaker.structure.StructureMatcher
import st.evening.mc.cbtweaker.structure.StructureMatcherType
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.impl.MultiBlockControllerStructureBlockMatcher
import st.evening.mc.cbtweaker.util.BlockArrayHelper
import st.evening.mc.cbtweaker.util.getRotationFromNorth
import st.evening.mc.cbtweaker.util.rotate
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectIntValue
import st.evening.mc.prelude.api.data.tjson.useArray
import st.evening.mc.prelude.api.data.tjson.useArrayValue
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.math.minus
import st.evening.mc.prelude.api.util.math.plus
import st.evening.mc.prelude.api.util.world.BlockSide

class LinearStructureMatcher(
    private val startRegion: MatcherCuboid,
    private val repeatRegion: MatcherCuboid,
    private val endRegion: MatcherCuboid?,
    private val repeatDir: RepeatDir,
    private val minInstances: Int,
    private val maxInstances: Int,
    private val allowMirror: Boolean
) : StructureMatcher {
    private val repeatLength: Int = repeatDir.axis.getLength(repeatRegion.sizeX, repeatRegion.sizeY, repeatRegion.sizeZ)

    override val visualization: Map<Vec3i, StructureBlockMatcher> by lazy {
        val matchers = mutableMapOf<Vec3i, StructureBlockMatcher>()
        val dirVec = repeatDir.face.directionVec
        for (i in 0..<minInstances) { // TODO configurable length visualization
            addVisualizationBlocks(matchers, repeatRegion, dirVec, i)
        }
        if (endRegion != null) {
            addVisualizationBlocks(matchers, endRegion, dirVec, minInstances)
        }
        return@lazy matchers
    }

    override fun getRegion(world: World, corePos: BlockPos, front: BlockSide): MutableIterator<BlockPos> {
        val rotation = front.getRotationFromNorth()
        val positions = mutableSetOf<BlockPos>()
        addBlocks(positions, corePos, rotation, false)
        if (allowMirror) {
            addBlocks(positions, corePos, rotation, true)
        }
        return positions.iterator()
    }

    private fun addBlocks(dest: MutableSet<BlockPos>, corePos: BlockPos, rotation: Rotation, mirror: Boolean) {
        startRegion.computePositions(corePos, rotation, mirror).forEach(dest::add)
        if (endRegion != null) {
            for (i in 0..<minInstances) {
                addBlocksForRegion(dest, repeatRegion, corePos, rotation, mirror, i)
            }
            addBlocksForRegion(dest, endRegion, corePos, rotation, mirror, minInstances)
            for (i in minInstances..<maxInstances) {
                addBlocksForRegion(dest, repeatRegion, corePos, rotation, mirror, i)
                addBlocksForRegion(dest, endRegion, corePos, rotation, mirror, i + 1)
            }
        } else {
            for (i in 0..<maxInstances) {
                addBlocksForRegion(dest, repeatRegion, corePos, rotation, mirror, i)
            }
        }
    }

    private fun addBlocksForRegion(
        dest: MutableSet<BlockPos>,
        region: MatcherCuboid,
        corePos: BlockPos,
        rotation: Rotation,
        mirror: Boolean,
        repeatCount: Int
    ) {
        region.computePositions(
            rotation.rotate(corePos.offset(repeatDir.face, repeatCount * repeatLength), corePos, mirror),
            rotation,
            mirror
        ).forEach(dest::add)
    }

    override fun findMatch(world: World, corePos: BlockPos, front: BlockSide): StructureMatch? {
        val rotation = front.getRotationFromNorth()
        val match = findMatch(world, corePos, rotation, false)
        if (match != null) return match
        return orNull(allowMirror) { findMatch(world, corePos, rotation, true) }
    }

    private fun findMatch(world: World, corePos: BlockPos, rotation: Rotation, mirror: Boolean): StructureMatch? {
        val match = startRegion.tryMatch(world, corePos, rotation, mirror) ?: return null

        var repeatCount = 0
        while (repeatCount < maxInstances) {
            if (tryMatchRegion(match, world, corePos, rotation, mirror, repeatRegion, repeatCount)) break
            ++repeatCount
        }
        if (repeatCount < minInstances) return null

        if (endRegion == null) return match
        if (tryMatchRegion(match, world, corePos, rotation, mirror, endRegion, repeatCount)) return null
        return match
    }

    // returns true if the match fails
    private fun tryMatchRegion(
        dest: StructureMatch, world: World, corePos: BlockPos, rotation: Rotation, mirror: Boolean,
        region: MatcherCuboid, repeatCount: Int
    ): Boolean {
        val offsetCorePos = rotation.rotate(corePos.offset(repeatDir.face, repeatCount * repeatLength), corePos, mirror)
        val match = region.tryMatch(world, offsetCorePos, rotation, mirror) ?: return true
        val offsetVec = corePos.subtract(offsetCorePos)
        dest.addFrom(match, offsetVec.x, offsetVec.y, offsetVec.z)
        return false
    }

    private fun addVisualizationBlocks(
        dest: MutableMap<Vec3i, StructureBlockMatcher>,
        region: MatcherCuboid,
        dirVec: Vec3i,
        repeatCount: Int
    ) {
        val repeat = repeatCount * repeatLength
        val offset = Vec3i(dirVec.x * repeat, dirVec.y * repeat, dirVec.z * repeat)
        region.posMatcherTable.forEach { (pos, matcher) ->
            dest[pos + offset] = matcher
        }
    }

    enum class RepeatDir(val face: EnumFacing, val axis: RepeatAxis) {
        X_POS(EnumFacing.EAST, RepeatAxis.X) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(baseSizeX, 0, 0)
        },
        X_NEG(EnumFacing.WEST, RepeatAxis.X) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(-extSizeX, 0, 0)
        },
        Y_POS(EnumFacing.UP, RepeatAxis.Y) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(0, baseSizeY, 0)
        },
        Y_NEG(EnumFacing.DOWN, RepeatAxis.Y) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(0, -extSizeY, 0)
        },
        Z_POS(EnumFacing.SOUTH, RepeatAxis.Z) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(0, 0, baseSizeZ)
        },
        Z_NEG(EnumFacing.NORTH, RepeatAxis.Z) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(0, 0, -extSizeZ)
        };

        abstract fun getConnectionPosition(
            baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
        ): Vec3i

        companion object {
            context(_: JsonPath)
            fun load(str: String): RepeatDir {
                when (str.lowercase()) {
                    "+x" -> return X_POS
                    "-x" -> return X_NEG
                    "+y" -> return Y_POS
                    "-y" -> return Y_NEG
                    "+z" -> return Z_POS
                    "-z" -> return Z_NEG
                }
                throw SerializationException.withPath("Direction must be of the form [+-][xyz], but got: $str")
            }
        }
    }

    enum class RepeatAxis {
        X {
            override fun getLength(sizeX: Int, sizeY: Int, sizeZ: Int): Int = sizeX

            override fun checkFlush(
                aSizeX: Int, aSizeY: Int, aSizeZ: Int, bSizeX: Int, bSizeY: Int, bSizeZ: Int
            ): Boolean = aSizeY == bSizeY && aSizeZ == bSizeZ
        },
        Y {
            override fun getLength(sizeX: Int, sizeY: Int, sizeZ: Int): Int = sizeY

            override fun checkFlush(
                aSizeX: Int, aSizeY: Int, aSizeZ: Int, bSizeX: Int, bSizeY: Int, bSizeZ: Int
            ): Boolean = aSizeX == bSizeX && aSizeZ == bSizeZ
        },
        Z {
            override fun getLength(sizeX: Int, sizeY: Int, sizeZ: Int): Int = sizeZ

            override fun checkFlush(
                aSizeX: Int, aSizeY: Int, aSizeZ: Int, bSizeX: Int, bSizeY: Int, bSizeZ: Int
            ): Boolean = aSizeX == bSizeX && aSizeY == bSizeY
        };

        abstract fun getLength(sizeX: Int, sizeY: Int, sizeZ: Int): Int

        abstract fun checkFlush(aSizeX: Int, aSizeY: Int, aSizeZ: Int, bSizeX: Int, bSizeY: Int, bSizeZ: Int): Boolean
    }

    object Type : StructureMatcherType {
        override val id: ResourceLocation = CbTweaker.resource("linear")

        context(_: JsonPath)
        override fun loadMatcher(mbType: MultiBlockType<*>, dto: TJson.Object): StructureMatcher {
            val repeatDir = dto.useStringValue("repeat_direction") { RepeatDir.load(it) }
            val palette = dto.useObjectValue("palette") { BlockArrayHelper.loadPalette(it) }
            palette.put('@', MultiBlockControllerStructureBlockMatcher(mbType))

            val startBlkArr = dto.useArrayValue("structure_start") { BlockArrayHelper.loadBlockArray(it) }
            val ctrlPos = BlockArrayHelper.findControllerPosition(startBlkArr)
            val startRegion = MatcherCuboid.load(startBlkArr, ctrlPos, palette)
            val startSizeX = startRegion.sizeX
            val startSizeY = startRegion.sizeY
            val startSizeZ = startRegion.sizeZ

            return LinearStructureMatcher(
                startRegion,
                dto.useArrayValue("structure_repeat") {
                    loadExtensionRegion(it, repeatDir, ctrlPos, palette, "repeat", startSizeX, startSizeY, startSizeZ)
                        ?: throw SerializationException.withPath("Repeat region cannot be empty!")
                },
                dto.useArray("structure_end") {
                    loadExtensionRegion(it, repeatDir, ctrlPos, palette, "end", startSizeX, startSizeY, startSizeZ)
                },
                repeatDir,
                dto.expectInt("min_instances") ?: 0,
                dto.expectIntValue("max_instances"),
                dto.expectBool("allow_mirror") ?: true
            )
        }

        context(_: JsonPath)
        private fun loadExtensionRegion(
            blockArrayDto: TJson.Array,
            repeatDir: RepeatDir,
            ctrlPos: Vec3i,
            palette: Char2ObjectMap<StructureBlockMatcher>,
            name: String,
            ssX: Int,
            ssY: Int,
            ssZ: Int
        ): MatcherCuboid? {
            val blockArray = BlockArrayHelper.loadBlockArray(blockArrayDto)
            val extDims = BlockArrayHelper.computeSize(blockArray)
            val esX = extDims.x
            val esY = extDims.y
            val esZ = extDims.z
            if (esX == 0 || esY == 0 || esZ == 0) return null
            if (!repeatDir.axis.checkFlush(ssX, ssY, ssZ, esX, esY, esZ)) {
                throw SerializationException.withPath(
                    "Dimension mismatch for $name region! Expected ($ssX, $ssY, $ssZ) but got ($esX, $esY, $esZ)!",
                )
            }
            val cxnPos = repeatDir.getConnectionPosition(ssX, ssY, ssZ, esX, esY, esZ)
            return MatcherCuboid.load(blockArray, ctrlPos - cxnPos, palette)
        }
    }
}
