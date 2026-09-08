package st.evening.mc.cbtweaker.util.world

import st.evening.mc.prelude.api.util.world.RelativeFace

object AllFaces : Set<RelativeFace> {
    override val size: Int
        get() = RelativeFace.entries.size

    override fun contains(element: RelativeFace): Boolean = true

    override fun containsAll(elements: Collection<RelativeFace>): Boolean = true

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<RelativeFace> = RelativeFace.entries.iterator()
}
