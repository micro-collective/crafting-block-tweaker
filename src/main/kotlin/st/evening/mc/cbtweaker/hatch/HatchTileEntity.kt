package st.evening.mc.cbtweaker.hatch

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
import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.common.BlockBehaviour
import st.evening.mc.cbtweaker.common.LazyTileEntity
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerTileEntity
import st.evening.mc.cbtweaker.serconfig.CopiableConfigHost
import st.evening.mc.cbtweaker.util.sync.TileEntitySyncProxy
import st.evening.mc.prelude.api.util.collection.CapabilityMap
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import java.util.Random

class HatchTileEntity : LazyTileEntity<HatchData<*>>(), BlockBehaviour.Handler, CopiableConfigHost, ITickable {
    private val capabilities: CapabilityMap by lazy {
        CapabilityMap().also { data.attachCapabilities(it) }
    }

    override fun initData(): HatchData<*> {
        val state = world.getBlockState(getPos())
        val block = state.getBlock() as HatchBlock
        val hatchType = block.hatchType
        val data = HatchData(this, hatchType, hatchType.data, state.getValue(block.tierProperty))
        data.getSyncState()?.let {
            initSync(TileEntitySyncProxy(this, it))
        }
        return data
    }

    val hatchType: HatchType
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

    override fun handlePlacement(blockState: IBlockState, placer: EntityLivingBase, stack: ItemStack) {
        data.handlePlacement(blockState, placer, stack)
    }

    override fun handleInteraction(
        blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
        face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean = data.handleInteraction(blockState, player, hand, face, hitX, hitY, hitZ)

    @ServerSide
    override fun handleBlockUpdate(blockState: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        data.handleBlockUpdate(blockState, fromBlock, fromPos)
    }

    @ServerSide
    override fun handleDestruction(blockState: IBlockState) {
        data.handleDestruction(blockState)
    }

    @ClientSide.Strong
    override fun handleDisplayTick(blockState: IBlockState, rand: Random) {
        data.handleDisplayTick(blockState, rand)
    }

    @ServerSide
    override fun writeConfig(dto: NBTTagCompound) {
        data.writeConfig(dto)
    }

    @ServerSide
    override fun readConfig(dto: NBTTagCompound) {
        data.readConfig(dto)
    }

    fun createUiElement(): UiElement? = data.createUiElement()
}
