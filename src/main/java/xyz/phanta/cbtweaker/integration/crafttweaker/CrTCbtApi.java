package xyz.phanta.cbtweaker.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;
import xyz.phanta.cbtweaker.singleblock.SingleBlockType;

import javax.annotation.Nullable;

@ZenRegister
@ZenClass("mods.cbtweaker.CbtApi")
public class CrTCbtApi {

    @Nullable
    @ZenMethod
    public static CrTCraftingBlock<?, ?, ?> getMultiBlock(String id) {
        MultiBlockType<?, ?, ?> mbType = CbtMod.PROXY.getMultiBlocks().lookUp(id);
        return mbType != null ? new CrTCraftingBlock<>(mbType) : null;
    }

    @Nullable
    @ZenMethod
    public static CrTCraftingBlock<?, ?, ?> getSingleBlock(String id) {
        SingleBlockType<?, ?, ?> sbType = CbtMod.PROXY.getSingleBlocks().lookUp(id);
        return sbType != null ? new CrTCraftingBlock<>(sbType) : null;
    }

}
