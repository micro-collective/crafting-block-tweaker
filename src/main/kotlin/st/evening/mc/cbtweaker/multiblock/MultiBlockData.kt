package st.evening.mc.cbtweaker.multiblock

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.common.BufferedSyncHolder
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.serconfig.CopiableConfigHost
import st.evening.mc.cbtweaker.structure.impl.SimpleStructureMatcher
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.cbtweaker.util.machine.RefreshState
import st.evening.mc.cbtweaker.world.RoiHost
import st.evening.mc.cbtweaker.world.RoiTicket
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.data.ser.ServerSideSerializable
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.onClient
import st.evening.mc.prelude.api.util.world.onServer

class MultiBlockData<S>(
    val mbCtrl: MultiBlockControllerTileEntity,
    val mbType: MultiBlockType<S>
) : RoiHost, CopiableConfigHost, BufferedSyncHolder, ServerSideSerializable {
    private var mbRoiTicket: RoiTicket? = null
    private var assembly: MultiBlockAssembly<S>? = null
    @ServerSide
    private var bufferedAssemblyDeser: NBTTagCompound? = null
    @ClientSide
    private var bufferedAssemblyBind: BindData? = null
    @ServerSide
    private var cachedAssemblyConfig: NBTTagCompound? = null
    var assemblyStateClock: Int = 0
        private set
    private var structDirty: Boolean = false

    private var refreshState: RefreshState = RefreshState.NONE
    private var wasActiveLastTick: Boolean = false

    val assembled: Boolean
        get() = assembly != null

    val isActive: Boolean
        get() = assembly?.isActive == true

    val rsHandler: RedstoneControlHandler?
        get() = assembly?.rsHandler

    override val isValidRoiHost: Boolean
        get() = !mbCtrl.isInvalid

    private fun dropRoi(invalidateAssembly: Boolean) {
        mbRoiTicket?.let { ticket ->
            ticket.invalidateRoi()
            if (invalidateAssembly) {
                assembly?.let {
                    mbCtrl.world.onServer {
                        val configDto = NBTTagCompound()
                        it.writeConfig(configDto)
                        if (!configDto.isEmpty) {
                            cachedAssemblyConfig = configDto
                        }
                        it.handleDestruction(mbCtrl.world.getBlockState(mbCtrl.pos))
                    }
                    it.disassociateHatches(mbCtrl)
                    it.invalidate()
                    assembly = null
                    assemblyStateClock++
                    mbCtrl.onAssemblyChanged(null)
                }
                structDirty = true
            }
            mbRoiTicket = null
        }
    }

    private fun acquireBaseRoi() {
        val world = mbCtrl.world
        val pos = mbCtrl.pos
        val region = mbType.structureMatcher.getRegion(
            world, pos, world.getBlockState(pos).getValue(BlockSidedIfc.PROP_FACING)
        )
        mbRoiTicket = CbTweaker.defns.roiTracker.registerRoi(this, world, region)
    }

    fun onInvalidated() {
        dropRoi(true)
    }

    override fun onRegionChanged(ticket: RoiTicket, pos: BlockPos) {
        if (pos == mbCtrl.pos) {
            dropRoi(true)
        }
        structDirty = true
    }

    fun notifyHatchChanged(compsDirty: Boolean) {
        refreshState = if (compsDirty) RefreshState.HARD_REFRESH else RefreshState.SOFT_REFRESH
    }

    fun tick() {
        val world = mbCtrl.world
        val pos = mbCtrl.pos
        if (mbRoiTicket == null) {
            acquireBaseRoi()
            structDirty = true
        }

        if (structDirty) {
            val matcher = mbType.structureMatcher
            val match = matcher.findMatch(world, pos, world.getBlockState(pos).getValue(BlockSidedIfc.PROP_FACING))
            if (match != null) {
                assembly?.invalidate()
                val assembly = MultiBlockAssembly.fromStructure(this, match, assembly)
                this.assembly = assembly
                assemblyStateClock++
                mbCtrl.world.onClient {
                    bufferedAssemblyBind?.let {
                        assembly.bindSync(it.hostId, it.data)
                        it.data.release()
                        bufferedAssemblyBind = null
                    }
                }
                mbCtrl.onAssemblyChanged(assembly)
                // this only really works for the simple structure matcher, because other structures can potentially
                // grow without needing an original block removed, e.g. the linear matcher
                // in the future, could allow the matcher itself to define dynamic ROIs, but this works for now
                if (matcher is SimpleStructureMatcher) {
                    dropRoi(false)
                    mbRoiTicket = CbTweaker.defns.roiTracker.registerRoi(
                        this, world, assembly.structureBlocks.iterator()
                    )
                }
                assembly.associateHatches(mbCtrl)
                mbCtrl.world.onServer {
                    bufferedAssemblyDeser?.let {
                        assembly.readFromNbtServerSide(it)
                        bufferedAssemblyDeser = null
                    }
                    cachedAssemblyConfig?.let {
                        assembly.readConfig(it)
                        cachedAssemblyConfig = null
                    }
                }
                // recheck recipe immediately once assembled
                // don't need to hard-refresh because the executor state will be fresh anyways
                refreshState = RefreshState.SOFT_REFRESH
            } else {
                dropRoi(true)
            }
            structDirty = false
        }

        val active: Boolean
        val assembly = this.assembly
        if (assembly != null) {
            if (refreshState != RefreshState.NONE) {
                assembly.refreshState(refreshState == RefreshState.HARD_REFRESH)
                refreshState = RefreshState.NONE
            }
            world.onServer {
                assembly.tick()
            }
            active = assembly.isActive
        } else {
            active = false
        }

        if (wasActiveLastTick != active) {
            wasActiveLastTick = active
            world.markBlockRangeForRenderUpdate(pos, pos)
        }
    }

    fun handleInteraction(
        state: IBlockState, player: EntityPlayer, hand: EnumHand,
        face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean = assembly?.handleInteraction(state, player, hand, face, hitX, hitY, hitZ) ?: false

    @ServerSide
    fun handleBlockUpdate(state: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        assembly?.handleBlockUpdate(state, fromBlock, fromPos)
    }

    @ServerSide
    fun handleDestruction(state: IBlockState) {
        assembly?.handleDestruction(state)
    }

    @ServerSide
    override fun writeConfig(dto: NBTTagCompound) {
        assembly?.writeConfig(dto)
    }

    @ServerSide
    override fun readConfig(dto: NBTTagCompound) {
        assembly?.readConfig(dto)
    }

    @ClientSide
    fun bindSync(hostId: Int, syncData: PacketBuffer) {
        val assembly = this.assembly
        if (assembly != null) {
            assembly.bindSync(hostId, syncData)
        } else {
            bufferSyncData(hostId, syncData)
        }
    }

    @ClientSide
    override fun bufferSyncData(hostId: Int, syncData: PacketBuffer) {
        bufferedAssemblyBind?.data?.release()
        syncData.retain()
        bufferedAssemblyBind = BindData(hostId, syncData)
    }

    @ServerSide
    override fun writeToNbtServerSide(dto: NBTTagCompound) {
        assembly?.writeToNbtServerSide(dto)
    }

    @ServerSide
    override fun readFromNbtServerSide(dto: NBTTagCompound) {
        if (!dto.isEmpty) {
            val assembly = this.assembly
            if (assembly != null) {
                assembly.readFromNbtServerSide(dto)
            } else {
                bufferedAssemblyDeser = dto
            }
        }
    }

    fun createMachineUiElement(): UiElement? = assembly?.createMachineUiElement()

    private class BindData(val hostId: Int, val data: PacketBuffer)
}
