package st.evening.mc.cbtweaker.util.machine

import st.evening.mc.prelude.api.data.ser.EnumSerializer
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.math.Arithmetic
import st.evening.mc.prelude.api.util.math.arithMaxOf
import st.evening.mc.prelude.api.util.math.arithMinOf
import st.evening.mc.prelude.api.util.math.arithSumOf

enum class StatMetric {
    MIN {
        override fun <T : Any> compute(arith: Arithmetic<T>, data: Iterable<T>, count: Int): T? =
            arith.run { data.arithMinOf { it } }
    },
    MAX {
        override fun <T : Any> compute(arith: Arithmetic<T>, data: Iterable<T>, count: Int): T? =
            arith.run { data.arithMaxOf { it } }
    },
    MEAN {
        override fun <T : Any> compute(arith: Arithmetic<T>, data: Iterable<T>, count: Int): T? = orNull(count > 0) {
            arith.run {
                data.arithSumOf { it } / fromInt(count)
            }
        }
    },
    MEDIAN {
        override fun <T : Any> compute(arith: Arithmetic<T>, data: Iterable<T>, count: Int): T? = when (count) {
            0 -> null
            1 -> if (data is List<T>) data[0] else data.iterator().next()
            else -> arith.run {
                val sorted = data.sortedWith { x, y -> x.compareTo(y) }
                if (count % 2 == 1) {
                    return@run sorted[count / 2]
                } else {
                    val k = count / 2
                    return@run (sorted[k - 1] + sorted[k]) / fromInt(2)
                }
            }
        }
    };

    abstract fun <T : Any> compute(arith: Arithmetic<T>, data: Iterable<T>, count: Int): T?

    fun <T : Any> compute(arith: Arithmetic<T>, data: Collection<T>): T? = compute(arith, data, data.size)

    companion object {
        val serializer: EnumSerializer<StatMetric> = EnumSerializer()
    }
}
