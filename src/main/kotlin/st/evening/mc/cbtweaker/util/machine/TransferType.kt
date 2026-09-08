package st.evening.mc.cbtweaker.util.machine

import st.evening.mc.prelude.api.data.ser.EnumSerializer

enum class TransferType {
    INSERT, EXTRACT;

    companion object {
        val serializer: EnumSerializer<TransferType> = EnumSerializer()
    }
}
