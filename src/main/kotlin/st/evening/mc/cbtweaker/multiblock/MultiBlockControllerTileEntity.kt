package st.evening.mc.cbtweaker.multiblock

import io.netty.buffer.Unpooled
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.common.LazyTileEntity
import st.evening.mc.cbtweaker.common.MachineTileEntity
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.network.S2CBindMultiBlockAssembly
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.onServer

class MultiBlockControllerTileEntity : LazyTileEntity<MultiBlockData<*>>(), MachineTileEntity, ITickable {
    override fun initData(): MultiBlockData<*> =
        MultiBlockData(this, (world.getBlockState(pos).block as MultiBlockControllerBlock).mbType)

    val mbType: MultiBlockType<*>
        get() = data.mbType

    val assembled: Boolean
        get() = data.assembled

    val assemblyStateClock: Int
        get() = data.assemblyStateClock

    override val isActive: Boolean
        get() = data.isActive

    override val rsHandler: RedstoneControlHandler?
        get() = data.rsHandler

    override fun invalidate() {
        super.invalidate()
        data.onInvalidated()
    }

    fun notifyHatchChanged(compsDirty: Boolean) {
        data.notifyHatchChanged(compsDirty)
    }

    fun onAssemblyChanged(assembly: MultiBlockAssembly<*>?) {
        if (assembly != null) {
            val syncProxy = assembly.syncProxy
            if (syncProxy != null) {
                initSync(syncProxy)
                world.onServer {
                    val syncMgr = syncProxy.syncManager.getServer()
                    syncProxy.dispatchSync(
                        CbTweaker.defns.s2cBindMultiBlockAssembly,
                        S2CBindMultiBlockAssembly(
                            pos,
                            syncMgr.hostId,
                            PacketBuffer(Unpooled.buffer()).also { syncMgr.writeFullState(it) }
                        )
                    )
                }
                return
            }
        }
        initSync(null)
    }

    override fun update() {
        data.tick()
    }

    fun handleInteraction(
        state: IBlockState, player: EntityPlayer, hand: EnumHand,
        face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean = data.handleInteraction(state, player, hand, face, hitX, hitY, hitZ)

    @ServerSide
    fun handleBlockUpdate(state: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        data.handleBlockUpdate(state, fromBlock, fromPos)
    }

    @ServerSide
    fun handleDestruction(state: IBlockState) {
        data.handleDestruction(state)
    }

    @ClientSide
    fun bindSync(hostId: Int, syncData: PacketBuffer) {
        data.bindSync(hostId, syncData)
    }

    fun createMachineUiElement(): UiElement? = data.createMachineUiElement()
}
