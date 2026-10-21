package st.evening.mc.cbtweaker.singleblock

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability
import st.evening.mc.cbtweaker.common.BlockBehaviour
import st.evening.mc.cbtweaker.common.LazyTileEntity
import st.evening.mc.cbtweaker.common.MachineTileEntity
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.serconfig.CopiableConfigHost
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.cbtweaker.util.component.SidedBufferHandler
import st.evening.mc.cbtweaker.util.sync.TileEntitySyncProxy
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import java.util.Random

class SingleBlockMachineTileEntity :
    LazyTileEntity<SingleBlockState<*>>(), MachineTileEntity, BlockBehaviour.Handler, CopiableConfigHost, ITickable {

    override fun initData(): SingleBlockState<*> {
        val sbType = (world.getBlockState(pos).block as SingleBlockMachineBlock).sbType
        val data = SingleBlockState(this, sbType, sbType.data)
        data.getSyncState()?.let {
            initSync(TileEntitySyncProxy(this, it))
        }
        return data
    }

    val sbType: SingleBlockType
        get() = state.sbType

    val bufHandler: SidedBufferHandler
        get() = state.bufHandler

    override val isActive: Boolean
        get() = state.isActive

    override val rsHandler: RedstoneControlHandler?
        get() = state.rsHandler

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        bufHandler.hasCapability(capability, facing)

    override fun <T : Any> getCapability(capability: Capability<T>, facing: EnumFacing?): T? =
        bufHandler.getCapability(capability, facing)

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

    fun createMachineUiElement(): UiElement? = state.createMachineUiElement()
}
