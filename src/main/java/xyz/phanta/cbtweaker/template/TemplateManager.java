package xyz.phanta.cbtweaker.template;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.helper.JsonUtils9;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcherType;
import xyz.phanta.cbtweaker.util.ConfigException;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateManager {

    private final TemplateRegistry<UiLayout> uiTemplates;
    private final TemplateRegistry<List<StructureBlockMatcher>> structureBlockMatcherTemplates;

    public TemplateManager(Path templateDir) {
        uiTemplates = new TemplateRegistry<>(
                "UI", templateDir.resolve("ui.json"), dto -> UiLayout.fromJson(dto.getAsJsonObject()));
        structureBlockMatcherTemplates = new TemplateRegistry<>(
                "structure block matcher", templateDir.resolve("structure_block_matcher.json"), dto -> {
            if (dto.isJsonArray()) {
                return JsonUtils9.stream(dto.getAsJsonArray())
                        .map(specDto -> resolveStructureBlockMatcher(specDto.getAsJsonObject()))
                        .collect(Collectors.toList());
            } else {
                return Collections.singletonList(resolveStructureBlockMatcher(dto.getAsJsonObject()));
            }
        });
    }

    private static StructureBlockMatcher resolveStructureBlockMatcher(JsonObject specDto) {
        ResourceLocation typeId = new ResourceLocation(specDto.get("type").getAsString());
        StructureBlockMatcherType type = CbtMod.PROXY.getStructureMatchers().lookUpBlockMatcher(typeId);
        if (type == null) {
            throw new ConfigException("Unknown structure block matcher type: " + typeId);
        }
        return type.loadMatcher(specDto);
    }

    public TemplateRegistry<UiLayout> getUiTemplates() {
        return uiTemplates;
    }

    public TemplateRegistry<List<StructureBlockMatcher>> getStructureBlockMatcherTemplates() {
        return structureBlockMatcherTemplates;
    }

    public void loadPreInit() {
        uiTemplates.loadTemplates();
    }

    public void loadInit() {
        structureBlockMatcherTemplates.loadTemplates();
    }

}
