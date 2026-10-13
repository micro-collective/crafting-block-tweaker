package st.evening.mc.cbtweaker.common

import net.minecraft.block.Block
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.MachineStateFactory
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig

interface CraftingBlockType : CustomBlockType {
    val id: String

    val craftingBlock: Block

    val data: CraftingBlockData<*>
}

interface CraftingBlockData<S> {
    val behaviour: MachineBehaviour<S>

    val stateFactory: MachineStateFactory<S>

    val windowConfig: WindowConfig
}
