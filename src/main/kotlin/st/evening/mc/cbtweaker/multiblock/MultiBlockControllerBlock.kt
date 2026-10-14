package st.evening.mc.cbtweaker.multiblock

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

class MultiBlockControllerBlock private constructor(val mbType: MultiBlockType) :
    OrientableMachineBlock(mbType.blockConfig), TileEntityBlock {
    companion object {
        fun construct(mbType: MultiBlockType): MultiBlockControllerBlock =
            MultiBlockControllerBlock(mbType).also { it.init() }
    }

    override val blockType: CustomBlockType
        get() = mbType

    override fun getTileEntityType(world: World, meta: Int): TileEntityType<*> =
        CbTweaker.defns.tileEntityMultiBlockController

    override fun getBehaviourHandler(world: World, pos: BlockPos): BlockBehaviour.Handler? =
        world.findTileEntity<MultiBlockControllerTileEntity>(pos)

    override fun getContainerType(): ContainerType = CbTweaker.defns.containerMultiBlockController

    override fun getTranslationKey(): String = "${CbtConsts.MOD_ID}.multiblock.${mbType.id}.controller"
}
