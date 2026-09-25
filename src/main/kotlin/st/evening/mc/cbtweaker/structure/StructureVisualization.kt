package st.evening.mc.cbtweaker.structure

import net.minecraft.util.math.Vec3i
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcher

data class StructureVisualization(val matchers: Map<Vec3i, StructureBlockMatcher>, val mirrorX: Boolean)
