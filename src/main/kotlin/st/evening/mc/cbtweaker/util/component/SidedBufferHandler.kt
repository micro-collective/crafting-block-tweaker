package st.evening.mc.cbtweaker.util.component

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMap
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.Constants
import st.evening.mc.cbtweaker.buffer.AutoExportingBufferType
import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.buffer.BufferGroups
import st.evening.mc.cbtweaker.buffer.BufferType
import st.evening.mc.cbtweaker.buffer.SidedBufferType
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.util.capability.CapabilityMerger
import st.evening.mc.cbtweaker.util.machine.MutableComponentSet
import st.evening.mc.cbtweaker.util.world.AllFaces
import st.evening.mc.prelude.api.PreludeInternal
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.onObservableUpdate
import st.evening.mc.prelude.api.util.collection.CapabilityMap
import st.evening.mc.prelude.api.util.collection.CapabilityMultimap
import st.evening.mc.prelude.api.util.collection.WeakValidityMap
import st.evening.mc.prelude.api.util.data.BitVector
import st.evening.mc.prelude.api.util.data.forEachString
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.RelativeFace
import kotlin.experimental.and

interface BufferConfig<B> : NbtCompoundSerializable {
    val bufType: BufferType<B, *, *, *>
    val buffer: B

    val exportHandler: AutoExportHandler<B>?

    val configSyncState: Piecewise?
}

fun <B> BufferConfig<B>.collectComponents(components: MutableComponentSet) {
    bufType.collectComponents(components, buffer)
}

fun <B> BufferConfig<B>.handleInteraction(
    state: IBlockState, player: EntityPlayer, hand: EnumHand, face: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
): Boolean = bufType.handleInteraction(buffer, state, player, hand, face, hitX, hitY, hitZ)

@ServerSide
fun <B> BufferConfig<B>.handleBlockUpdate(state: IBlockState, fromBlock: Block, fromPos: BlockPos) {
    bufType.handleBlockUpdate(buffer, state, fromBlock, fromPos)
}

@ServerSide
fun <B> BufferConfig<B>.handleDestruction(state: IBlockState) {
    bufType.handleDestruction(buffer, state)
}

fun <B> BufferConfig<B>.getBufferSyncState(): Piecewise? = bufType.getBufferSyncState(buffer)

fun <B> BufferConfig<B>.createUiElement(): UiElement? = bufType.createUiElement(buffer)

class SidedBufferConfig<B>(
    override val bufType: BufferType<B, *, *, *>,
    override val buffer: B,
    private val getFront: () -> BlockSide,
) : BufferConfig<B> {
    companion object {
        private const val SER_BUFFER: String = "buffer"
        private const val SER_SIDES: String = "sides"
        private const val SER_EXPORT: String = "export"
    }

    private val enabledFaces: BitVector.EnumSet<RelativeFace> = BitVector.EnumSet()
    private val capabilities: CapabilityMap = CapabilityMap()

    private val _exportHandler: MachineSideAutoExportHandler? =
        (bufType as? AutoExportingBufferType<B, *, *, *>)?.let { exportBufType ->
            exportBufType.getDefaultAutoExportState(buffer)?.let {
                MachineSideAutoExportHandler(exportBufType, it)
            }
        }
    override val exportHandler: AutoExportHandler<B>?
        get() = _exportHandler

    private val _configSyncState: ConfigSyncState = ConfigSyncState()
    override val configSyncState: Piecewise
        get() = _configSyncState

    internal fun initSidedCapabilities() {
        bufType.attachCapabilities(capabilities, buffer)
    }

    fun hasCapability(capability: Capability<*>): Boolean = capability in capabilities

    fun <T : Any> getCapability(capability: Capability<T>): T? = capabilities[capability]

    fun isEnabled(face: RelativeFace): Boolean = face in enabledFaces

    fun setEnabled(face: RelativeFace, enabled: Boolean) {
        if (enabled) {
            if (!enabledFaces.add(face)) return
        } else {
            if (!enabledFaces.remove(face)) return
        }
        _configSyncState.stateObservers.onObservableUpdate()
    }

    fun setAllEnabled(enabled: Boolean) {
        var changed = false
        if (enabled) {
            RelativeFace.entries.forEach {
                if (enabledFaces.add(it)) {
                    changed = true
                }
            }
        } else {
            RelativeFace.entries.forEach {
                if (enabledFaces.remove(it)) {
                    changed = true
                }
            }
        }
        if (changed) {
            _configSyncState.stateObservers.onObservableUpdate()
        }
    }

    fun toggle(face: RelativeFace) {
        enabledFaces[face] = face !in enabledFaces
        _configSyncState.stateObservers.onObservableUpdate()
    }

    @ServerSide
    internal fun tick() {
        bufType.tick(buffer)
        _exportHandler?.tick()
    }

    override fun writeToNbt(dto: NBTTagCompound) {
        dto.runAction {
            SER_BUFFER tag NBTTagCompound().also { bufType.serializeBufferToNbt(buffer, it) }
            SER_SIDES stringList enabledFaces.map { it.name }
            _exportHandler?.let {
                SER_EXPORT bool it.autoExporting
            }
        }
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        enabledFaces.clear()
        bufType.deserializeBufferFromNbt(buffer, dto.getCompoundTag(SER_BUFFER))
        dto.getTagList(SER_SIDES, Constants.NBT.TAG_STRING).forEachString {
            try {
                enabledFaces += enumValueOf<RelativeFace>(it)
            } catch (_: IllegalArgumentException) {
            }
        }
        _exportHandler?.setStateFromSync(dto.getBoolean(SER_EXPORT))
        _configSyncState.syncObservers.onObservableUpdate()
    }

    private inner class MachineSideAutoExportHandler(
        exportBufType: AutoExportingBufferType<B, *, *, *>,
        initiallyExporting: Boolean
    ) : AutoExportHandler<B>(exportBufType, buffer, initiallyExporting) {
        override fun getFrontSide(): BlockSide = getFront()

        override fun getEnabledFaces(): Set<RelativeFace> = enabledFaces

        override fun onAutoExportStateChange() {
            _configSyncState.stateObservers.onObservableUpdate()
        }

        fun setStateFromSync(exporting: Boolean) {
            setAutoExportState(exporting)
        }
    }

    private inner class ConfigSyncState : Piecewise.Atom {
        val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()
        val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

        override fun observeState(observer: Observer.Simple) {
            stateObservers += observer
        }

        override fun observeSync(observer: Observer.Simple) {
            syncObservers += observer
        }

        @OptIn(PreludeInternal::class)
        override fun writeToNetwork(buf: PacketBuffer) {
            val dirMask = enabledFaces.backingBitVector.backingByteArray[0].toInt()
            buf.writeByte(if (_exportHandler?.autoExporting == true) dirMask or 0x80 else dirMask)
        }

        override fun readFromNetwork(buf: PacketBuffer) {
            _exportHandler?.setStateFromSync(buf.getByte(buf.readerIndex()) and 0x80.toByte() != 0.toByte())
            enabledFaces.readFromNetwork(buf)
            syncObservers.onObservableUpdate()
        }
    }
}

private typealias ConfigBufTable<T> = Object2ObjectSortedMap<String, T>
private typealias ConfigSubTable<T> = Object2ObjectSortedMap<BufferType<*, *, *, *>, ConfigBufTable<T>>
private typealias ConfigTable<T> = Object2ObjectSortedMap<String, ConfigSubTable<T>>

inline fun <T> ConfigTable<T>.forEachConfig(action: (T) -> Unit) {
    values.forEach { subTable ->
        subTable.values.forEach { configs ->
            configs.values.forEach(action)
        }
    }
}

private fun <T : NbtCompoundSerializable> ConfigTable<T>.writeConfigsToNbt(dto: NBTTagCompound) {
    dto.runAction {
        forEach { (bufGroupId, subTable) ->
            bufGroupId compound {
                subTable.forEach { (bufType, configs) ->
                    bufType.id.toString() compound {
                        configs.forEach { (name, config) ->
                            name tag config.writeToNbt()
                        }
                    }
                }
            }
        }
    }
}

private fun <T : NbtCompoundSerializable> ConfigTable<T>.readConfigsFromNbt(dto: NBTTagCompound) {
    forEach { (bufGroupId, subTable) ->
        val subTableDto = dto.getCompoundTag(bufGroupId)
        subTable.forEach { (bufType, configs) ->
            val configsDto = subTableDto.getCompoundTag(bufType.id.toString())
            configs.forEach { (name, config) ->
                config.readFromNbt(configsDto.getCompoundTag(name))
            }
        }
    }
}

typealias UiElementTable = Map<String, Map<BufferType<*, *, *, *>, Map<String, UiElement>>>

class SidedBufferHandler(
    val getFront: () -> BlockSide,
    bufGroups: BufferGroups
) : ICapabilityProvider, NbtCompoundSerializable {
    private val sideConfigTable: ConfigTable<SidedBufferConfig<*>>
    private val unsidedConfigTable: ConfigTable<UnsidedConfig<*>>
    private val unsidedCapabilities: CapabilityMultimap = CapabilityMultimap()

    val bufferSyncState: List<Piecewise>

    init {
        // insertion-ordered maps do NOT inherently maintain the correct order; we MUST replicate the order in bufGroups
        val sideConfigTable = Object2ObjectLinkedOpenHashMap<String, ConfigSubTable<SidedBufferConfig<*>>>()
        val unsidedConfigTable = Object2ObjectLinkedOpenHashMap<String, ConfigSubTable<UnsidedConfig<*>>>()
        val visitor = SideConfigVisitor()
        bufGroups.forEach { (bufGroupId, bufGroup) ->
            visitor.sideConfigSubTable = Object2ObjectLinkedOpenHashMap()
            visitor.unsidedConfigSubTable = Object2ObjectLinkedOpenHashMap()
            bufGroup.forEach(visitor)
            if (visitor.sideConfigSubTable.isNotEmpty()) {
                sideConfigTable[bufGroupId] = visitor.sideConfigSubTable
            }
            if (visitor.unsidedConfigSubTable.isNotEmpty()) {
                unsidedConfigTable[bufGroupId] = visitor.unsidedConfigSubTable
            }
        }
        this.sideConfigTable = sideConfigTable
        this.unsidedConfigTable = unsidedConfigTable
        this.bufferSyncState = buildList {
            sideConfigTable.forEachConfig { config ->
                config.getBufferSyncState()?.let { add(it) }
            }
            unsidedConfigTable.forEachConfig { config ->
                config.getBufferSyncState()?.let { add(it) }
            }
        }
    }

    @PreludeInternal
    fun getSideConfigTable(): ConfigTable<SidedBufferConfig<*>> = sideConfigTable

    @PreludeInternal
    fun getUnsidedConfigTable(): ConfigTable<UnsidedConfig<*>> = unsidedConfigTable

    @OptIn(PreludeInternal::class)
    inline fun forEachConfig(action: (BufferConfig<*>) -> Unit) {
        getSideConfigTable().forEachConfig(action)
        getUnsidedConfigTable().forEachConfig(action)
    }

    @OptIn(PreludeInternal::class)
    inline fun forEachEnabledConfig(face: RelativeFace, action: (BufferConfig<*>) -> Unit) {
        getSideConfigTable().forEachConfig {
            if (it.isEnabled(face)) {
                action(it)
            }
        }
        getUnsidedConfigTable().forEachConfig(action)
    }

    inline fun forEachEnabledConfig(face: EnumFacing, action: (BufferConfig<*>) -> Unit) {
        forEachEnabledConfig(RelativeFace.fromFace(getFront(), face), action)
    }

    @Suppress("UNCHECKED_CAST")
    fun <B> getConfig(bufGroupId: String, bufType: BufferType<B, *, *, *>, name: String): BufferConfig<B>? =
        (sideConfigTable[bufGroupId]?.get(bufType)?.get(name)
            ?: unsidedConfigTable[bufGroupId]?.get(bufType)?.get(name)) as? BufferConfig<B>

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        if (capability in unsidedCapabilities) return true
        if (facing != null) {
            val relFace = RelativeFace.fromFace(getFront(), facing)
            sideConfigTable.forEachConfig {
                if (it.isEnabled(relFace) && it.hasCapability(capability)) return true
            }
        }
        return false
    }

    override fun <T : Any> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        val instances = mutableListOf<T>()
        unsidedCapabilities[capability]?.let { instances.addAll(it) }
        if (facing != null) {
            val relFace = RelativeFace.fromFace(getFront(), facing)
            sideConfigTable.forEachConfig { config ->
                if (config.isEnabled(relFace)) {
                    config.getCapability(capability)?.let {
                        instances += it
                    }
                }
            }
        }
        return when (instances.size) {
            0 -> null
            1 -> instances[0]
            else -> CapabilityMerger.merge(capability, instances)
        }
    }

    @ServerSide
    fun tick() {
        sideConfigTable.forEachConfig { it.tick() }
        unsidedConfigTable.forEachConfig { it.tick() }
    }

    override fun writeToNbt(dto: NBTTagCompound) {
        // sided/unsided configs partition the original bufGroups, so sharing the same table shouldn't be a problem
        sideConfigTable.writeConfigsToNbt(dto)
        unsidedConfigTable.writeConfigsToNbt(dto)
    }

    override fun readFromNbt(dto: NBTTagCompound) {
        sideConfigTable.readConfigsFromNbt(dto)
        unsidedConfigTable.readConfigsFromNbt(dto)
    }

    fun createBufferUiElements(): UiElementTable {
        val uiElemTable = mutableMapOf<String, MutableMap<BufferType<*, *, *, *>, MutableMap<String, UiElement>>>()
        sideConfigTable.forEach { (bufGroupId, subTable) ->
            val uiElemSubTable = mutableMapOf<BufferType<*, *, *, *>, MutableMap<String, UiElement>>()
            subTable.forEach { (bufType, configs) ->
                val uiElems = mutableMapOf<String, UiElement>()
                configs.forEach { (name, config) ->
                    config.createUiElement()?.let {
                        uiElems[name] = it
                    }
                }
                if (uiElems.isNotEmpty()) {
                    uiElemSubTable[bufType] = uiElems
                }
            }
            if (uiElemSubTable.isNotEmpty()) {
                uiElemTable[bufGroupId] = uiElemSubTable
            }
        }
        unsidedConfigTable.forEach { (bufGroupId, subTable) ->
            val uiElemSubTable = uiElemTable[bufGroupId] ?: mutableMapOf()
            subTable.forEach { (bufType, configs) ->
                val uiElems = uiElemSubTable[bufType] ?: mutableMapOf()
                configs.forEach { (name, config) ->
                    config.createUiElement()?.let {
                        uiElems[name] = it
                    }
                }
                if (uiElems.isNotEmpty()) {
                    uiElemSubTable[bufType] = uiElems
                }
            }
            if (uiElemSubTable.isNotEmpty()) {
                uiElemTable[bufGroupId] = uiElemSubTable
            }
        }
        return uiElemTable
    }

    private inner class SideConfigVisitor : BufferGroup.Visitor {
        lateinit var sideConfigSubTable: ConfigSubTable<SidedBufferConfig<*>>
        lateinit var unsidedConfigSubTable: ConfigSubTable<UnsidedConfig<*>>

        override fun <B, A> visit(bufType: BufferType<B, A, *, *>, buffers: Object2ObjectSortedMap<String, B>) {
            val sideConfigs = Object2ObjectLinkedOpenHashMap<String, SidedBufferConfig<*>>()
            val unsidedConfigs = Object2ObjectLinkedOpenHashMap<String, UnsidedConfig<*>>()
            buffers.forEach { (name, buffer) ->
                if (bufType is SidedBufferType<B, A, *, *>) {
                    val sideConfig = SidedBufferConfig(bufType, buffer, getFront)
                    sideConfigs[name] = sideConfig
                    bufType.configureDefaultSides(sideConfig)
                    if (bufType.isCapabilitySided(buffer)) {
                        sideConfig.initSidedCapabilities()
                    } else {
                        bufType.attachCapabilities(unsidedCapabilities, buffer)
                    }
                } else {
                    unsidedConfigs[name] = UnsidedConfig(bufType, buffer)
                    bufType.attachCapabilities(unsidedCapabilities, buffer)
                }
            }
            if (sideConfigs.isNotEmpty()) {
                sideConfigSubTable[bufType] = sideConfigs
            }
            if (unsidedConfigs.isNotEmpty()) {
                unsidedConfigSubTable[bufType] = unsidedConfigs
            }
        }
    }

    class UnsidedConfig<B>(override val bufType: BufferType<B, *, *, *>, override val buffer: B) : BufferConfig<B> {
        companion object {
            private const val SER_BUFFER: String = "buffer"
            private const val SER_EXPORT: String = "export"
        }

        private val _exportHandler: UnsidedAutoExportHandler? =
            (bufType as? AutoExportingBufferType<B, *, *, *>)?.let { exportBufType ->
                exportBufType.getDefaultAutoExportState(buffer)?.let {
                    UnsidedAutoExportHandler(exportBufType, it)
                }
            }
        override val exportHandler: AutoExportHandler<B>?
            get() = _exportHandler

        override val configSyncState: Piecewise?
            get() = _exportHandler

        @ServerSide
        internal fun tick() {
            bufType.tick(buffer)
            _exportHandler?.tick()
        }

        override fun writeToNbt(dto: NBTTagCompound) {
            dto.runAction {
                SER_BUFFER tag NBTTagCompound().also { bufType.serializeBufferToNbt(buffer, it) }
                _exportHandler?.let {
                    SER_EXPORT bool it.autoExporting
                }
            }
        }

        override fun readFromNbt(dto: NBTTagCompound) {
            bufType.deserializeBufferFromNbt(buffer, dto.getCompoundTag(SER_BUFFER))
            _exportHandler?.setStateFromSync(dto.getBoolean(SER_EXPORT))
        }

        private inner class UnsidedAutoExportHandler(
            exportBufType: AutoExportingBufferType<B, *, *, *>,
            initiallyExporting: Boolean
        ) : AutoExportHandler<B>(exportBufType, buffer, initiallyExporting), Piecewise.Atom {
            private val stateObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()
            private val syncObservers: MutableSet<Observer.Simple> = WeakValidityMap.newSet()

            override fun getFrontSide(): BlockSide = BlockSide.NORTH

            override fun getEnabledFaces(): Set<RelativeFace> = AllFaces

            override fun onAutoExportStateChange() {
                stateObservers.onObservableUpdate()
            }

            override fun observeState(observer: Observer.Simple) {
                stateObservers += observer
            }

            override fun observeSync(observer: Observer.Simple) {
                syncObservers += observer
            }

            fun setStateFromSync(exporting: Boolean) {
                if (setAutoExportState(exporting)) {
                    syncObservers.onObservableUpdate()
                }
            }

            override fun writeToNetwork(buf: PacketBuffer) {
                _exportHandler?.let {
                    buf.writeBoolean(it.autoExporting)
                }
            }

            override fun readFromNetwork(buf: PacketBuffer) {
                _exportHandler?.setStateFromSync(buf.readBoolean())
                syncObservers.onObservableUpdate()
            }
        }
    }
}
