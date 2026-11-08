package st.evening.mc.cbtweaker.multiblock

import net.minecraft.block.ITileEntityProvider
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.CbtConsts
import st.evening.mc.cbtweaker.common.BlockBehaviour
import st.evening.mc.cbtweaker.common.CustomBlockType
import st.evening.mc.cbtweaker.common.OrientableMachineBlock
import st.evening.mc.prelude.api.registration.ContainerType
import st.evening.mc.prelude.api.util.world.findTileEntity

class MultiBlockControllerBlock private constructor(val mbType: MultiBlockType) :
    OrientableMachineBlock(mbType.blockConfig), ITileEntityProvider {
    companion object {
        fun construct(mbType: MultiBlockType): MultiBlockControllerBlock =
            MultiBlockControllerBlock(mbType).also { it.init() }
    }

    override val blockType: CustomBlockType
        get() = mbType

    override fun createNewTileEntity(world: World, meta: Int): TileEntity = MultiBlockControllerTileEntity()

    override fun getBehaviourHandler(world: World, pos: BlockPos): BlockBehaviour.Handler? =
        world.findTileEntity<MultiBlockControllerTileEntity>(pos)

    override fun getContainerType(): ContainerType = CbTweaker.defns.containerMultiBlockController

    override fun getTranslationKey(): String = "${CbtConsts.MOD_ID}.multiblock.${mbType.id}.controller"
}
