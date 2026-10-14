package st.evening.mc.cbtweaker.singleblock

import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.common.BlockBehaviour
import st.evening.mc.cbtweaker.common.CustomBlockType
import st.evening.mc.cbtweaker.common.OrientableMachineBlock
import st.evening.mc.prelude.api.block.TileEntityBlock
import st.evening.mc.prelude.api.registration.ContainerType
import st.evening.mc.prelude.api.registration.TileEntityType
import st.evening.mc.prelude.api.util.world.findTileEntity

class SingleBlockMachineBlock private constructor(val sbType: SingleBlockType) :
    OrientableMachineBlock(sbType.blockConfig), TileEntityBlock {
    companion object {
        fun construct(sbType: SingleBlockType): SingleBlockMachineBlock =
            SingleBlockMachineBlock(sbType).also { it.init() }
    }

    override val blockType: CustomBlockType
        get() = sbType

    override fun getTileEntityType(world: World, meta: Int): TileEntityType<*> =
        CbTweaker.defns.tileEntitySingleBlockMachine

    override fun getBehaviourHandler(world: World, pos: BlockPos): BlockBehaviour.Handler? =
        world.findTileEntity<SingleBlockMachineTileEntity>(pos)

    override fun getContainerType(): ContainerType = CbTweaker.defns.containerSingleBlockMachine

    override fun getTranslationKey(): String = "${CbtConsts.MOD_ID}.singleblock.${sbType.id}"
}
