package st.evening.mc.cbtweaker.util.component

import net.minecraft.nbt.NBTTagString
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.prelude.api.data.ser.GameSerializable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.onObservableUpdate
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.machine.RedstoneBehaviour
import kotlin.enums.enumEntries

class RedstoneControlHandler(private var redstoneState: Boolean) : Piecewise.Atom, GameSerializable<NBTTagString> {
    var redstoneBehaviour: RedstoneBehaviour = RedstoneBehaviour.ACTIVE_LOW
        private set

    private val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()
    private val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

    constructor(world: World, pos: BlockPos) : this(world.isBlockPowered(pos))

    fun updateRedstoneBehaviour(newBehaviour: RedstoneBehaviour) {
        if (redstoneBehaviour !== newBehaviour) {
            redstoneBehaviour = newBehaviour
            stateObservers.onObservableUpdate()
        }
    }

    fun updateRedstoneState(newState: Boolean): Boolean {
        if (redstoneState == newState) return false
        redstoneState = newState
        return true
    }

    fun updateRedstoneState(world: World, pos: BlockPos): Boolean = updateRedstoneState(world.isBlockPowered(pos))

    fun canWork(): Boolean = redstoneBehaviour.canWork(redstoneState)

    override fun observeState(observer: Observer.Simple) {
        stateObservers += observer
    }

    override fun observeSync(observer: Observer.Simple) {
        syncObservers += observer
    }

    override fun writeToNbt(): NBTTagString = RedstoneBehaviour.serializer.serializeToNbt(redstoneBehaviour)

    override fun readFromNbt(dto: NBTTagString) {
        readFromNbt(dto.string)
    }

    fun readFromNbt(strValue: String) {
        redstoneBehaviour = RedstoneBehaviour.serializer.deserializeFromNbt(strValue) ?: RedstoneBehaviour.ACTIVE_LOW
        syncObservers.onObservableUpdate()
    }

    override fun writeToNetwork(buf: PacketBuffer) {
        val mask = redstoneBehaviour.ordinal
        buf.writeByte(if (redstoneState) mask or 0x80 else mask)
    }

    override fun readFromNetwork(buf: PacketBuffer) {
        val mask = buf.readByte().toInt()
        redstoneBehaviour = enumEntries<RedstoneBehaviour>()[mask and 0x7F]
        redstoneState = mask and 0x80 != 0
        syncObservers.onObservableUpdate()
    }
}
