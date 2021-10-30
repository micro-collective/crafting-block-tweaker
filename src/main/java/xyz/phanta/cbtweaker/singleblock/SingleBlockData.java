package xyz.phanta.cbtweaker.singleblock;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.buffer.BufferObserver;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.common.MachineRecipeHandler;
import xyz.phanta.cbtweaker.common.MachineRecipeHost;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerBlock;
import xyz.phanta.cbtweaker.recipe.ComponentSet;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.util.RefreshState;
import xyz.phanta.cbtweaker.util.block.MachineSideHandler;
import xyz.phanta.cbtweaker.util.block.RedstoneControlHandler;

import java.util.*;

public class SingleBlockData<R, C, D, S, E> implements MachineRecipeHost<R, C, D, S, E>, BufferObserver, ISerializable {

    public static <R, C, D> SingleBlockData<R, C, D, ?, ?> construct(SingleBlockMachineTileEntity sbMachine,
                                                                     SingleBlockType<R, C, D> sbType) {
        return new SingleBlockData<>(sbMachine, sbType, sbType.getRecipeLogic());
    }

    private final SingleBlockMachineTileEntity sbMachine;
    private final SingleBlockType<R, C, D> sbType;
    private final RecipeLogic<R, C, D, S, E> recipeLogic;

    private final SortedMap<String, BufferGroup> bufGroups;
    private final MachineSideHandler sideHandler;
    private final MachineRecipeHandler<R, C, D, S, E> recipeHandler;
    private final RedstoneControlHandler rsHandler = new RedstoneControlHandler(this::setDirty);

    private RefreshState refreshState = RefreshState.NONE;

    private boolean wasActiveLastTick = false;

    public SingleBlockData(SingleBlockMachineTileEntity sbMachine,
                           SingleBlockType<R, C, D> sbType, RecipeLogic<R, C, D, S, E> recipeLogic) {
        this.sbMachine = sbMachine;
        this.sbType = sbType;
        this.recipeLogic = recipeLogic;
        World world = sbMachine.getWorld();
        BlockPos pos = sbMachine.getPos();
        this.bufGroups = sbType.createBufferGroups(world, pos, this);
        this.sideHandler = new MachineSideHandler(world, pos,
                () -> world.getBlockState(pos).getValue(MultiBlockControllerBlock.PROP_DIRECTION),
                this::setDirty, bufGroups);
        this.recipeHandler = new MachineRecipeHandler<>(this, ComponentSet.EMPTY, bufGroups);
        rsHandler.setRedstoneState(world.isBlockPowered(pos));
    }

    public SingleBlockType<R, C, D> getSingleBlockType() {
        return sbType;
    }

    @Override
    public RecipeLogic<R, C, D, S, E> getRecipeLogic() {
        return recipeLogic;
    }

    @Override
    public C getRecipeConfig() {
        return sbType.getRecipeConfig();
    }

    @Override
    public D getRecipeDatabase() {
        return sbType.getRecipeDatabase();
    }

    public boolean isActive() {
        return rsHandler.canWork() && recipeHandler.getCurrentRecipe() != null;
    }

    public MachineSideHandler getSideHandler() {
        return sideHandler;
    }

    public RedstoneControlHandler getRedstoneHandler() {
        return rsHandler;
    }

    public List<Pair<Object, UiElement>> createBufferUiElements() {
        List<Pair<Object, UiElement>> uiElems = new ArrayList<>();
        for (BufferGroup bufGroup : bufGroups.values()) {
            bufGroup.forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    for (B buffer : buffers) {
                        uiElems.add(Pair.of(buffer, bufType.createUiElement(buffer)));
                    }
                }
            });
        }
        return uiElems;
    }

    public UiElement createRecipeLogicUiElement() {
        return recipeLogic.createUiElement(sbType.getRecipeConfig(), recipeHandler);
    }

    public boolean handleInteraction(World world, BlockPos pos, IBlockState state,
                                     EntityPlayer player, EnumHand hand, EnumFacing face) {
        Iterator<MachineSideHandler.SideConfig<?>> iter = sideHandler.getSideConfigs(face).iterator();
        while (iter.hasNext()) {
            if (handleInteraction(iter.next(), world, pos, state, player, hand, face)) {
                return true;
            }
        }
        return false;
    }

    private <B> boolean handleInteraction(MachineSideHandler.SideConfig<B> sideConfig,
                                          World world, BlockPos pos, IBlockState state,
                                          EntityPlayer player, EnumHand hand, EnumFacing face) {
        return sideConfig.getBufferType()
                .handleInteraction(sideConfig.getBuffer(), state, player, hand, face);
    }

    public void dropContents(World world, BlockPos pos) {
        for (BufferGroup bufGroup : bufGroups.values()) {
            bufGroup.forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    for (B buffer : buffers) {
                        bufType.dropContents(buffer);
                    }
                }
            });
        }
    }

    @Override
    public void setDirty() {
        sbMachine.setDirty();
    }

    @Override
    public void onIngredientsChanged() {
        refreshState = RefreshState.SOFT_REFRESH;
        setDirty();
    }

    @Override
    public void onComponentsChanged() {
        refreshState = RefreshState.HARD_REFRESH;
        setDirty();
    }

    public void tick() {
        World world = sbMachine.getWorld();
        boolean active;
        if (rsHandler.canWork()) {
            if (refreshState != RefreshState.NONE) {
                recipeHandler.refreshState(refreshState == RefreshState.HARD_REFRESH);
                refreshState = RefreshState.NONE;
            }
            if (!world.isRemote) {
                recipeHandler.tick();
            }
            active = recipeHandler.getCurrentRecipe() != null;
        } else {
            active = false;
        }
        if (wasActiveLastTick != active) {
            wasActiveLastTick = active;
            BlockPos pos = sbMachine.getPos();
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
        sideHandler.tick();
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        for (BufferGroup bufGroup : bufGroups.values()) {
            bufGroup.forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    for (B buffer : buffers) {
                        bufType.serializeBufferBytes(data, buffer);
                    }
                }
            });
        }
        sideHandler.serBytes(data);
        recipeHandler.serializeToBytes(data);
        rsHandler.serBytes(data);
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        for (BufferGroup bufGroup : bufGroups.values()) {
            bufGroup.forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    for (B buffer : buffers) {
                        bufType.deserializeBufferBytes(data, buffer);
                    }
                }
            });
        }
        sideHandler.deserBytes(data);
        recipeHandler.deserializeFromBytes(data, data.readByte());
        rsHandler.deserBytes(data);
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        NBTTagCompound bufGroupsTag = new NBTTagCompound();
        for (Map.Entry<String, BufferGroup> bufGroupEntry : bufGroups.entrySet()) {
            NBTTagCompound bufGroupTag = new NBTTagCompound();
            bufGroupEntry.getValue().forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    NBTTagList buffersTag = new NBTTagList();
                    for (B buffer : buffers) {
                        NBTTagCompound bufferTag = new NBTTagCompound();
                        bufType.serializeBufferNbt(bufferTag, buffer);
                        buffersTag.appendTag(bufferTag);
                    }
                    bufGroupTag.setTag(bufType.getId().toString(), buffersTag);
                }
            });
            bufGroupsTag.setTag(bufGroupEntry.getKey(), bufGroupTag);
        }
        tag.setTag("Buffers", bufGroupsTag);
        for (BufferGroup bufGroup : bufGroups.values()) {
            bufGroup.forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    for (B buffer : buffers) {
                        bufType.serializeBufferNbt(tag, buffer);
                    }
                }
            });
        }
        NBTTagCompound sideHandlerTag = new NBTTagCompound();
        sideHandler.serNBT(sideHandlerTag);
        tag.setTag("Sides", sideHandlerTag);
        NBTTagCompound recipeHandlerTag = new NBTTagCompound();
        recipeHandler.serializeToNbt(recipeHandlerTag);
        tag.setTag("RecipeHandler", recipeHandlerTag);
        rsHandler.writeToNbt(tag, "RedstoneControl");
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        NBTTagCompound bufGroupsTag = tag.getCompoundTag("Buffers");
        for (Map.Entry<String, BufferGroup> bufGroupEntry : bufGroups.entrySet()) {
            NBTTagCompound bufGroupTag = bufGroupsTag.getCompoundTag(bufGroupEntry.getKey());
            if (bufGroupTag.isEmpty()) {
                continue;
            }
            bufGroupEntry.getValue().forEach(new BufferGroup.Visitor() {
                @Override
                public <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers) {
                    NBTTagList buffersTag = bufGroupTag.getTagList(
                            bufType.getId().toString(), Constants.NBT.TAG_COMPOUND);
                    if (!buffersTag.isEmpty()) {
                        int limit = Math.min(buffers.size(), buffersTag.tagCount());
                        for (int i = 0; i < limit; i++) {
                            bufType.deserializeBufferNbt(buffersTag.getCompoundTagAt(i), buffers.get(i));
                        }
                    }
                }
            });
        }
        sideHandler.deserNBT(tag.getCompoundTag("Sides"));
        recipeHandler.deserializeFromNbt(tag.getCompoundTag("RecipeHandler"));
        rsHandler.readFromNbt(tag, "RedstoneControl");
    }

}
