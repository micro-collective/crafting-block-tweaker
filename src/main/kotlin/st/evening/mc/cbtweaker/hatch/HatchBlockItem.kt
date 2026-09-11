package st.evening.mc.cbtweaker.hatch

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.ItemStack
import net.minecraftforge.client.model.ModelLoader
import st.evening.mc.cbtweaker.common.CbtCustomBlockItem
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.mapPath

class HatchBlockItem(private val hatchBlock: HatchBlock) : CbtCustomBlockItem(hatchBlock), CustomModelItem {
    init {
        setHasSubtypes(true)
    }

    override fun getMetadata(damage: Int): Int = damage

    override fun getTranslationKey(stack: ItemStack): String = hatchBlock.translationKey + "." + stack.metadata

    @ClientSide.Physical
    override fun registerItemModels() {
        val regName = registryName!!
        for (i in 0..<hatchBlock.hatchType.tierCount) {
            ModelLoader.setCustomModelResourceLocation(
                this, i, ModelResourceLocation(regName.mapPath { "$it/${it}_$i" }, "inventory")
            )
        }
    }
}
