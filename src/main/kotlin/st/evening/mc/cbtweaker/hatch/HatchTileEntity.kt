package st.evening.mc.cbtweaker.hatch

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability
import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.common.LazyTileEntity
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerTileEntity
import st.evening.mc.cbtweaker.util.sync.TileEntitySyncProxy
import st.evening.mc.prelude.api.util.collection.CapabilityMap
import st.evening.mc.prelude.api.util.game.ServerSide

class HatchTileEntity : LazyTileEntity<HatchData<*>>(), ITickable {
    private val capabilities: CapabilityMap by lazy {
        CapabilityMap().also { data.attachCapabilities(it) }
    }

    override fun initData(): HatchData<*> {
        val state = world.getBlockState(getPos())
        val block = state.getBlock() as HatchBlock
        val data = HatchData(this, block.hatchType, state.getValue(block.tierProperty))
        data.getSyncState()?.let {
            initSync(TileEntitySyncProxy(this, it))
        }
        return data
    }

    val hatchType: HatchType<*>
        get() = data.hatchType

    val tier: Int
        get() = data.hatchTier

    val exportHandler: HatchData<*>.HatchAutoExportHandler?
        get() = data.exportHandler

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean = capability in capabilities

    override fun <T : Any> getCapability(capability: Capability<T>, facing: EnumFacing?): T? = capabilities[capability]

    fun associate(mbCtrl: MultiBlockControllerTileEntity) {
        data.associate(mbCtrl)
    }

    fun disassociate(mbCtrl: MultiBlockControllerTileEntity) {
        data.disassociate(mbCtrl)
    }

    fun addToGroup(group: BufferGroup) {
        data.addToGroup(group)
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

    fun createUiElement(): UiElement? = data.createUiElement()
}
