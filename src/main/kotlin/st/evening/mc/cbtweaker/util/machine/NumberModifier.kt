package st.evening.mc.cbtweaker.util.machine

import net.minecraft.util.math.MathHelper
import st.evening.mc.prelude.api.data.ser.EnumSerializer
import st.evening.mc.prelude.api.data.ser.JsonSerializer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.TJsonDsl
import st.evening.mc.prelude.api.data.tjson.useStringValue
import kotlin.math.pow

class NumberModifier {
    companion object {
        private const val SER_OP: String = "op"
        private const val SER_VALUE: String = "value"
    }

    private var modMultiply: Double = 1.0
    private var modAddMultiply: Double = 0.0
    private var modAddFlat: Double = 0.0

    fun addModifier(op: Operation, modValue: Double, multiplicity: Int) {
        when (op) {
            Operation.MULTIPLY -> modMultiply *= modValue.pow(multiplicity)
            Operation.ADD_MULTIPLY -> modAddMultiply += modValue * multiplicity
            Operation.ADD_FLAT -> modAddFlat += modValue * multiplicity
        }
    }

    fun addModifier(mod: Modifier, multiplicity: Int) {
        addModifier(mod.operation, mod.modifierValue, multiplicity)
    }

    fun modify(value: Double): Double = value * modMultiply * (1.0 + modAddMultiply) + modAddFlat

    fun modifyIntCeil(value: Int): Int = MathHelper.ceil(modify(value.toDouble()))

    fun modifyIntFloor(value: Int): Int = MathHelper.floor(modify(value.toDouble()))

    data class Modifier(val operation: Operation, val modifierValue: Double) {
        object Serializer : JsonSerializer<Modifier, TJson.Object> {
            override fun serializeToJson(x: Modifier): TJson.Object = TJsonDsl.obj {
                SER_OP string x.operation.name
                SER_VALUE double x.modifierValue
            }

            context(_: JsonPath)
            override fun deserializeFromJson(dto: TJson.Object): Modifier = Modifier(
                dto.useStringValue(SER_OP) {
                    Operation.serializer.deserializeFromJson(it)
                },
                dto.getDoubleValue(SER_VALUE)
            )
        }
    }

    enum class Operation {
        MULTIPLY, ADD_MULTIPLY, ADD_FLAT;

        companion object {
            val serializer: EnumSerializer<Operation> = EnumSerializer()
        }
    }
}

fun <T> Map<T, NumberModifier>.tryModify(key: T, value: Double): Double = this[key]?.modify(value) ?: value

fun <T> Map<T, NumberModifier>.tryModifyIntCeil(key: T, value: Int): Int = this[key]?.modifyIntCeil(value) ?: value

fun <T> Map<T, NumberModifier>.tryModifyIntFloor(key: T, value: Int): Int = this[key]?.modifyIntFloor(value) ?: value
