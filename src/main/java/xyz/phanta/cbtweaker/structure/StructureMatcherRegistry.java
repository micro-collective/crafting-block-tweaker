package xyz.phanta.cbtweaker.structure;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.event.CbtRegistrationEvent;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcherType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class StructureMatcherRegistry {

    private final Map<ResourceLocation, StructureBlockMatcherType> blockMatcherTable = new HashMap<>();
    private final Map<ResourceLocation, StructureMatcherType> structureMatcherTable = new HashMap<>();

    public void init() {
        CbtMod.LOGGER.info("Loading structure block matchers...");
        MinecraftForge.EVENT_BUS.post(
                new CbtRegistrationEvent<>(StructureBlockMatcherType.class, this::registerBlockMatcher));
        CbtMod.LOGGER.info("Loading structure matchers...");
        MinecraftForge.EVENT_BUS.post(
                new CbtRegistrationEvent<>(StructureMatcherType.class, this::registerStructureMatcher));
        CbtMod.LOGGER.info("Finished loading structure matchers.");
    }

    private void registerBlockMatcher(StructureBlockMatcherType matcher) {
        ResourceLocation typeId = matcher.getId();
        StructureBlockMatcherType clashing = blockMatcherTable.get(typeId);
        if (clashing != null) {
            throw new IllegalArgumentException(
                    String.format("Structure block matcher ID clash! ID: %s, existing: %s, new: %s",
                            typeId, clashing.getClass().getCanonicalName(), matcher.getClass().getCanonicalName()));
        }
        blockMatcherTable.put(typeId, matcher);
        CbtMod.LOGGER.debug("Registered structure block matcher type {} ({})",
                typeId, matcher.getClass().getCanonicalName());
    }

    private void registerStructureMatcher(StructureMatcherType matcher) {
        ResourceLocation typeId = matcher.getId();
        StructureMatcherType clashing = structureMatcherTable.get(typeId);
        if (clashing != null) {
            throw new IllegalArgumentException(
                    String.format("Structure matcher ID clash! ID: %s, existing: %s, new: %s",
                            typeId, clashing.getClass().getCanonicalName(), matcher.getClass().getCanonicalName()));
        }
        structureMatcherTable.put(typeId, matcher);
        CbtMod.LOGGER.debug("Registered structure matcher type {} ({})",
                typeId, matcher.getClass().getCanonicalName());
    }

    @Nullable
    public StructureBlockMatcherType lookUpBlockMatcher(ResourceLocation id) {
        return blockMatcherTable.get(id);
    }

    @Nullable
    public StructureMatcherType lookUpStructureMatcher(ResourceLocation id) {
        return structureMatcherTable.get(id);
    }

}
