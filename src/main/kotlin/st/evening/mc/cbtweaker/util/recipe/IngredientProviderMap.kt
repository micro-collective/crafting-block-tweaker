package st.evening.mc.cbtweaker.util.recipe

import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.buffer.ingredient.IngredientProvider

class IngredientProviderMap {
    private val ingProviderTable: MutableMap<BufferType<*, *, *, *>, List<IngredientProvider<*, *>>> = mutableMapOf()

    val bufferTypes: Set<BufferType<*, *, *, *>>
        get() = ingProviderTable.keys

    operator fun <A, JA> set(bufType: BufferType<*, A, *, JA>, providers: List<IngredientProvider<A, JA>>) {
        ingProviderTable[bufType] = providers
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <A, JA> get(bufType: BufferType<*, A, *, JA>): List<IngredientProvider<A, JA>> =
        ingProviderTable[bufType] as? List<IngredientProvider<A, JA>> ?: emptyList()

    fun forEach(visitor: Visitor): Boolean {
        ingProviderTable.forEach { (bufType, providers) ->
            if (
                @Suppress("UNCHECKED_CAST")
                !visitor.visitProviders(
                    bufType as BufferType<*, Any, *, Any>,
                    providers as List<IngredientProvider<Any, Any>>
                )
            ) {
                return false
            }
        }
        return true
    }

    interface Visitor {
        fun <B, A, JB, JA> visitProviders(
            bufType: BufferType<B, A, JB, JA>,
            providers: List<IngredientProvider<A, JA>>
        ): Boolean
    }
}

fun Map<String, IngredientProviderMap>.checkOutputs(accs: LazyAccumulatorMap, checker: ProviderChecker): Boolean {
    if (keys.any { it !in accs }) return false
    val simAccs = accs.copyAccumulators()
    val visitor = ProviderChecker.MutableVisitor(checker)
    forEach { (bufGroupId, outputMap) ->
        visitor.accGroup = simAccs[bufGroupId]!!
        if (!outputMap.forEach(visitor)) return false
    }
    return true
}

interface ProviderChecker {
    fun <B, A, JB, JA> checkProviders(
        bufType: BufferType<B, A, JB, JA>,
        acc: Lazy<A>,
        providers: List<IngredientProvider<A, JA>>
    ): Boolean

    class MutableVisitor(private val checker: ProviderChecker) : IngredientProviderMap.Visitor {
        lateinit var accGroup: LazyAccumulatorMap.Group

        override fun <B, A, JB, JA> visitProviders(
            bufType: BufferType<B, A, JB, JA>,
            providers: List<IngredientProvider<A, JA>>
        ): Boolean = checker.checkProviders(bufType, accGroup.getAccumulator(bufType), providers)
    }

    object Final : ProviderChecker {
        override fun <B, A, JB, JA> checkProviders(
            bufType: BufferType<B, A, JB, JA>,
            acc: Lazy<A>,
            providers: List<IngredientProvider<A, JA>>
        ): Boolean = providers.all { it.insertFinal(acc, true) }
    }

    object Periodic : ProviderChecker {
        override fun <B, A, JB, JA> checkProviders(
            bufType: BufferType<B, A, JB, JA>,
            acc: Lazy<A>,
            providers: List<IngredientProvider<A, JA>>
        ): Boolean = providers.all { it.insertPeriodic(acc, true) }
    }
}

fun Map<String, IngredientProviderMap>.useOutputs(accs: LazyAccumulatorMap, consumer: ProviderConsumer) {
    val visitor = ProviderConsumer.MutableVisitor(consumer)
    forEach { (bufGroupId, outputMap) ->
        visitor.accGroup = accs[bufGroupId]!!
        outputMap.forEach(visitor)
    }
}

interface ProviderConsumer {
    fun <B, A, JB, JA> useProviders(
        bufType: BufferType<B, A, JB, JA>,
        acc: Lazy<A>,
        providers: List<IngredientProvider<A, JA>>
    )

    class MutableVisitor(private val consumer: ProviderConsumer) : IngredientProviderMap.Visitor {
        lateinit var accGroup: LazyAccumulatorMap.Group

        override fun <B, A, JB, JA> visitProviders(
            bufType: BufferType<B, A, JB, JA>,
            providers: List<IngredientProvider<A, JA>>
        ): Boolean {
            consumer.useProviders(bufType, accGroup.getAccumulator(bufType), providers)
            return true
        }
    }

    object Final : ProviderConsumer {
        override fun <B, A, JB, JA> useProviders(
            bufType: BufferType<B, A, JB, JA>,
            acc: Lazy<A>,
            providers: List<IngredientProvider<A, JA>>
        ) {
            providers.forEach {
                it.insertFinal(acc, false)
            }
        }
    }

    object Periodic : ProviderConsumer {
        override fun <B, A, JB, JA> useProviders(
            bufType: BufferType<B, A, JB, JA>,
            acc: Lazy<A>,
            providers: List<IngredientProvider<A, JA>>
        ) {
            providers.forEach {
                it.insertPeriodic(acc, false)
            }
        }
    }
}
