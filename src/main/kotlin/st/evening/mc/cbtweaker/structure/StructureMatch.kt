package st.evening.mc.cbtweaker.structure

import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.hatch.HatchTileEntity
import st.evening.mc.cbtweaker.util.machine.MutableComponentSet

class StructureMatch {
    val positions: MutableSet<BlockPos> = mutableSetOf()
    val hatches: MutableMap<String, MutableList<HatchTileEntity>> = mutableMapOf()
    val components: MutableComponentSet = MutableComponentSet()

    fun addPosition(pos: BlockPos) {
        positions += pos
    }

    fun addHatch(groupId: String, hatch: HatchTileEntity) {
        hatches.getOrPut(groupId) { mutableListOf() } += hatch
    }

    fun addComponent(compId: String) {
        components.put(compId)
    }

    fun addFrom(o: StructureMatch, offsetX: Int = 0, offsetY: Int = 0, offsetZ: Int = 0) {
        o.positions.forEach {
            positions.add(it.add(offsetX, offsetY, offsetZ))
        }
        o.hatches.forEach { (groupId, bufGroupHatches) ->
            hatches.getOrPut(groupId) { mutableListOf() }.addAll(bufGroupHatches)
        }
        components.addAll(o.components)
    }

    fun addFrom(o: StructureMatch, offsetDir: EnumFacing, offsetLength: Int) {
        val vec = offsetDir.directionVec
        addFrom(o, vec.x * offsetLength, vec.y * offsetLength, vec.z * offsetLength)
    }
}
