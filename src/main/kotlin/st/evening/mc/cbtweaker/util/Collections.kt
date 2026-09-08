package st.evening.mc.cbtweaker.util

import gnu.trove.iterator.TObjectIntIterator

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
