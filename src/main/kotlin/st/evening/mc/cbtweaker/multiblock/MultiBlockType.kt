package st.evening.mc.cbtweaker.multiblock

import net.minecraft.block.Block
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.MachineStateFactory
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.structure.StructureMatcher
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.block

class MultiBlockType<S>(
    reg: ModRegistrar,
    override val id: String,
    override val blockConfig: BlockConfig,
    override val behaviour: MachineBehaviour<S>,
    override val windowConfig: WindowConfig
) : CraftingBlockType<S> {
    // multiblock type has to be initialized at pre-init time so that the controller block can be constructed
    // however, these properties may rely on registry entries from other mods, so they must be initialized at init time
    lateinit var structureMatcher: StructureMatcher
        private set
    override lateinit var stateFactory: MachineStateFactory<S>
        private set

    val controllerBlock: MultiBlockControllerBlock by reg.block("mb_$id") { MultiBlockControllerBlock(this) }

    override val craftingBlock: Block
        get() = controllerBlock

    internal fun init(structureMatcher: StructureMatcher, stateFactory: MachineStateFactory<S>) {
        this.structureMatcher = structureMatcher
        this.stateFactory = stateFactory
    }

    val translationKey: String
        get() = "${CbtConsts.MOD_ID}.multiblock.$id.name"
}
