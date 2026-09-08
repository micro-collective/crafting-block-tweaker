package st.evening.mc.cbtweaker.singleblock

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability
import st.evening.mc.cbtweaker.common.LazyTileEntity
import st.evening.mc.cbtweaker.common.MachineTileEntity
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.util.TileEntitySyncProxy
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.cbtweaker.util.component.SidedBufferHandler
import st.evening.mc.cbtweaker.util.component.UiElementTable
import st.evening.mc.prelude.api.util.game.ServerSide

class SingleBlockMachineTileEntity : LazyTileEntity<SingleBlockData<*>>(), MachineTileEntity, ITickable {
    override fun initData(): SingleBlockData<*> {
        val data = SingleBlockData(this, (world.getBlockState(pos).block as SingleBlockMachineBlock).sbType)
        initSync(TileEntitySyncProxy(this, data.syncState))
        return data
    }

    val sbType: SingleBlockType<*>
        get() = data.sbType

    val bufHandler: SidedBufferHandler
        get() = data.bufHandler

    override val isActive: Boolean
        get() = data.isActive

    override val rsHandler: RedstoneControlHandler?
        get() = data.rsHandler

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        bufHandler.hasCapability(capability, facing)

    override fun <T : Any> getCapability(capability: Capability<T>, facing: EnumFacing?): T? =
        bufHandler.getCapability(capability, facing)

    override fun update() {
        data.tick()
    }

    fun handleInteraction(
        player: EntityPlayer, hand: EnumHand, face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean = data.handleInteraction(world.getBlockState(pos), player, hand, face, hitX, hitY, hitZ)

    @ServerSide
    fun handleBlockUpdate(blockState: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        data.handleBlockUpdate(blockState, fromBlock, fromPos)
    }

    @ServerSide
    fun handleDestruction(blockState: IBlockState) {
        data.handleDestruction(blockState)
    }

    fun createBufferUiElements(): UiElementTable = data.createBufferUiElements()

    fun createMachineUiElement(): UiElement? = data.createMachineUiElement()
}
