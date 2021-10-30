package xyz.phanta.cbtweaker.multiblock;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.common.MachineRecipeHandler;
import xyz.phanta.cbtweaker.common.MachineRecipeHost;
import xyz.phanta.cbtweaker.hatch.HatchTileEntity;
import xyz.phanta.cbtweaker.recipe.ComponentSet;
import xyz.phanta.cbtweaker.recipe.RecipeExecutor;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.structure.StructureMatch;

import java.util.*;

public class MultiBlockAssembly<R, C, D, S, E> implements MachineRecipeHost<R, C, D, S, E> {

    public static <R, C, D, S, E> MultiBlockAssembly<R, C, D, S, E> fromStructure(MultiBlockData<R, C, D, S, E> mbData,
                                                                                  StructureMatch structMatch) {
        List<HatchTileEntity> hatches = new ArrayList<>();
        Map<String, BufferGroup> bufGroups = new HashMap<>();
        for (Map.Entry<String, List<HatchTileEntity>> entry : structMatch.getHatches().entrySet()) {
            BufferGroup group = new BufferGroup();
            for (HatchTileEntity hatch : entry.getValue()) {
                hatches.add(hatch);
                hatch.addToGroup(group);
            }
            bufGroups.put(entry.getKey(), group);
        }
        return new MultiBlockAssembly<>(
                mbData, structMatch.getPositions(), structMatch.getComponents(), hatches, bufGroups);
    }

    private final MultiBlockData<R, C, D, S, E> mbData;
    private final Set<BlockPos> structureBlocks;
    private final List<HatchTileEntity> hatches;
    private final MachineRecipeHandler<R, C, D, S, E> recipeHandler;

    public MultiBlockAssembly(MultiBlockData<R, C, D, S, E> mbData,
                              Set<BlockPos> structureBlocks, ComponentSet baseComponents,
                              List<HatchTileEntity> hatches, Map<String, BufferGroup> bufGroups) {
        this.mbData = mbData;
        this.structureBlocks = structureBlocks;
        this.hatches = hatches;
        this.recipeHandler = new MachineRecipeHandler<>(this, baseComponents, bufGroups);
    }

    public Set<BlockPos> getStructureBlocks() {
        return structureBlocks;
    }

    public RecipeExecutor<R, E> getRecipeExecutor() {
        return recipeHandler;
    }

    @Override
    public RecipeLogic<R, C, D, S, E> getRecipeLogic() {
        return mbData.getRecipeLogic();
    }

    @Override
    public C getRecipeConfig() {
        return mbData.getMultiBlockType().getRecipeConfig();
    }

    @Override
    public D getRecipeDatabase() {
        return mbData.getMultiBlockType().getRecipeDatabase();
    }

    public boolean isActive() {
        return recipeHandler.getCurrentRecipe() != null;
    }

    public void associateHatches(MultiBlockControllerTileEntity mbCtrl) {
        for (HatchTileEntity hatch : hatches) {
            hatch.associate(mbCtrl);
        }
    }

    public void disassociateHatches(MultiBlockControllerTileEntity mbCtrl) {
        for (HatchTileEntity hatch : hatches) {
            hatch.disassociate(mbCtrl);
        }
    }

    @Override
    public void setDirty() {
        mbData.markControllerStateDirty();
    }

    public void refreshCraftingState(boolean refreshComponents) {
        recipeHandler.refreshState(refreshComponents);
    }

    public void tick() {
        recipeHandler.tick();
    }

    public void serializeToBytes(ByteUtils.Writer data) {
        recipeHandler.serializeToBytes(data);
    }

    public void deserializeFromBytes(ByteUtils.Reader data, byte activeFlag) {
        recipeHandler.deserializeFromBytes(data, activeFlag);
    }

    public void serializeToNbt(NBTTagCompound tag) {
        recipeHandler.serializeToNbt(tag);
    }

    public void deserializeFromNbt(NBTTagCompound tag) {
        recipeHandler.deserializeFromNbt(tag);
    }

}
