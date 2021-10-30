package xyz.phanta.cbtweaker.structure;

import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;

public interface StructureMatcherType {

    ResourceLocation getId();

    StructureMatcher loadMatcher(MultiBlockType<?, ?, ?> mbType, JsonObject spec);

}
