package st.evening.mc.cbtweaker.structure

import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import st.evening.mc.cbtweaker.CbTweaker
import st.evening.mc.cbtweaker.event.CbtRegistrationEvent
import st.evening.mc.cbtweaker.structure.block.StructureBlockMatcherType

class StructureMatcherRegistry {
    private val blockMatcherTable: MutableMap<ResourceLocation, StructureBlockMatcherType> = mutableMapOf()
    private val structureMatcherTable: MutableMap<ResourceLocation, StructureMatcherType> = mutableMapOf()

    fun init() {
        CbTweaker.logger.info("Loading structure block matchers...")
        MinecraftForge.EVENT_BUS.post(CbtRegistrationEvent(this::registerBlockMatcher))
        CbTweaker.logger.info("Loading structure matchers...")
        MinecraftForge.EVENT_BUS.post(CbtRegistrationEvent(this::registerStructureMatcher))
        CbTweaker.logger.info("Finished loading structure matchers.")
    }

    private fun registerBlockMatcher(matcher: StructureBlockMatcherType) {
        val typeId = matcher.id
        blockMatcherTable[typeId]?.let {
            throw IllegalStateException(
                "Structure block matcher ID clash! ID: $typeId, " +
                    "existing: ${it.javaClass.canonicalName}, new: ${matcher.javaClass.canonicalName}"
            )
        }
        blockMatcherTable[typeId] = matcher
        CbTweaker.logger.debug(
            "Registered structure block matcher type {} ({})",
            typeId, matcher.javaClass.canonicalName
        )
    }

    private fun registerStructureMatcher(matcher: StructureMatcherType) {
        val typeId = matcher.id
        structureMatcherTable[typeId]?.let {
            throw IllegalStateException(
                "Structure matcher ID clash! ID: $typeId, " +
                    "existing: ${it.javaClass.canonicalName}, new: ${matcher.javaClass.canonicalName}"
            )
        }
        structureMatcherTable[typeId] = matcher
        CbTweaker.logger.debug(
            "Registered structure matcher type {} ({})",
            typeId, matcher.javaClass.canonicalName
        )
    }

    fun lookUpBlockMatcher(id: ResourceLocation): StructureBlockMatcherType? = blockMatcherTable[id]

    fun lookUpStructureMatcher(id: ResourceLocation): StructureMatcherType? = structureMatcherTable[id]
}
