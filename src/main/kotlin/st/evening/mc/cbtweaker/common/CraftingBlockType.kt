package st.evening.mc.cbtweaker.common

import net.minecraft.block.Block
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.MachineStateFactory
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig

interface CraftingBlockType<S> {
    val id: String

    val blockConfig: BlockConfig

    val craftingBlock: Block

    val behaviour: MachineBehaviour<S>

    val stateFactory: MachineStateFactory<S>

    val windowConfig: WindowConfig
}
