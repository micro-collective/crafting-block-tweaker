package st.evening.mc.cbtweaker.behaviour

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import st.evening.mc.cbtweaker.buffer.BufferGroups
import st.evening.mc.cbtweaker.common.BlockBehaviour
import st.evening.mc.cbtweaker.common.CraftingBlockType
import st.evening.mc.cbtweaker.gui.inventory.UiElement
import st.evening.mc.cbtweaker.util.Identifiable
import st.evening.mc.cbtweaker.util.component.RedstoneControlHandler
import st.evening.mc.cbtweaker.util.machine.ComponentSet
import st.evening.mc.cbtweaker.util.machine.TickModulator
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.tjson.JsonPath
import st.evening.mc.prelude.api.data.tjson.TJson
import st.evening.mc.prelude.api.util.game.ServerSide

interface MachineHost {
    val machineType: CraftingBlockType<*>

    @ServerSide
    fun onMachineStateChanged()
}

fun interface MachineStateFactory<S> {
    fun createState(
        world: World,
        pos: BlockPos,
        bufGroups: BufferGroups,
        components: ComponentSet,
        host: MachineHost,
        oldState: S?
    ): S
}

interface MachineBehaviour<S> : Identifiable, BlockBehaviour<S> {
    context(_: JsonPath)
    fun loadStateFactory(machine: CraftingBlockType<*>, dto: TJson.Object): MachineStateFactory<S>

    fun notifyState(state: S, newComponents: ComponentSet?)

    fun isActive(state: S): Boolean

    fun getActiveState(state: S): Piecewise?

    fun getRedstoneControlHandler(state: S): RedstoneControlHandler? = null

    fun tick(state: S, ticker: TickModulator)

    @ServerSide
    fun serializeMachineToNbt(state: S, dto: NBTTagCompound)

    @ServerSide
    fun deserializeMachineFromNbt(state: S, dto: NBTTagCompound)

    fun getMachineSyncState(state: S): Piecewise? = null

    fun createUiElement(state: S): UiElement?
}
