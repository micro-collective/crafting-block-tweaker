package st.evening.mc.cbtweaker.hatch

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.buffer.AutoExportingBufferType
import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.buffer.BufferObserver
import st.evening.mc.cbtweaker.common.BlockBehaviour
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerTileEntity
import st.evening.mc.cbtweaker.serconfig.CopiableConfigHost
import st.evening.mc.cbtweaker.util.component.AutoExportHandler
import st.evening.mc.cbtweaker.util.config.putNonEmpty
import st.evening.mc.cbtweaker.util.machine.RefreshState
import st.evening.mc.cbtweaker.util.world.AllFaces
import st.evening.mc.prelude.api.data.ser.ServerSideSerializable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.onObservableUpdate
import st.evening.mc.prelude.api.util.collection.IdentityHashStrategy
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.data.getBoolOrNull
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.CapabilityVisitor
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace
import st.evening.mc.prelude.api.util.world.onServer
import java.util.Random

class HatchState<B>(
    private val hatch: HatchTileEntity,
    val hatchType: HatchType,
    private val hatchTypeData: HatchType.Data<B>,
    val hatchTier: Int
) : BlockBehaviour.Handler, BufferObserver, CopiableConfigHost, ServerSideSerializable {
    companion object {
        private const val SER_BUFFER: String = "buffer"
        private const val SER_EXPORT: String = "export"
    }

    val buffer: B = hatchTypeData.getTier(hatchTier).bufferFactory.createBuffer(hatch.world, hatch.pos, this)

    val exportHandler: HatchAutoExportHandler? =
        (hatchTypeData.bufferType as? AutoExportingBufferType<B, *, *, *>)?.let { exportBufType ->
            exportBufType.getDefaultAutoExportState(buffer)?.let {
                HatchAutoExportHandler(exportBufType, it)
            }
        }

    private val linkedMbControllers: ObjectSet<MultiBlockControllerTileEntity> =
        ObjectOpenCustomHashSet(IdentityHashStrategy())
    private var dirtyState: RefreshState = RefreshState.NONE

    fun attachCapabilities(target: CapabilityVisitor) {
        hatchTypeData.bufferType.attachCapabilities(target, buffer)
    }

    override fun onIngredientsChanged() {
        dirtyState = RefreshState.SOFT_REFRESH
        hatch.markDirty()
    }

    override fun onComponentsChanged() {
        dirtyState = RefreshState.HARD_REFRESH
        hatch.markDirty()
    }

    fun associate(mbCtrl: MultiBlockControllerTileEntity) {
        linkedMbControllers += mbCtrl
    }

    fun disassociate(mbCtrl: MultiBlockControllerTileEntity) {
        linkedMbControllers -= mbCtrl
    }

    fun addToGroup(group: BufferGroup) {
        val pos = hatch.pos
        group.addBuffer(hatchTypeData.bufferType, "${pos.x},${pos.y},${pos.z}", buffer)
    }

    fun tick() {
        if (dirtyState != RefreshState.NONE) {
            val compsDirty = dirtyState == RefreshState.HARD_REFRESH
            val iter = linkedMbControllers.iterator()
            while (iter.hasNext()) {
                val mbCtrl = iter.next()
                if (mbCtrl.isInvalid) {
                    iter.remove()
                } else {
                    mbCtrl.notifyHatchChanged(compsDirty)
                }
            }
            dirtyState = RefreshState.NONE
        }
        hatchTypeData.bufferType.tick(buffer)
        hatch.world.onServer {
            exportHandler?.tick()
        }
    }

    override fun handlePlacement(blockState: IBlockState, placer: EntityLivingBase, stack: ItemStack) {
        hatchTypeData.bufferType.handlePlacement(buffer, blockState, placer, stack)
    }

    override fun handleInteraction(
        blockState: IBlockState, player: EntityPlayer, hand: EnumHand,
        face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean = hatchTypeData.bufferType.handleInteraction(buffer, blockState, player, hand, face, hitX, hitY, hitZ)

    @ServerSide
    override fun handleBlockUpdate(blockState: IBlockState, fromBlock: Block, fromPos: BlockPos) {
        hatchTypeData.bufferType.handleBlockUpdate(buffer, blockState, fromBlock, fromPos)
    }

    @ServerSide
    override fun handleDestruction(blockState: IBlockState) {
        hatchTypeData.bufferType.handleDestruction(buffer, blockState)
    }

    @ClientSide.Strong
    override fun handleDisplayTick(blockState: IBlockState, rand: Random) {
        hatchTypeData.bufferType.handleDisplayTick(buffer, blockState, rand)
    }

    @ServerSide
    override fun writeConfig(dto: NBTTagCompound) {
        dto.runAction {
            putNonEmpty(SER_BUFFER) { hatchTypeData.bufferType.writeBufferConfig(buffer, it) }
            exportHandler?.let {
                SER_EXPORT bool it.autoExporting
            }
        }
    }

    @ServerSide
    override fun readConfig(dto: NBTTagCompound) {
        dto.getCompoundOrNull(SER_BUFFER)?.let {
            hatchTypeData.bufferType.readBufferConfig(buffer, it)
        }
        exportHandler?.let { handler ->
            dto.getBoolOrNull(SER_EXPORT)?.let {
                handler.autoExporting = it
            }
        }
    }

    fun getSyncState(): Piecewise? = hatchTypeData.bufferType.getBufferSyncState(buffer)

    @ServerSide
    override fun writeToNbtServerSide(dto: NBTTagCompound) {
        dto.runAction {
            SER_BUFFER tag NBTTagCompound().also { hatchTypeData.bufferType.serializeBufferToNbt(buffer, it) }
            exportHandler?.let {
                SER_EXPORT bool it.autoExporting
            }
        }
    }

    @ServerSide
    override fun readFromNbtServerSide(dto: NBTTagCompound) {
        hatchTypeData.bufferType.deserializeBufferFromNbt(buffer, dto.getCompoundTag(SER_BUFFER))
        exportHandler?.setStateFromSync(dto.getBoolean(SER_EXPORT))
    }

    fun createUiElement(): UiElement? = hatchTypeData.bufferType.createUiElement(buffer)

    inner class HatchAutoExportHandler(
        exportBufType: AutoExportingBufferType<B, *, *, *>,
        initiallyExporting: Boolean
    ) : AutoExportHandler<B>(exportBufType, buffer, initiallyExporting), Piecewise.Atom {
        private val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()
        private val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

        override fun getFrontSide(): BlockSide = BlockSide.NORTH

        override fun getEnabledFaces(): Set<RelativeFace> = AllFaces

        override fun onAutoExportStateChange() {
            stateObservers.onObservableUpdate()
            hatch.markDirty()
        }

        override fun observeState(observer: Observer.Simple) {
            stateObservers += observer
        }

        override fun observeSync(observer: Observer.Simple) {
            syncObservers += observer
        }

        internal fun setStateFromSync(exporting: Boolean) {
            if (setAutoExportState(exporting)) {
                syncObservers.onObservableUpdate()
            }
        }

        override fun writeToNetwork(buf: PacketBuffer) {
            buf.writeBoolean(autoExporting)
        }

        override fun readFromNetwork(buf: PacketBuffer) {
            setStateFromSync(buf.readBoolean())
        }
    }
}
