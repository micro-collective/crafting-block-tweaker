package xyz.phanta.cbtweaker.hatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.helper.JsonUtils9;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.ItemBlockRegistrar;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.BufferTypeRegistry;
import xyz.phanta.cbtweaker.common.BlockMaterial;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HatchManager {

    private static final UiLayout DEFAULT_UI_LAYOUT = new UiLayout(CbtTextureResources.GUI_GENERIC_SMALL,
            UiLayout.DEFAULT_MACHINE_REGION, true, UiLayout.DEFAULT_PLAYER_INV_POS, true);

    private final Path specDir;
    private final Map<String, HatchType<?>> hatchTypeTable = new HashMap<>();
    private final ItemBlockRegistrar registrar;

    public HatchManager(Path specDir, ItemBlockRegistrar registrar) {
        this.specDir = specDir;
        this.registrar = registrar;
    }

    public void loadAll() {
        CbtMod.LOGGER.info("Loading hatch types...");
        try {
            Files.list(specDir).forEach(specFile -> {
                try {
                    if (!Files.isRegularFile(specFile)) {
                        return;
                    }
                    String specFileName = specFile.getFileName().toString();
                    if (!specFileName.endsWith(".json")) {
                        CbtMod.LOGGER.warn("Ignoring non-JSON file in hatch specification directory: {}", specFileName);
                        return;
                    }
                    String hatchId = specFileName.substring(0, specFileName.length() - 5);
                    if (hatchId.isEmpty()) {
                        throw new ConfigException("Bad hatch ID: " + hatchId);
                    }

                    JsonObject specDto = DataLoadUtils.readJsonFile(specFile).getAsJsonObject();
                    BlockMaterial blockMat = specDto.has("material")
                            ? BlockMaterial.fromString(specDto.get("material").getAsString()) : BlockMaterial.METAL;

                    ResourceLocation bufTypeId = new ResourceLocation(specDto.get("type").getAsString());
                    BufferTypeRegistry.Entry<?, ?, ?, ?> bufTypeEntry = CbtMod.PROXY.getBufferTypes().lookUp(bufTypeId);
                    if (bufTypeEntry == null) {
                        throw new ConfigException("Unknown buffer type: " + bufTypeId);
                    }

                    JsonObject archetypeDto = specDto.has("archetype")
                            ? specDto.getAsJsonObject("archetype") : new JsonObject();
                    JsonArray tiersDto = specDto.getAsJsonArray("tiers");
                    if (tiersDto == null || tiersDto.size() == 0) {
                        throw new ConfigException("At least one tier must be defined!");
                    } else if (JsonUtils9.stream(tiersDto).anyMatch(t -> !t.isJsonObject())) {
                        throw new ConfigException("Tier config must be a JSON object!");
                    }

                    HatchType<?> hatchType = constructHatchType(hatchId, blockMat, bufTypeEntry.getBufferType(),
                            JsonUtils9.stream(tiersDto)
                                    .map(t -> JsonUtils9.mergeCloning(archetypeDto, t.getAsJsonObject())));
                    hatchTypeTable.put(hatchId, hatchType);
                    registrar.addBlockRegistration(
                            "hatch_" + hatchId, HatchBlock.construct(hatchType), HatchBlockItem::new);
                    CbtMod.LOGGER.debug("Loaded hatch: {}", hatchId);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to load hatch specification: " + specFile, e);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load hatch types!", e);
        }
        CbtMod.LOGGER.info("Finished loading hatch specifications.");
    }

    private static <B> HatchType<B> constructHatchType(String id, BlockMaterial blockMat, BufferType<B, ?, ?, ?> bufType,
                                                       Stream<JsonObject> tierConfigs) {
        return new HatchType<>(id, blockMat, bufType, tierConfigs
                .map(tierConfig -> new HatchType.TierData<>(
                        bufType.loadBufferFactory(
                                tierConfig.has("buffer") ? tierConfig.getAsJsonObject("buffer") : new JsonObject()),
                        tierConfig.has("ui")
                                ? CbtMod.PROXY.getTemplates().getUiTemplates().resolve(tierConfig.get("ui"))
                                : DEFAULT_UI_LAYOUT))
                .collect(Collectors.toList()));
    }

    @Nullable
    public HatchType<?> lookUp(String id) {
        return hatchTypeTable.get(id);
    }

}
