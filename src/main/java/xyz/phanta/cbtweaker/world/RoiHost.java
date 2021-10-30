package xyz.phanta.cbtweaker.world;

import net.minecraft.util.math.BlockPos;

public interface RoiHost {

    boolean isValidRoiHost();

    void onRegionChanged(RoiTicket ticket, BlockPos pos);

}
