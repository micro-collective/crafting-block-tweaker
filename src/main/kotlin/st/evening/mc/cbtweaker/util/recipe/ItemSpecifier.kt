package st.evening.mc.cbtweaker.util.recipe

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import st.evening.mc.prelude.api.data.ser.NbtCompoundSerializer
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.data.tjson.expectShort
import st.evening.mc.prelude.api.data.tjson.useObject
import st.evening.mc.prelude.api.data.tjson.useStringValue
import st.evening.mc.prelude.api.util.game.ItemKey
import st.evening.mc.prelude.api.util.game.OreEntry
import st.evening.mc.prelude.api.util.game.RegistryHelper
import st.evening.mc.prelude.api.util.game.dataTagOrNull
import st.evening.mc.prelude.api.util.game.newStackWithData
import java.util.function.Predicate

sealed interface ItemSpecifier : Predicate<ItemStack> {
    val item: Item

    val dataTag: NBTTagCompound?

    fun matchesOreEntry(oreEntry: OreEntry): Boolean

    fun newStack(count: Int): ItemStack

    companion object {
        private const val SER_ITEM: String = "item"
        private const val SER_DATA_TAG: String = "data_tag"

        context(_: JsonPath)
        fun load(dto: TJson.Object): ItemSpecifier = dto.expectShort("meta")?.let { meta ->
            ByMeta(
                dto.useStringValue(SER_ITEM) { RegistryHelper.getAssertItem(contextOf<JsonPath>(), it) },
                meta.toInt(),
                dto.useObject(SER_DATA_TAG) { NbtCompoundSerializer.deserializeFromJson(it) }
            )
        } ?: Wildcard(
            dto.useStringValue(SER_ITEM) { RegistryHelper.getAssertItem(contextOf<JsonPath>(), it) },
            dto.useObject(SER_DATA_TAG) { NbtCompoundSerializer.deserializeFromJson(it) }
        )
    }

    data class Wildcard(override val item: Item, override val dataTag: NBTTagCompound?) : ItemSpecifier {
        companion object {
            fun fromStack(stack: ItemStack): Wildcard = Wildcard(stack.item, stack.dataTagOrNull)

            fun fromKey(key: ItemKey): Wildcard = Wildcard(key.item, key.dataTag)
        }

        override fun test(stack: ItemStack): Boolean = stack.item == item && stack.dataTagOrNull == dataTag

        override fun matchesOreEntry(oreEntry: OreEntry): Boolean = oreEntry.matches(item, 0)

        override fun newStack(count: Int): ItemStack = item.newStackWithData(count, 0, dataTag)
    }

    data class ByMeta(override val item: Item, val meta: Int, override val dataTag: NBTTagCompound?) : ItemSpecifier {
        companion object {
            fun fromStack(stack: ItemStack): ByMeta = ByMeta(stack.item, stack.metadata, stack.dataTagOrNull)

            fun fromKey(key: ItemKey): ByMeta = ByMeta(key.item, key.meta, key.dataTag)
        }

        override fun test(stack: ItemStack): Boolean =
            stack.item == item && stack.metadata == meta && stack.dataTagOrNull == dataTag

        override fun matchesOreEntry(oreEntry: OreEntry): Boolean = oreEntry.matches(item, meta)

        override fun newStack(count: Int): ItemStack = item.newStackWithData(count, meta, dataTag)
    }
}
