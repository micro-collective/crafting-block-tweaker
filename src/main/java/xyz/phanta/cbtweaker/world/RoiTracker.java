package xyz.phanta.cbtweaker.world;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xyz.phanta.cbtweaker.event.BlockStateChangedEvent;

import java.util.*;

public class RoiTracker {

    private static final long ROI_CLEANUP_INTERVAL = 5L * 60L * 1000L;

    private final TIntObjectHashMap<WorldRoiData> worldTable = new TIntObjectHashMap<>();

    public RoiTracker() {
        MinecraftForge.EVENT_BUS.register(new BlockChangeTracker());
    }

    public RoiTicket registerRoi(RoiHost host, World world, Iterator<BlockPos> region) {
        int dimId = world.provider.getDimension();
        WorldRoiData worldData = worldTable.get(dimId);
        if (worldData == null) {
            worldData = new WorldRoiData();
            worldTable.put(dimId, worldData);
        }
        return worldData.registerRoi(host, region);
    }

    private static class WorldRoiData {

        private final Map<RoiHost, Set<RoiEntry>> hostTable = new IdentityHashMap<>();
        private final TLongObjectMap<Set<RoiEntry>> regionTable = new TLongObjectHashMap<>();
        private final List<RoiEntry> invalidationQueue = new LinkedList<>();
        private long lastRoiCleanup = -1L;

        public RoiEntry registerRoi(RoiHost host, Iterator<BlockPos> region) {
            TLongSet posKeys = new TLongHashSet();
            while (region.hasNext()) {
                posKeys.add(region.next().toLong());
            }
            RoiEntry entry = new RoiEntry(host, posKeys);
            posKeys.forEach(posKey -> {
                Set<RoiEntry> entries = regionTable.get(posKey);
                if (entries == null) {
                    regionTable.put(posKey, entries = new HashSet<>());
                }
                entries.add(entry);
                return true;
            });
            hostTable.computeIfAbsent(host, k -> new HashSet<>()).add(entry);
            doLingeringWork();
            return entry;
        }

        public void notify(BlockPos pos) {
            Set<RoiEntry> entries = regionTable.get(pos.toLong());
            if (entries == null) {
                return;
            }
            for (RoiEntry entry : entries) {
                entry.notify(pos);
            }
            doLingeringWork();
        }

        public void doLingeringWork() {
            long now = System.currentTimeMillis();
            if (lastRoiCleanup == -1L) {
                lastRoiCleanup = now;
            } else if (now - lastRoiCleanup > ROI_CLEANUP_INTERVAL) {
                lastRoiCleanup = now;
                for (Map.Entry<RoiHost, Set<RoiEntry>> hostEntry : hostTable.entrySet()) {
                    if (!hostEntry.getKey().isValidRoiHost()) {
                        for (RoiEntry entry : hostEntry.getValue()) {
                            entry.invalidateRoi();
                        }
                    }
                }
            }

            if (!invalidationQueue.isEmpty()) {
                for (RoiEntry entry : invalidationQueue) {
                    entry.clearRegion();
                }
                invalidationQueue.clear();
            }
        }

        private class RoiEntry implements RoiTicket {

            private final RoiHost host;
            private final TLongSet posKeys;
            private boolean valid = true;

            public RoiEntry(RoiHost host, TLongSet posKeys) {
                this.host = host;
                this.posKeys = posKeys;
            }

            public void notify(BlockPos pos) {
                if (valid) {
                    host.onRegionChanged(this, pos);
                }
            }

            @Override
            public void invalidateRoi() {
                if (valid) {
                    valid = false;
                    invalidationQueue.add(this);
                }
            }

            private void clearRegion() {
                posKeys.forEach(posKey -> {
                    Set<RoiEntry> entries = regionTable.get(posKey);
                    if (entries != null) { // just to be safe...
                        entries.remove(this);
                        if (entries.isEmpty()) {
                            regionTable.remove(posKey);
                        }
                    }
                    return true;
                });
                Set<RoiEntry> hostEntries = hostTable.get(host);
                hostEntries.remove(this);
                if (hostEntries.isEmpty()) {
                    hostTable.remove(host);
                }
            }

        }

    }

    private class BlockChangeTracker {

        @SubscribeEvent
        public void onBlockChanged(BlockStateChangedEvent event) {
            WorldRoiData worldData = worldTable.get(event.getWorld().provider.getDimension());
            if (worldData != null) {
                worldData.notify(event.getPos());
            }
        }

    }

}
