package st.evening.mc.cbtweaker.structure

import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.hatch.HatchTileEntity
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatch
import st.evening.mc.cbtweaker.util.machine.ComponentSet
import st.evening.mc.cbtweaker.util.machine.MutableComponentSet

data class StructureParts(
    val positions: Set<BlockPos>,
    val hatches: Map<String, List<HatchTileEntity>>,
    val components: ComponentSet
) {
    companion object {
        inline fun fromMatches(action: ((BlockPos, StructureBlockMatch) -> Unit) -> Unit): StructureParts {
            val positions = mutableSetOf<BlockPos>()
            val hatches = mutableMapOf<String, MutableList<HatchTileEntity>>()
            val components = MutableComponentSet()
            action { pos, match ->
                positions += pos
                when (match) {
                    StructureBlockMatch.Normal -> {}
                    is StructureBlockMatch.Hatch -> hatches.getOrPut(match.groupId) { mutableListOf() } += match.hatch
                    is StructureBlockMatch.Component -> components.put(match.componentId, match.count)
                }
            }
            return StructureParts(positions, hatches, components)
        }

        fun fromMatches(matches: Map<BlockPos, StructureBlockMatch>): StructureParts = fromMatches { visit ->
            matches.forEach { (pos, match) ->
                visit(pos, match)
            }
        }
    }
}

sealed interface StructureMatch<M> {
    val data: M

    val newRegion: Iterable<BlockPos>?

    class Fail<M>(override val data: M, override val newRegion: Iterable<BlockPos>?) : StructureMatch<M>

    class Success<M>(
        override val data: M,
        override val newRegion: Iterable<BlockPos>?,
        val parts: StructureParts
    ) : StructureMatch<M>

    companion object {
        inline fun <M, S, R> mirrorMatch(
            rotation: Rotation,
            prev: M?,
            allowMirror: Boolean,
            getForwardState: (M) -> S?,
            getMirrorState: (M) -> S?,
            createState: (mirrorX: Boolean) -> S,
            createData: (forward: S, mirror: S?) -> M,
            getStructureParts: (R) -> StructureParts,
            getSuccessRegion: (S, R, mirrorX: Boolean) -> Iterable<BlockPos>,
            getNoMirrorFailRegion: (S) -> Iterable<BlockPos>,
            getFailRegion: (forward: S, mirror: S) -> Iterable<BlockPos>,
            tryMatch: (S) -> R?
        ): StructureMatch<M> {
            val fState = prev?.let { getForwardState(it) } ?: createState(false)
            val fMatch = tryMatch(fState)
            if (fMatch != null) {
                return Success(
                    createData(fState, prev?.let { getMirrorState(it) }),
                    getSuccessRegion(fState, fMatch, false),
                    getStructureParts(fMatch)
                )
            }
            if (!allowMirror) {
                return Fail(createData(fState, null), getNoMirrorFailRegion(fState))
            }

            val mState = prev?.let { getMirrorState(it) } ?: createState(true)
            val mMatch = tryMatch(mState)
            if (mMatch != null) {
                return Success(
                    createData(fState, mState),
                    getSuccessRegion(mState, mMatch, true),
                    getStructureParts(mMatch)
                )
            }
            return Fail(createData(fState, mState), getFailRegion(fState, mState))
        }
    }
}
