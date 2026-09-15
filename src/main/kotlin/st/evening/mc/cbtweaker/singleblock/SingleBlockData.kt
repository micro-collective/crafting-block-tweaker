package st.evening.mc.cbtweaker.singleblock

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.MachineHost
import st.evening.mc.cbtweaker.buffer.BufferObserver
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.serconfig.CopiableConfigHost
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.cbtweaker.util.component.SidedBufferHandler
import st.evening.mc.cbtweaker.util.component.collectComponents
import st.evening.mc.cbtweaker.util.component.handleBlockUpdate
import st.evening.mc.cbtweaker.util.component.handleDestruction
import st.evening.mc.cbtweaker.util.component.handleInteraction
import st.evening.mc.cbtweaker.util.config.putNonEmpty
import st.evening.mc.cbtweaker.util.machine.ComponentSet
import st.evening.mc.cbtweaker.util.machine.MutableComponentSet
import st.evening.mc.cbtweaker.util.machine.RefreshState
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.cbtweaker.util.sync.CbtSyncHelper
import st.evening.mc.cbtweaker.util.world.FrontGetter
import st.evening.mc.prelude.api.data.ser.ServerSideSerializable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.getTileEntityWeakValidity
import st.evening.mc.prelude.api.util.world.onServer

class SingleBlockData<S>(val sbMachine: SingleBlockMachineTileEntity, val sbType: SingleBlockType<S>) :
    MachineHost, BufferObserver, Observer.Simple, Observer.Indexed, CopiableConfigHost, ServerSideSerializable {
    companion object {
        private const val SER_BUFFERS: String = "buffers"
        private const val SER_MACHINE: String = "machine"
    }

    val bufHandler: SidedBufferHandler
    private var bufferDirtyState: RefreshState = RefreshState.NONE

    private val behaviour: MachineBehaviour<S> = sbType.behaviour
    private val machineState: S
    private val ticker: TickModulator = TickModulator(true)

    private var knownActiveState: Boolean = false

    override val weakValidity: WeakValidity
        get() = sbMachine.getTileEntityWeakValidity()

    init {
        val world = sbMachine.world
        val pos = sbMachine.pos
        val bufGroups = sbType.createBufferGroups(world, pos, this)
        this.bufHandler = SidedBufferHandler(FrontGetter(sbMachine), bufGroups)
        this.machineState = sbType.stateFactory.createState(world, pos, bufGroups, collectComponents(), this, null)
        when (val activeState = behaviour.getActiveState(machineState)) {
            null -> {}
            is Piecewise.Atom -> activeState.observeSync(this)
            is Piecewise.Composite -> activeState.observeSync(this)
        }
    }

    override val machineType: CraftingBlockType<*>
        get() = sbType

    val isActive: Boolean
        get() = behaviour.isActive(machineState)

    val rsHandler: RedstoneControlHandler?
        get() = behaviour.getRedstoneControlHandler(machineState)

    private fun collectComponents(): ComponentSet {
        val components = MutableComponentSet()
        bufHandler.forEachConfig {
            it.collectComponents(components)
        }
        return components
    }

    override fun onObservableUpdate() { // observing the active state
        val active = isActive
        if (active != knownActiveState) {
            knownActiveState = active
            val pos = sbMachine.pos
            sbMachine.world.markBlockRangeForRenderUpdate(pos, pos)
        }
    }

    override fun onObservableUpdate(index: Int) {
        onObservableUpdate()
    }

    override fun onIngredientsChanged() {
        bufferDirtyState = RefreshState.SOFT_REFRESH
        sbMachine.markDirty()
    }

    override fun onComponentsChanged() {
        bufferDirtyState = RefreshState.HARD_REFRESH
        sbMachine.markDirty()
    }

    @ServerSide
    override fun onMachineStateChanged() {
        sbMachine.markDirty()
    }

    fun tick() {
        if (bufferDirtyState != RefreshState.NONE) {
            behaviour.notifyState(
                machineState,
                orNull(bufferDirtyState == RefreshState.HARD_REFRESH) { collectComponents() }
            )
            bufferDirtyState = RefreshState.NONE
        }
        sbMachine.world.onServer {
            bufHandler.tick()
        }
        behaviour.tick(machineState, ticker)
    }

    fun handleInteraction(
        blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
        face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (behaviour.handleInteraction(machineState, blockState, player, hand, face, hitX, hitY, hitZ)) return true
        bufHandler.forEachEnabledConfig(face) {
            if (it.handleInteraction(blockState, player, hand, face, hitX, hitY, hitZ)) return true
        }
        return false
    }

    @ServerSide
    fun handleBlockUpdate(blockState: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        behaviour.handleBlockUpdate(machineState, blockState, fromBlock, fromPos)
        bufHandler.forEachConfig {
            it.handleBlockUpdate(blockState, fromBlock, fromPos)
        }
    }

    @ServerSide
    fun handleDestruction(blockState: IBlockState) {
        behaviour.handleDestruction(machineState, blockState)
        bufHandler.forEachConfig {
            it.handleDestruction(blockState)
        }
    }

    @ServerSide
    override fun writeConfig(dto: NBTTagCompound) {
        dto.runAction {
            putNonEmpty(SER_BUFFERS) { bufHandler.writeConfig(it) }
            putNonEmpty(SER_MACHINE) { behaviour.writeMachineConfig(machineState, it) }
        }
    }

    @ServerSide
    override fun readConfig(dto: NBTTagCompound) {
        dto.getCompoundOrNull(SER_BUFFERS)?.let {
            bufHandler.readConfig(it)
        }
        dto.getCompoundOrNull(SER_MACHINE)?.let {
            behaviour.readMachineConfig(machineState, it)
        }
    }

    fun getSyncState(): Piecewise? = CbtSyncHelper.buildSyncState {
        bufHandler.getBufferSyncState(this)
        behaviour.getActiveState(machineState)?.let { add(it) }
        behaviour.getMachineSyncState(machineState)?.let { add(it) }
    }

    @ServerSide
    override fun writeToNbtServerSide(dto: NBTTagCompound) {
        dto.runAction {
            SER_BUFFERS tag bufHandler.writeToNbtServerSide()
            SER_MACHINE tag NBTTagCompound().also { behaviour.serializeMachineToNbt(machineState, it) }
        }
    }

    @ServerSide
    override fun readFromNbtServerSide(dto: NBTTagCompound) {
        bufHandler.readFromNbtServerSide(dto.getCompoundTag(SER_BUFFERS))
        behaviour.deserializeMachineFromNbt(machineState, dto.getCompoundTag(SER_MACHINE))
    }

    fun createMachineUiElement(): UiElement? = behaviour.createUiElement(machineState)
}
