package st.evening.mc.cbtweaker.structure

import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.multiblock.MultiBlockControllerBlock
import st.evening.mc.cbtweaker.multiblock.MultiBlockType
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.isEqual
import st.evening.mc.prelude.api.util.world.BlockSide

@ClientSide.Physical
data class ClientVisualizationState(
    val visToolStack: ItemStack,
    val hand: EnumHand,
    val mbType: MultiBlockType<*>,
    val ctrlPos: BlockPos,
    val ctrlFront: BlockSide,
    val mbStack: ItemStack,
    val renderer: VisualizationRenderer
) {
    companion object {
        private var currentState: ClientVisualizationState? = null
        private var lastUpdateTick: Long = -1

        fun getState(): ClientVisualizationState? {
            val now = Prelude.defns.client().clientTick
            if (now > lastUpdateTick) {
                lastUpdateTick = now
                updateState()
            }
            return currentState
        }

        private fun updateState() {
            val player = Minecraft.getMinecraft().player
            if (player == null) {
                currentState = null
                return
            }

            val hand: EnumHand
            var stack = player.heldItemMainhand
            if (stack.item == CbTweaker.defns.itemVisualizationTool) {
                hand = EnumHand.MAIN_HAND
            } else {
                stack = player.heldItemOffhand
                if (stack.item == CbTweaker.defns.itemVisualizationTool) {
                    hand = EnumHand.OFF_HAND
                } else {
                    currentState = null
                    return
                }
            }

            val state = currentState
            if (state != null && state.visToolStack.isEqual(stack)) return

            val world = player.world
            val ctrlPos = VisualizationToolItem.getBoundPos(stack)
            if (ctrlPos == null || VisualizationToolItem.getBoundDim(stack) != world.provider.dimension) {
                currentState = null
                return
            }
            val ctrlState = world.getBlockState(ctrlPos)
            val ctrlBlock = ctrlState.getBlock()
            if (ctrlBlock !is MultiBlockControllerBlock) {
                currentState = null
                return
            }

            val mbType = ctrlBlock.mbType
            if (state != null && state.mbType == mbType) {
                currentState = state.copy(
                    visToolStack = stack,
                    hand = hand,
                    ctrlPos = ctrlPos,
                    ctrlFront = ctrlState.getValue(BlockSidedIfc.PROP_FACING)
                )
                return
            }

            val visRenderer = VisualizationRenderer(mbType.structureMatcher)
            visRenderer.level = VisualizationToolItem.getLevel(stack)
            currentState = ClientVisualizationState(
                stack,
                hand,
                mbType,
                ctrlPos,
                ctrlState.getValue(BlockSidedIfc.PROP_FACING),
                ItemStack(mbType.controllerBlock),
                visRenderer
            )
        }
    }
}
