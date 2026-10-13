package st.evening.mc.cbtweaker.multiblock

import net.minecraft.block.Block
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.MachineStateFactory
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.common.CraftingBlockData
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.cbtweaker.structure.StructureMatcher
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.block

class MultiBlockType(
    reg: ModRegistrar,
    override val id: String,
    override val blockConfig: BlockConfig,
) : CraftingBlockType {
    val controllerBlock: MultiBlockControllerBlock by reg.block("mb_$id") { MultiBlockControllerBlock.construct(this) }

    override val craftingBlock: Block
        get() = controllerBlock

    override lateinit var data: Data<*>
        private set

    internal fun <S> initData(
        behaviour: MachineBehaviour<S>,
        stateFactory: MachineStateFactory<S>,
        structureMatcher: StructureMatcher<*>,
        windowConfig: WindowConfig
    ) {
        data = Data(behaviour, stateFactory, structureMatcher, windowConfig)
    }

    val translationKey: String
        get() = "${CbtConsts.MOD_ID}.multiblock.$id.name"

    class Data<S>(
        override val behaviour: MachineBehaviour<S>,
        override val stateFactory: MachineStateFactory<S>,
        val structureMatcher: StructureMatcher<*>,
        override val windowConfig: WindowConfig
    ) : CraftingBlockData<S>
}
