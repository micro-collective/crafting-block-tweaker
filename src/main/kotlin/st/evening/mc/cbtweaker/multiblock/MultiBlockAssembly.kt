package st.evening.mc.cbtweaker.multiblock

import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.MachineHost
import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.buffer.BufferGroups
import st.evening.mc.cbtweaker.buffer.collectComponents
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.hatch.HatchTileEntity
import st.evening.mc.cbtweaker.structure.StructureMatch
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.cbtweaker.util.machine.ComponentSet
import st.evening.mc.cbtweaker.util.machine.MutableComponentSet
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.prelude.api.data.ser.BoolSerializer
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.sync.SyncHost
import st.evening.mc.prelude.api.data.sync.SyncManager
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.network.sendToAllTracking
import st.evening.mc.prelude.api.util.collection.WeakValidity
import st.evening.mc.prelude.api.util.data.orNull
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide

class MultiBlockAssembly<S>(
    private val mbData: MultiBlockData<S>,
    val structureBlocks: Set<BlockPos>,
    private val baseComponents: ComponentSet,
    private val hatches: List<HatchTileEntity>,
    private val bufGroups: BufferGroups,
    oldAssembly: MultiBlockAssembly<S>?
) : MachineHost, SyncHost, NbtCompoundSerializable {
    companion object {
        fun <S> fromStructure(
            mbData: MultiBlockData<S>,
            structMatch: StructureMatch,
            oldAssembly: MultiBlockAssembly<S>?
        ): MultiBlockAssembly<S> {
            val hatches = mutableListOf<HatchTileEntity>()
            val bufGroups = Object2ObjectRBTreeMap<String, BufferGroup>()
            structMatch.hatches.forEach { (groupId, matchHatches) ->
                val group = BufferGroup()
                matchHatches.forEach {
                    hatches += it
                    it.addToGroup(group)
                }
                bufGroups[groupId] = group
            }
            return MultiBlockAssembly(
                mbData, structMatch.positions, structMatch.components, hatches, bufGroups, oldAssembly
            )
        }
    }

    private val behaviour: MachineBehaviour<S> = mbData.mbType.behaviour
    private val machineState: S = mbData.run {
        mbType.stateFactory.createState(
            mbCtrl.world, mbCtrl.pos, bufGroups, collectComponents(), this@MultiBlockAssembly, oldAssembly?.machineState
        )
    }
    private val activeState: ValueStateAtom<Boolean> = ValueStateAtom(false, BoolSerializer)
    private val ticker: TickModulator = TickModulator(true)

    override var weakValidity: WeakValidity = WeakValidity.VALID
        private set

    override val syncManager: SyncManager = SyncManager.create(
        this,
        ListStateComposite.fromStates(
            buildList {
                add(activeState)
                behaviour.getMachineSyncState(machineState)?.let { add(it) }
            }
        )
    )

    override val machineType: CraftingBlockType<*>
        get() = mbData.mbType

    val isActive: Boolean
        get() = activeState.value

    val rsHandler: RedstoneControlHandler?
        get() = behaviour.getRedstoneControlHandler(machineState)

    fun associateHatches(mbCtrl: MultiBlockControllerTileEntity) {
        hatches.forEach {
            it.associate(mbCtrl)
        }
    }

    fun disassociateHatches(mbCtrl: MultiBlockControllerTileEntity) {
        hatches.forEach {
            it.disassociate(mbCtrl)
        }
    }

    private fun collectComponents(): ComponentSet {
        val components = MutableComponentSet()
        components.addAll(baseComponents)
        bufGroups.values.collectComponents(components)
        return components
    }

    fun refreshState(refreshComponents: Boolean) {
        behaviour.notifyState(machineState, orNull(refreshComponents) { collectComponents() })
    }

    override fun onMachineStateChange() {
        activeState.update(behaviour.isActive(machineState))
        mbData.mbCtrl.markDirty()
    }

    @ServerSide
    fun tick() {
        behaviour.tick(machineState, ticker)
    }

    fun handleInteraction(
        blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
        face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean = behaviour.handleInteraction(machineState, blockState, player, hand, face, hitX, hitY, hitZ)

    @ServerSide
    fun handleBlockUpdate(blockState: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        behaviour.handleBlockUpdate(machineState, blockState, fromBlock, fromPos)
    }

    @ServerSide
    fun handleDestruction(blockState: IBlockState) {
        behaviour.handleDestruction(machineState, blockState)
    }

    fun invalidate() {
        weakValidity = WeakValidity.INVALID
    }

    @ClientSide
    fun bindSync(hostId: Int, syncData: PacketBuffer) {
        val syncMgr = syncManager.getClient()
        syncMgr.registerClientSide(hostId)
        syncMgr.readFullState(syncData)
    }

    @ServerSide
    override fun <M> dispatchSync(packetType: PacketType.S2C<M>, message: M) {
        packetType.sendToAllTracking(message, mbData.mbCtrl)
    }

    override fun writeToNbt(dto: NBTTagCompound) {
        behaviour.serializeMachineToNbt(machineState, dto)
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        behaviour.deserializeMachineFromNbt(machineState, dto)
    }

    fun createMachineUiElement(): UiElement? = behaviour.createUiElement(machineState)
}
