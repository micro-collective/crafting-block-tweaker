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
import st.evening.mc.cbtweaker.structure.StructureParts
import st.evening.mc.cbtweaker.structure.StructureVisualization
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher
import st.evening.mc.cbtweaker.structure.block.impl.MultiBlockControllerStructureBlockMatcher
import st.evening.mc.cbtweaker.util.config.BlockArrayHelper
import st.evening.mc.cbtweaker.util.getRotationFromNorth
import st.evening.mc.cbtweaker.util.invOffsetWithRotation
import st.evening.mc.cbtweaker.util.offsetWithRotation
import st.evening.mc.prelude.api.data.ser.SerializationException
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectBool
import st.evening.mc.prelude.api.data.tjson.expectInt
import st.evening.mc.prelude.api.data.tjson.expectIntValue
import st.evening.mc.prelude.api.data.tjson.useArray
import st.evening.mc.prelude.api.data.tjson.useArrayValue
import st.evening.mc.prelude.api.data.tjson.useObjectValue
import st.evening.mc.prelude.api.data.tjson.useString
import st.evening.mc.prelude.api.resource
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.math.ceilDivPos
import st.evening.mc.prelude.api.util.math.minus
import st.evening.mc.prelude.api.util.math.plus
import st.evening.mc.prelude.api.util.math.times
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace

class LinearStructureMatcher(
    private val startRegion: MatcherCuboid,
    private val repeatRegion: MatcherCuboid,
    private val endRegion: MatcherCuboid?,
    private val repeatDir: RepeatDir,
    private val minInstances: Int,
    private val maxInstances: Int,
    private val allowMirror: Boolean
) : StructureMatcher<LinearStructureMatcher.MatchData> {
    private val repeatLength: Int = repeatDir.axis.getLength(repeatRegion.sizeX, repeatRegion.sizeY, repeatRegion.sizeZ)

    private val visTables: Array<Map<Vec3i, StructureBlockMatcher>> by lazy {
        // set up array with base case
        @Suppress("UNCHECKED_CAST")
        val tables = arrayOfNulls<MutableMap<Vec3i, StructureBlockMatcher>>(maxInstances - minInstances + 1)
            as Array<MutableMap<Vec3i, StructureBlockMatcher>>
        val first = startRegion.posMatcherTable.toMutableMap()
        for (i in 0..<minInstances) {
            first.addVisualizationBlocks(repeatRegion, i)
        }
        tables[0] = first

        // copy entries over, attaching one more repetition each time
        for (i in 0..<(tables.size - 1)) {
            val table = tables[i].toMutableMap()
            table.addVisualizationBlocks(repeatRegion, minInstances + i)
            tables[i + 1] = table
        }

        // attach end region to each table
        endRegion?.let { region ->
            tables.forEachIndexed { i, table ->
                table.addVisualizationBlocks(region, minInstances + i)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return@lazy tables as Array<Map<Vec3i, StructureBlockMatcher>>
    }

    fun getOffsetCorePos(pos: BlockPos, repeatCount: Int, rotation: Rotation, mirrorX: Boolean): BlockPos =
        pos.offsetWithRotation(repeatDir.face.directionVec * (repeatCount * repeatLength), rotation, mirrorX)

    private fun MutableMap<Vec3i, StructureBlockMatcher>.addVisualizationBlocks(
        region: MatcherCuboid,
        repeatCount: Int
    ) {
        val offset = repeatDir.face.directionVec * (repeatCount * repeatLength)
        region.posMatcherTable.forEach { (pos, matcher) ->
            this[pos + offset] = matcher
        }
    }

    override fun findMatch(
        world: World,
        corePos: BlockPos,
        front: BlockSide,
        prev: MatchData?,
        changedBlocks: Collection<BlockPos>
    ): StructureMatch<MatchData> {
        val rotation = front.getRotationFromNorth()
        val fPrevRepeatCount: Int
        val mPrevRepeatCount: Int
        if (prev != null) {
            prev.markDirty(changedBlocks)
            fPrevRepeatCount = prev.forward.lastMatchRepeatCount
            mPrevRepeatCount = prev.mirror?.lastMatchRepeatCount ?: -2
        } else {
            fPrevRepeatCount = -2
            mPrevRepeatCount = -2
        }
        return StructureMatch.mirrorMatch(
            rotation,
            prev,
            allowMirror,
            MatchData::forward,
            MatchData::mirror,
            { MatchTable(this, world, corePos, rotation, it) },
            ::MatchData,
            { it },
            { table, _, mirrorX -> table.getNewRoi(if (mirrorX) mPrevRepeatCount else fPrevRepeatCount)?.asIterable() },
            { it.getNewRoi(fPrevRepeatCount)?.asIterable() },
            { forward, mirror ->
                val p = forward.getNewRoi(fPrevRepeatCount)
                val q = mirror.getNewRoi(mPrevRepeatCount)
                return@mirrorMatch if (p != null) {
                    if (q != null) (p + q).asIterable() else p.asIterable()
                } else {
                    q?.asIterable()
                }
            },
            MatchTable::tryMatch
        )
    }

    override fun getVisualization(data: MatchData?): StructureVisualization {
        if (data == null) {
            return StructureVisualization(visTables[0], false)
        }
        val forward = data.forward
        val mirror = data.mirror
        return if (mirror == null) {
            if (forward.lastMatchEndIndex >= 0) {
                StructureVisualization(visTables[forward.lastMatchEndIndex - minInstances], false)
            } else {
                getIncompleteVisualization(forward, false)
            }
        } else if (mirror.lastMatchEndIndex >= 0) {
            StructureVisualization(visTables[mirror.lastMatchEndIndex - minInstances], true)
        } else if (forward.lastMatchRepeatCount >= mirror.lastMatchRepeatCount) {
            getIncompleteVisualization(forward, false)
        } else {
            getIncompleteVisualization(mirror, true)
        }
    }

    private fun getIncompleteVisualization(table: MatchTable, mirrorX: Boolean): StructureVisualization =
        StructureVisualization(visTables[(table.lastMatchRepeatCount - minInstances).coerceAtLeast(0)], mirrorX)

    enum class RepeatDir(val face: EnumFacing, val axis: RepeatAxis) {
        X_POS(EnumFacing.EAST, RepeatAxis.X) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(baseSizeX, 0, 0)

            override fun getContainingRepetition(
                offset: Vec3i, baseRegion: MatcherCuboid, repeatRegion: MatcherCuboid
            ): Int = (offset.x - baseRegion.maxX - 1) / repeatRegion.sizeX
        },
        X_NEG(EnumFacing.WEST, RepeatAxis.X) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(-extSizeX, 0, 0)

            override fun getContainingRepetition(
                offset: Vec3i, baseRegion: MatcherCuboid, repeatRegion: MatcherCuboid
            ): Int = (baseRegion.minX - offset.x - 1) / repeatRegion.sizeX
        },
        Y_POS(EnumFacing.UP, RepeatAxis.Y) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(0, baseSizeY, 0)

            override fun getContainingRepetition(
                offset: Vec3i, baseRegion: MatcherCuboid, repeatRegion: MatcherCuboid
            ): Int = (offset.y - baseRegion.maxY - 1) / repeatRegion.sizeY
        },
        Y_NEG(EnumFacing.DOWN, RepeatAxis.Y) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(0, -extSizeY, 0)

            override fun getContainingRepetition(
                offset: Vec3i, baseRegion: MatcherCuboid, repeatRegion: MatcherCuboid
            ): Int = (baseRegion.minY - offset.y - 1) / repeatRegion.sizeY
        },
        Z_POS(EnumFacing.SOUTH, RepeatAxis.Z) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(0, 0, baseSizeZ)

            override fun getContainingRepetition(
                offset: Vec3i, baseRegion: MatcherCuboid, repeatRegion: MatcherCuboid
            ): Int = (offset.z - baseRegion.maxZ - 1) / repeatRegion.sizeZ
        },
        Z_NEG(EnumFacing.NORTH, RepeatAxis.Z) {
            override fun getConnectionPosition(
                baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
            ): Vec3i = Vec3i(0, 0, -extSizeZ)

            override fun getContainingRepetition(
                offset: Vec3i, baseRegion: MatcherCuboid, repeatRegion: MatcherCuboid
            ): Int = (baseRegion.minZ - offset.z - 1) / repeatRegion.sizeZ
        };

        abstract fun getConnectionPosition(
            baseSizeX: Int, baseSizeY: Int, baseSizeZ: Int, extSizeX: Int, extSizeY: Int, extSizeZ: Int
        ): Vec3i

        abstract fun getContainingRepetition(offset: Vec3i, baseRegion: MatcherCuboid, repeatRegion: MatcherCuboid): Int

        companion object {
            context(_: JsonPath)
            fun load(str: String): RepeatDir = when (RelativeFace.serializer.deserializeFromJson(str)) {
                RelativeFace.FRONT -> Z_NEG
                RelativeFace.LEFT -> X_POS
                RelativeFace.BACK -> Z_POS
                RelativeFace.RIGHT -> X_NEG
                RelativeFace.UP -> Y_POS
                RelativeFace.DOWN -> Y_NEG
            }
        }
    }

    enum class RepeatAxis {
        X {
            override fun getLength(sizeX: Int, sizeY: Int, sizeZ: Int): Int = sizeX

            override fun getLength(region: MatcherCuboid): Int = region.sizeX

            override fun checkFlush(
                aSizeX: Int, aSizeY: Int, aSizeZ: Int, bSizeX: Int, bSizeY: Int, bSizeZ: Int
            ): Boolean = aSizeY == bSizeY && aSizeZ == bSizeZ
        },
        Y {
            override fun getLength(sizeX: Int, sizeY: Int, sizeZ: Int): Int = sizeY

            override fun getLength(region: MatcherCuboid): Int = region.sizeY

            override fun checkFlush(
                aSizeX: Int, aSizeY: Int, aSizeZ: Int, bSizeX: Int, bSizeY: Int, bSizeZ: Int
            ): Boolean = aSizeX == bSizeX && aSizeZ == bSizeZ
        },
        Z {
            override fun getLength(sizeX: Int, sizeY: Int, sizeZ: Int): Int = sizeZ

            override fun getLength(region: MatcherCuboid): Int = region.sizeZ

            override fun checkFlush(
                aSizeX: Int, aSizeY: Int, aSizeZ: Int, bSizeX: Int, bSizeY: Int, bSizeZ: Int
            ): Boolean = aSizeX == bSizeX && aSizeY == bSizeY
        };

        abstract fun getLength(sizeX: Int, sizeY: Int, sizeZ: Int): Int

        abstract fun getLength(region: MatcherCuboid): Int

        abstract fun checkFlush(aSizeX: Int, aSizeY: Int, aSizeZ: Int, bSizeX: Int, bSizeY: Int, bSizeZ: Int): Boolean
    }

    class MatchData(val forward: MatchTable, val mirror: MatchTable?) {
        fun markDirty(changedBlocks: Collection<BlockPos>) {
            forward.markDirty(changedBlocks)
            mirror?.markDirty(changedBlocks)
        }
    }

    class MatchTable(
        private val matcher: LinearStructureMatcher,
        world: World,
        originPos: BlockPos,
        rotation: Rotation,
        mirrorX: Boolean
    ) {
        private val startMatcher: MatcherCuboid.IncrementalMatcher =
            MatcherCuboid.IncrementalMatcher(matcher.startRegion, world, originPos, rotation, mirrorX)
        private val repeatMatchers: Array<MatcherCuboid.IncrementalMatcher> = Array(matcher.maxInstances) { i ->
            MatcherCuboid.IncrementalMatcher(
                matcher.repeatRegion,
                world,
                matcher.getOffsetCorePos(originPos, i, rotation, mirrorX),
                rotation,
                mirrorX
            )
        }
        private val endMatchers: Array<MatcherCuboid.IncrementalMatcher>?
        private val endRegionRepeatDepth: Int

        var lastMatchRepeatCount: Int = -1
            private set
        var lastMatchEndIndex: Int = -1
            private set

        init {
            val endRegion = matcher.endRegion
            if (endRegion != null) {
                endMatchers = Array(matcher.maxInstances - matcher.minInstances + 1) { i ->
                    MatcherCuboid.IncrementalMatcher(
                        endRegion,
                        world,
                        matcher.getOffsetCorePos(originPos, matcher.minInstances + i, rotation, mirrorX),
                        rotation,
                        mirrorX
                    )
                }
                val axis = matcher.repeatDir.axis
                endRegionRepeatDepth = (axis.getLength(endRegion) ceilDivPos axis.getLength(matcher.repeatRegion)) - 1
            } else {
                endMatchers = null
                endRegionRepeatDepth = -1
            }
        }

        fun markDirty(positions: Collection<BlockPos>) {
            positions.forEach { pos ->
                val repeatIndex = matcher.repeatDir.getContainingRepetition(
                    pos.invOffsetWithRotation(startMatcher.originPos, startMatcher.rotation, startMatcher.mirrorX),
                    matcher.startRegion,
                    matcher.repeatRegion
                ).coerceAtMost(matcher.maxInstances)
                if (repeatIndex < 0) {
                    startMatcher.markDirty(pos)
                } else {
                    if (repeatIndex < matcher.maxInstances) {
                        repeatMatchers[repeatIndex].markDirty(pos)
                    }
                    if (endMatchers != null) {
                        val endIndex = repeatIndex - matcher.minInstances
                        if (endIndex >= 0) {
                            for (i in (endIndex - endRegionRepeatDepth).coerceAtLeast(0)..endIndex) {
                                endMatchers[i].markDirty(pos)
                            }
                        }
                    }
                }
            }
        }

        fun tryMatch(): StructureParts? {
            val startMatch = startMatcher.tryMatch()
            if (startMatch == null) {
                lastMatchRepeatCount = -1
                lastMatchEndIndex = -1
                return null
            }

            var repeatCount = 0
            while (repeatCount < matcher.maxInstances) {
                if (repeatMatchers[repeatCount].tryMatch() == null) break
                ++repeatCount
            }
            lastMatchRepeatCount = repeatCount
            val minInstances = matcher.minInstances
            if (repeatCount < minInstances) {
                lastMatchEndIndex = -1
                return null
            }

            val endMatch = endMatchers?.let endMatch@{ matchers ->
                while (repeatCount >= minInstances) { // the end region could be a superstructure of the repeat region
                    matchers[repeatCount - minInstances].tryMatch()?.let { return@endMatch it }
                    repeatCount--
                }
                lastMatchEndIndex = -1
                return null
            }
            lastMatchEndIndex = repeatCount

            return StructureParts.fromMatches { visit ->
                startMatch.forEach { (pos, match) ->
                    visit(pos, match)
                }
                val iter = repeatMatchers.iterator()
                repeat(repeatCount) {
                    iter.next().getLastMatch().forEach { (pos, match) ->
                        visit(pos, match)
                    }
                }
                endMatch?.forEach { (pos, match) ->
                    visit(pos, match)
                }
            }
        }

        fun getRoi(): Sequence<BlockPos> {
            if (startMatcher.lastMatchSuccess != true) {
                return startMatcher.computePositions()
            }
            val seqs = mutableListOf(startMatcher.computePositions())
            run {
                val endMatchers = this.endMatchers
                if (endMatchers != null) {
                    for (i in 0..<matcher.minInstances) {
                        val repeatMatcher = repeatMatchers[i]
                        seqs += repeatMatcher.computePositions()
                        if (repeatMatcher.lastMatchSuccess != true) return@run
                    }
                    for (i in matcher.minInstances..<matcher.maxInstances) {
                        seqs += endMatchers[i - matcher.minInstances].computePositions()
                        val repeatMatcher = repeatMatchers[i]
                        seqs += repeatMatcher.computePositions()
                        if (repeatMatcher.lastMatchSuccess != true) return@run
                    }
                    // if control reaches here, then all the repeat matchers must have succeeded
                    seqs += endMatchers.last().computePositions()
                } else {
                    for (i in 0..<matcher.maxInstances) {
                        val repeatMatcher = repeatMatchers[i]
                        seqs += repeatMatcher.computePositions()
                        if (repeatMatcher.lastMatchSuccess != true) return@run
                    }
                }
            }
            return seqs.asSequence().flatten()
        }

        fun getNewRoi(prevRepeatCount: Int): Sequence<BlockPos>? =
            orNull(prevRepeatCount != lastMatchRepeatCount) { getRoi() }
    }

    object Type : StructureMatcherType<MatchData> {
        override val id: ResourceLocation = CbTweaker.resource("linear")

        context(_: JsonPath)
        override fun loadMatcher(mbType: MultiBlockType<*>, dto: TJson.Object): LinearStructureMatcher {
            val repeatDir = dto.useString("repeat_direction") { RepeatDir.load(it) } ?: RepeatDir.Z_POS
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
                dto.expectBool("allow_mirror") ?: false
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
                    "Dimension mismatch for $name region! " +
                        "Parent dimensions: ($ssX, $ssY, $ssZ); region dimensions: ($esX, $esY, $esZ)!",
                )
            }
            return MatcherCuboid.load(
                blockArray,
                ctrlPos - repeatDir.getConnectionPosition(ssX, ssY, ssZ, esX, esY, esZ),
                palette
            )
        }
    }
}
