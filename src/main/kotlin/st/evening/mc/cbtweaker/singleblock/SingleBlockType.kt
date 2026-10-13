package st.evening.mc.cbtweaker.singleblock

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMap
import net.minecraft.block.Block
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.behaviour.MachineBehaviour
import st.evening.mc.cbtweaker.behaviour.MachineStateFactory
import st.evening.mc.cbtweaker.buffer.BufferGroup
import st.evening.mc.cbtweaker.buffer.BufferGroups
import st.evening.mc.cbtweaker.buffer.BufferObserver
import st.evening.mc.cbtweaker.common.BlockConfig
import st.evening.mc.cbtweaker.common.CraftingBlockData
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.compat.jei.recipe.JeiBufferGroup
import st.evening.mc.cbtweaker.gui.inventory.WindowConfig
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.block

class SingleBlockType(
    reg: ModRegistrar,
    override val id: String,
    override val blockConfig: BlockConfig,
) : CraftingBlockType {
    val machineBlock: SingleBlockMachineBlock by reg.block("sb_$id") { SingleBlockMachineBlock.construct(this) }

    override val craftingBlock: Block
        get() = machineBlock

    override lateinit var data: Data<*>
        private set

    internal fun <S> initData(
        behaviour: MachineBehaviour<S>,
        stateFactory: MachineStateFactory<S>,
        bufGroupFactories: Object2ObjectSortedMap<String, BufferGroup.Factory>,
        windowConfig: WindowConfig
    ) {
        data = Data(behaviour, stateFactory, bufGroupFactories, windowConfig)
    }

    val translationKey: String
        get() = "${CbtConsts.MOD_ID}.singleblock.$id.name"

    class Data<S>(
        override val behaviour: MachineBehaviour<S>,
        override val stateFactory: MachineStateFactory<S>,
        val bufGroupFactories: Object2ObjectSortedMap<String, BufferGroup.Factory>,
        override val windowConfig: WindowConfig
    ) : CraftingBlockData<S> {
        fun createBufferGroups(world: World, pos: BlockPos, observer: BufferObserver): BufferGroups {
            // Note that Object2ObjectLinkedOpenHashMap maintains insertion order and NOT the natural key order, but so long
            // as we insert the keys in the correct order, we can cheat a little since the buffer group map isn't supposed
            // to be changed after construction
            val bufGroups = Object2ObjectLinkedOpenHashMap<String, BufferGroup>(bufGroupFactories.size)
            bufGroupFactories.forEach { (bufGroupId, factory) ->
                bufGroups[bufGroupId] = factory.createBufferGroup(world, pos, observer)
            }
            return bufGroups
        }

        fun createJeiBufferGroups(): Map<String, JeiBufferGroup> {
            val jeiBufGroups = mutableMapOf<String, JeiBufferGroup>()
            bufGroupFactories.forEach { (bufGroupId, factory) ->
                jeiBufGroups[bufGroupId] = factory.createJeiBufferGroup()
            }
            return jeiBufGroups
        }
    }
}
