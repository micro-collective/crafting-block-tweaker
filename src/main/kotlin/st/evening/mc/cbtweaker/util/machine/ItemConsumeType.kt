package st.evening.mc.cbtweaker.util.machine

import st.evening.mc.prelude.api.data.ser.EnumSerializer

enum class ItemConsumeType {
    CONSUME, DAMAGE, KEEP;

    companion object {
        val serializer: EnumSerializer<ItemConsumeType> = EnumSerializer()
    }
}
