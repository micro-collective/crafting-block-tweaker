package st.evening.mc.cbtweaker.util.recipe

import it.unimi.dsi.fastutil.objects.Object2FloatMap
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientMatcher

class IngredientMatcherMap {
    private val ingMatcherTable: MutableMap<BufferType<*, *, *, *>, List<IngredientMatcher<*, *>>> = mutableMapOf()

    val bufferTypes: Set<BufferType<*, *, *, *>>
        get() = ingMatcherTable.keys

    operator fun <A, JA> set(bufType: BufferType<*, A, *, JA>, matchers: List<IngredientMatcher<A, JA>>) {
        ingMatcherTable[bufType] = matchers
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <A, JA> get(bufType: BufferType<*, A, *, JA>): List<IngredientMatcher<A, JA>> =
        ingMatcherTable[bufType] as? List<IngredientMatcher<A, JA>> ?: emptyList()

    fun forEach(visitor: Visitor): Boolean {
        ingMatcherTable.forEach { (bufType, matchers) ->
            if (
                @Suppress("UNCHECKED_CAST")
                !visitor.visitMatchers(
                    bufType as BufferType<*, Any, *, Any>,
                    matchers as List<IngredientMatcher<Any, Any>>
                )
            ) {
                return false
            }
        }
        return true
    }

    interface Visitor {
        fun <B, A, JB, JA> visitMatchers(
            bufType: BufferType<B, A, JB, JA>,
            matchers: List<IngredientMatcher<A, JA>>
        ): Boolean
    }
}

fun Map<String, IngredientMatcherMap>.checkInputs(
    accs: LazyAccumulatorMap,
    consumeFactors: Object2FloatMap<String>,
    checker: MatcherChecker
): Boolean {
    if (keys.any { it !in accs }) return false
    val simAccs = accs.copyAccumulators()
    val visitor = MatcherChecker.MutableVisitor(checker)
    forEach { (bufGroupId, inputMap) ->
        visitor.accGroup = simAccs[bufGroupId]!!
        visitor.consumeFactor = consumeFactors.getFloat(bufGroupId)
        if (!inputMap.forEach(visitor)) return false
    }
    return true
}

interface MatcherChecker {
    fun <B, A, JB, JA> checkMatchers(
        bufType: BufferType<B, A, JB, JA>,
        consumeFactor: Float,
        acc: Lazy<A>,
        matchers: List<IngredientMatcher<A, JA>>
    ): Boolean

    class MutableVisitor(private val checker: MatcherChecker) : IngredientMatcherMap.Visitor {
        lateinit var accGroup: LazyAccumulatorMap.Group
        var consumeFactor: Float = 1F

        override fun <B, A, JB, JA> visitMatchers(
            bufType: BufferType<B, A, JB, JA>,
            matchers: List<IngredientMatcher<A, JA>>
        ): Boolean = checker.checkMatchers(bufType, consumeFactor, accGroup.getAccumulator(bufType), matchers)
    }

    object Initial : MatcherChecker {
        override fun <B, A, JB, JA> checkMatchers(
            bufType: BufferType<B, A, JB, JA>,
            consumeFactor: Float,
            acc: Lazy<A>,
            matchers: List<IngredientMatcher<A, JA>>
        ): Boolean = matchers.all { it.consumeInitial(acc, consumeFactor, true) }
    }

    object Periodic : MatcherChecker {
        override fun <B, A, JB, JA> checkMatchers(
            bufType: BufferType<B, A, JB, JA>,
            consumeFactor: Float,
            acc: Lazy<A>,
            matchers: List<IngredientMatcher<A, JA>>
        ): Boolean = matchers.all { it.consumePeriodic(acc, consumeFactor, true) }
    }
}

fun Map<String, IngredientMatcherMap>.useInputs(
    accs: LazyAccumulatorMap,
    consumeFactors: Object2FloatMap<String>,
    consumer: MatcherConsumer
) {
    val visitor = MatcherConsumer.MutableVisitor(consumer)
    forEach { (bufGroupId, inputMap) ->
        visitor.accGroup = accs[bufGroupId]!!
        visitor.consumeFactor = consumeFactors.getFloat(bufGroupId)
        inputMap.forEach(visitor)
    }
}

interface MatcherConsumer {
    fun <B, A, JB, JA> useMatchers(
        bufType: BufferType<B, A, JB, JA>,
        consumeFactor: Float,
        acc: Lazy<A>,
        matchers: List<IngredientMatcher<A, JA>>
    )

    class MutableVisitor(private val consumer: MatcherConsumer) : IngredientMatcherMap.Visitor {
        lateinit var accGroup: LazyAccumulatorMap.Group
        var consumeFactor: Float = 1F

        override fun <B, A, JB, JA> visitMatchers(
            bufType: BufferType<B, A, JB, JA>,
            matchers: List<IngredientMatcher<A, JA>>
        ): Boolean {
            consumer.useMatchers(bufType, consumeFactor, accGroup.getAccumulator(bufType), matchers)
            return true
        }
    }

    object Initial : MatcherConsumer {
        override fun <B, A, JB, JA> useMatchers(
            bufType: BufferType<B, A, JB, JA>,
            consumeFactor: Float,
            acc: Lazy<A>,
            matchers: List<IngredientMatcher<A, JA>>
        ) {
            matchers.forEach {
                it.consumeInitial(acc, consumeFactor, false)
            }
        }
    }

    object Periodic : MatcherConsumer {
        override fun <B, A, JB, JA> useMatchers(
            bufType: BufferType<B, A, JB, JA>,
            consumeFactor: Float,
            acc: Lazy<A>,
            matchers: List<IngredientMatcher<A, JA>>
        ) {
            matchers.forEach {
                it.consumePeriodic(acc, consumeFactor, false)
            }
        }
    }
}
