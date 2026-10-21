package st.evening.mc.cbtweaker.multiblock

import io.netty.buffer.Unpooled
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.common.BlockBehaviour
import st.evening.mc.cbtweaker.common.LazyTileEntity
import st.evening.mc.cbtweaker.common.MachineTileEntity
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.network.S2CBindMultiBlockAssembly
import st.evening.mc.cbtweaker.serconfig.CopiableConfigHost
import st.evening.mc.cbtweaker.structure.StructureVisualization
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.onServer
import java.util.Random

class MultiBlockControllerTileEntity :
    LazyTileEntity<MultiBlockState<*, *>>(), MachineTileEntity, BlockBehaviour.Handler, CopiableConfigHost, ITickable {

    override fun initData(): MultiBlockState<*, *> {
        val mbType = (world.getBlockState(pos).block as MultiBlockControllerBlock).mbType
        val mbTypeData = mbType.data
        return MultiBlockState(this, mbType, mbTypeData, mbTypeData.structureMatcher)
    }

    val mbType: MultiBlockType
        get() = state.mbType

    val assembled: Boolean
        get() = state.assembled

    val assemblyStateClock: Int
        get() = state.assemblyStateClock

    override val isActive: Boolean
        get() = state.isActive

    override val rsHandler: RedstoneControlHandler?
        get() = state.rsHandler

    override fun invalidate() {
        super.invalidate()
        state.onInvalidated()
    }

    fun notifyHatchChanged(compsDirty: Boolean) {
        state.notifyHatchChanged(compsDirty)
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
        state.tick()
    }

    override fun handlePlacement(blockState: IBlockState, placer: EntityLivingBase, stack: ItemStack) {
        state.handlePlacement(blockState, placer, stack)
    }

    override fun handleInteraction(
        blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
        face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean = state.handleInteraction(blockState, player, hand, face, hitX, hitY, hitZ)

    @ServerSide
    override fun handleBlockUpdate(blockState: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        state.handleBlockUpdate(blockState, fromBlock, fromPos)
    }

    @ServerSide
    override fun handleDestruction(blockState: IBlockState) {
        state.handleDestruction(blockState)
    }

    @ClientSide.Strong
    override fun handleDisplayTick(blockState: IBlockState, rand: Random) {
        state.handleDisplayTick(blockState, rand)
    }

    @ServerSide
    override fun writeConfig(dto: NBTTagCompound) {
        state.writeConfig(dto)
    }

    @ServerSide
    override fun readConfig(dto: NBTTagCompound) {
        state.readConfig(dto)
    }

    @ClientSide
    fun bindSync(hostId: Int, syncData: PacketBuffer) {
        state.bindSync(hostId, syncData)
    }

    fun createMachineUiElement(): UiElement? = state.createMachineUiElement()

    fun getStructureVisualization(): StructureVisualization = state.getStructureVisualization()
}
