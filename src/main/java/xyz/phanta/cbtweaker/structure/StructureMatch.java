package xyz.phanta.cbtweaker.structure;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import xyz.phanta.cbtweaker.hatch.HatchTileEntity;
import xyz.phanta.cbtweaker.recipe.ComponentSet;

import java.util.*;

public class StructureMatch {

    private final Set<BlockPos> positions = new HashSet<>();
    private final Map<String, List<HatchTileEntity>> hatchTable = new HashMap<>();
    private final ComponentSet components = new ComponentSet();

    public Set<BlockPos> getPositions() {
        return positions;
    }

    public void addPosition(BlockPos pos) {
        positions.add(pos);
    }

    public Map<String, List<HatchTileEntity>> getHatches() {
        return hatchTable;
    }

    public void addHatch(String groupId, HatchTileEntity hatch) {
        hatchTable.computeIfAbsent(groupId, k -> new ArrayList<>()).add(hatch);
    }

    public ComponentSet getComponents() {
        return components;
    }

    public void addComponent(String compId) {
        components.put(compId);
    }

    public void addFrom(StructureMatch o, int offsetX, int offsetY, int offsetZ) {
        for (BlockPos pos : o.positions) {
            positions.add(pos.add(offsetX, offsetY, offsetZ));
        }
        for (Map.Entry<String, List<HatchTileEntity>> bufGroupEntry : o.hatchTable.entrySet()) {
            hatchTable.computeIfAbsent(bufGroupEntry.getKey(), k -> new ArrayList<>()).addAll(bufGroupEntry.getValue());
        }
        components.addFrom(o.components);
    }

    public void addFrom(StructureMatch o, EnumFacing offsetDir, int offsetLength) {
        Vec3i vec = offsetDir.getDirectionVec();
        addFrom(o, vec.getX() * offsetLength, vec.getY() * offsetLength, vec.getZ() * offsetLength);
    }

    public void addFrom(StructureMatch o) {
        addFrom(o, 0, 0, 0);
    }

}
