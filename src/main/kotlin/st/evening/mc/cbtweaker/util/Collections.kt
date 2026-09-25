package st.evening.mc.cbtweaker.util

import gnu.trove.iterator.TObjectIntIterator
import net.minecraft.util.math.BlockPos

object TObjectIntEmptyIterator : TObjectIntIterator<Any> {
    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    inline operator fun <T> invoke(): TObjectIntIterator<T> = this as TObjectIntIterator<T>

    override fun hasNext(): Boolean = false

    override fun advance() {
        throw NoSuchElementException()
    }

    override fun key(): Any = throw IllegalStateException()

    override fun value(): Int = throw IllegalStateException()

    override fun setValue(`val`: Int): Int = throw IllegalStateException()

    override fun remove() {
        throw IllegalStateException()
    }
}

object BlockPosComparator : Comparator<BlockPos> {
    override fun compare(o1: BlockPos, o2: BlockPos): Int = when {
        o1.y < o2.y -> -1
        o1.y > o2.y -> 1
        o1.x < o2.x -> -1
        o1.x > o2.x -> 1
        o1.z < o2.z -> -1
        o1.z > o2.z -> 1
        else -> 0
    }
}
