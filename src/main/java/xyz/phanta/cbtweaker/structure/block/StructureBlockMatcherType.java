package xyz.phanta.cbtweaker.structure.block;

import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

public interface StructureBlockMatcherType {

    ResourceLocation getId();

    StructureBlockMatcher loadMatcher(JsonObject spec);

}
