package st.evening.mc.cbtweaker.world

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.prelude.api.event.BlockStateChangedEvent
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.collection.forEachInline
import st.evening.mc.prelude.api.util.collection.getOrPutNonNull
import st.evening.mc.prelude.api.util.collection.minusAssign
import st.evening.mc.prelude.api.util.collection.plusAssign
import java.util.IdentityHashMap
import java.util.LinkedList

class RoiTracker(reg: ModRegistrar) {
    companion object {
        private const val ROI_CLEANUP_INTERVAL: Long = 5L * 60L * 1000L
    }

    private val worldTable: Int2ObjectMap<WorldRoiData> = Int2ObjectOpenHashMap()

    init {
        reg.on<BlockStateChangedEvent> { event ->
            worldTable[event.world.provider.dimension]?.notify(event.pos)
        }
    }

    fun registerRoi(host: RoiHost, world: World, region: Iterator<BlockPos>): RoiTicket =
        worldTable.getOrPutNonNull(world.provider.dimension) { WorldRoiData() }.registerRoi(host, region)

    private class WorldRoiData {
        private val hostTable: MutableMap<RoiHost, MutableSet<RoiEntry>> = IdentityHashMap()
        private val regionTable: Long2ObjectMap<MutableSet<RoiEntry>> = Long2ObjectOpenHashMap()
        private val invalidationQueue: MutableList<RoiEntry> = LinkedList()

        private var lastRoiCleanup: Long = -1L

        fun registerRoi(host: RoiHost, region: Iterator<BlockPos>): RoiTicket {
            val posKeys = LongOpenHashSet()
            region.forEach { posKeys += it.toLong() }
            val entry = RoiEntry(host, posKeys)
            posKeys.forEachInline { posKey ->
                regionTable.getOrPutNonNull(posKey) { mutableSetOf() } += entry
            }
            hostTable.getOrPut(host) { mutableSetOf() } += entry
            doLingeringWork()
            return entry
        }

        fun notify(pos: BlockPos) {
            regionTable[pos.toLong()]?.let { entries ->
                entries.forEach { it.notify(pos) }
                doLingeringWork()
            }
        }

        fun doLingeringWork() {
            val now = System.currentTimeMillis()
            if (lastRoiCleanup == -1L) {
                lastRoiCleanup = now
            } else if (now - lastRoiCleanup > ROI_CLEANUP_INTERVAL) {
                lastRoiCleanup = now
                hostTable.forEach { (host, entries) ->
                    if (!host.isValidRoiHost) {
                        entries.forEach { it.invalidateRoi() }
                    }
                }
            }

            if (invalidationQueue.isNotEmpty()) {
                invalidationQueue.forEach { it.clearRegion() }
                invalidationQueue.clear()
            }
        }

        private inner class RoiEntry(private val host: RoiHost, private val posKeys: LongSet) : RoiTicket {
            private var valid: Boolean = true

            fun notify(pos: BlockPos) {
                if (valid) {
                    host.onRegionChanged(this, pos)
                }
            }

            override fun invalidateRoi() {
                if (valid) {
                    valid = false
                    invalidationQueue += this
                }
            }

            fun clearRegion() {
                posKeys.forEachInline { posKey ->
                    regionTable[posKey]?.let { entries ->
                        entries -= this
                        if (entries.isEmpty()) {
                            regionTable -= posKey
                        }
                    }
                }
                hostTable[host]?.let { entries ->
                    entries -= this
                    if (entries.isEmpty()) {
                        hostTable -= host
                    }
                }
            }
        }
    }
}
