package xyz.phanta.cbtweaker.multiblock;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.data.ISerializable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.recipe.RecipeExecutor;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.structure.StructureMatch;
import xyz.phanta.cbtweaker.util.RefreshState;
import xyz.phanta.cbtweaker.util.block.RedstoneControlHandler;
import xyz.phanta.cbtweaker.world.RoiHost;
import xyz.phanta.cbtweaker.world.RoiTicket;

import javax.annotation.Nullable;

public class MultiBlockData<R, C, D, S, E> implements RoiHost, ISerializable {

    public static <R, C, D> MultiBlockData<R, C, D, ?, ?> construct(MultiBlockControllerTileEntity mbCtrl,
                                                                    MultiBlockType<R, C, D> mbType) {
        return new MultiBlockData<>(mbCtrl, mbType, mbType.getRecipeLogic());
    }

    private final MultiBlockControllerTileEntity mbCtrl;
    private final MultiBlockType<R, C, D> mbType;
    private final RecipeLogic<R, C, D, S, E> recipeLogic;
    private final RedstoneControlHandler rsHandler = new RedstoneControlHandler(this::markControllerStateDirty);

    @Nullable
    private RoiTicket mbRoiTicket = null;
    @Nullable
    private MultiBlockAssembly<R, C, D, S, E> assembly = null;
    @Nullable
    private NBTTagCompound bufferedAssemblyDeser = null;
    private boolean structDirty = false;
    private RefreshState refreshState = RefreshState.NONE;

    private boolean wasActiveLastTick = false;

    public MultiBlockData(MultiBlockControllerTileEntity mbCtrl,
                          MultiBlockType<R, C, D> mbType, RecipeLogic<R, C, D, S, E> recipeLogic) {
        this.mbCtrl = mbCtrl;
        this.mbType = mbType;
        this.recipeLogic = recipeLogic;
        rsHandler.setRedstoneState(mbCtrl.getWorld().isBlockPowered(mbCtrl.getPos()));
    }

    public MultiBlockType<R, C, D> getMultiBlockType() {
        return mbType;
    }

    public RecipeLogic<R, C, D, S, E> getRecipeLogic() {
        return recipeLogic;
    }

    public boolean isAssembled() {
        return assembly != null;
    }

    public boolean isActive() {
        return rsHandler.canWork() && assembly != null && assembly.isActive();
    }

    public RedstoneControlHandler getRedstoneHandler() {
        return rsHandler;
    }

    public UiElement createRecipeLogicUiElement() {
        return recipeLogic.createUiElement(mbType.getRecipeConfig(),
                new RecipeExecutor.OfNullable<>(() -> assembly != null ? assembly.getRecipeExecutor() : null));
    }

    @Override
    public boolean isValidRoiHost() {
        return !mbCtrl.isInvalid();
    }

    private void dropRoi(boolean invalidateAssembly) {
        if (mbRoiTicket != null) {
            mbRoiTicket.invalidateRoi();
            if (invalidateAssembly) {
                if (assembly != null) {
                    assembly.disassociateHatches(mbCtrl);
                }
                structDirty = true;
            }
            mbRoiTicket = null;
        }
    }

    private void acquireBaseRoi() {
        World world = mbCtrl.getWorld();
        BlockPos pos = mbCtrl.getPos();
        mbRoiTicket = CbtMod.PROXY.getRoiTracker().registerRoi(this, world, mbType.getStructureMatcher()
                .getRegion(world, pos, world.getBlockState(pos).getValue(MultiBlockControllerBlock.PROP_DIRECTION)));
    }

    public void onInvalidated() {
        dropRoi(true);
    }

    @Override
    public void onRegionChanged(RoiTicket ticket, BlockPos pos) {
        Vec3i posOffset = pos.subtract(mbCtrl.getPos());
        if (posOffset.equals(Vec3i.NULL_VECTOR)) {
            dropRoi(true);
        }
        structDirty = true;
    }

    public void notifyHatchChanged(boolean compsDirty) {
        refreshState = compsDirty ? RefreshState.HARD_REFRESH : RefreshState.SOFT_REFRESH;
    }

    public void markControllerStateDirty() {
        mbCtrl.setDirty();
    }

    public void tick() {
        World world = mbCtrl.getWorld();
        BlockPos pos = mbCtrl.getPos();
        if (mbRoiTicket == null) {
            acquireBaseRoi();
            structDirty = true;
        }
        if (structDirty) {
            StructureMatch structMatch = mbType.getStructureMatcher()
                    .findMatch(world, pos, world.getBlockState(pos).getValue(MultiBlockControllerBlock.PROP_DIRECTION));
            if (structMatch != null) {
                assembly = MultiBlockAssembly.fromStructure(this, structMatch);
                // this only really works for the simple structure matcher, because other structures can potentially
                // grow without needing an original block removed, e.g. the linear matcher
                // in the future, could allow the matcher itself to define dynamic ROIs, but this works for now
                /*dropRoi(false);
                mbRoiTicket = CbtMod.PROXY.getRoiTracker()
                        .registerRoi(this, world, assembly.getStructureBlocks().iterator());*/
                assembly.associateHatches(mbCtrl);
                if (bufferedAssemblyDeser != null) {
                    assembly.deserializeFromNbt(bufferedAssemblyDeser);
                    bufferedAssemblyDeser = null;
                }
                // recheck recipe immediately once assembled
                // don't need to hard-refresh because the executor state will be fresh anyways
                refreshState = RefreshState.SOFT_REFRESH;
            } else {
                assembly = null;
                // not needed if the dynamic ROIs as mentioned above are not implemented
                //dropRoi(true);
            }
            structDirty = false;
        }
        boolean active;
        if (assembly != null && rsHandler.canWork()) {
            if (refreshState != RefreshState.NONE) {
                assembly.refreshCraftingState(refreshState == RefreshState.HARD_REFRESH);
                refreshState = RefreshState.NONE;
            }
            if (!world.isRemote) {
                assembly.tick();
            }
            active = assembly.isActive();
        } else {
            active = false;
        }
        if (wasActiveLastTick != active) {
            wasActiveLastTick = active;
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public void serBytes(ByteUtils.Writer data) {
        if (assembly == null) {
            data.writeByte((byte)0);
        } else {
            assembly.serializeToBytes(data);
        }
        rsHandler.serBytes(data);
    }

    @Override
    public void deserBytes(ByteUtils.Reader data) {
        byte flag = data.readByte();
        if (flag != 0) {
            if (assembly == null) {
                return; // the rest of the packet will be unparsable if the assembly part isn't parsed out
            }
            assembly.deserializeFromBytes(data, flag);
        }
        rsHandler.deserBytes(data);
    }

    @Override
    public void serNBT(NBTTagCompound tag) {
        if (assembly != null) {
            assembly.serializeToNbt(tag);
        }
        rsHandler.writeToNbt(tag, "RedstoneControl");
    }

    @Override
    public void deserNBT(NBTTagCompound tag) {
        if (!tag.isEmpty()) {
            if (assembly != null) {
                assembly.deserializeFromNbt(tag);
            } else {
                bufferedAssemblyDeser = tag;
            }
        }
        rsHandler.readFromNbt(tag, "RedstoneControl");
    }

}
