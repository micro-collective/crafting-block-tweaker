package xyz.phanta.cbtweaker.util.helper;

import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.phantamanta44.libnine.util.ImpossibilityRealizedException;
import io.github.phantamanta44.libnine.util.gameobject.BlockIdentity;
import io.github.phantamanta44.libnine.util.gameobject.FluidIdentity;
import io.github.phantamanta44.libnine.util.gameobject.ItemIdentity;
import io.github.phantamanta44.libnine.util.helper.JsonUtils9;
import io.github.phantamanta44.libnine.util.render.DrawOrientation;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import io.github.phantamanta44.libnine.util.render.TextureResource;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.recipe.RecipeLogic;
import xyz.phanta.cbtweaker.util.ConfigException;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataLoadUtils {

    private static final Pattern PATTERN_INT_LITERAL = Pattern.compile("\\d+[bsil]");
    private static final Pattern PATTERN_FLOAT_LITERAL = Pattern.compile("\\d+(?:\\.\\d*)?[fd]");

    public static JsonElement readJsonFile(Path jsonFile) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(jsonFile)) {
            return JsonUtils9.PARSER.parse(reader);
        }
    }

    public static ItemIdentity loadItemIdentity(JsonObject dto) {
        String itemId = dto.get("item").getAsString();
        if (itemId == null) {
            throw new ConfigException("Item identity must define an item!");
        }
        Item item = Item.getByNameOrId(itemId);
        if (item == null) {
            throw new ConfigException("Unknown item: " + itemId);
        }
        return new ItemIdentity(item, dto.has("meta") ? dto.get("meta").getAsInt() : OreDictionary.WILDCARD_VALUE,
                dto.has("data") ? DataLoadUtils.loadNbtCompound(dto.get("data").getAsJsonObject()) : null);
    }

    public static BlockIdentity loadBlockIdentity(JsonObject dto) {
        String blockId = dto.get("block").getAsString();
        if (blockId == null) {
            throw new ConfigException("Block identity must define a block!");
        }
        Block block = Block.getBlockFromName(blockId);
        if (block == null) {
            throw new ConfigException("Unknown block: " + blockId);
        }
        if (dto.has("properties")) {
            IBlockState state = block.getDefaultState();
            BlockStateContainer stateCont = block.getBlockState();
            for (Map.Entry<String, JsonElement> entry : dto.getAsJsonObject("properties").entrySet()) {
                IProperty<?> prop = stateCont.getProperty(entry.getKey());
                if (prop == null) {
                    throw new ConfigException(String.format("Block %s has no such property: %s",
                            block.getRegistryName(), entry.getKey()));
                }
                state = withPropertyFromString(state, prop, entry.getValue().getAsString());
            }
            return new BlockIdentity(block, block.getMetaFromState(state));
        } else if (dto.has("meta")) {
            return new BlockIdentity(block, dto.get("meta").getAsInt());
        }
        return new BlockIdentity(block);
    }

    @SuppressWarnings("Guava")
    private static <T extends Comparable<T>> IBlockState withPropertyFromString(IBlockState state,
                                                                                IProperty<T> prop, String valStr) {
        Optional<T> parsedVal = prop.parseValue(valStr);
        if (!parsedVal.isPresent()) {
            throw new ConfigException(String.format("Block %s property %s cannot have value: %s",
                    state.getBlock().getRegistryName(), prop.getName(), valStr));
        }
        return state.withProperty(prop, parsedVal.get());
    }

    public static FluidIdentity loadFluidIdentity(JsonObject dto) {
        String fluidName = dto.get("fluid").getAsString();
        if (fluidName == null) {
            throw new ConfigException("Fluid identity must define an fluid!");
        }
        return new FluidIdentity(fluidName,
                dto.has("data") ? DataLoadUtils.loadNbtCompound(dto.get("data").getAsJsonObject()) : null);
    }

    public static TextureRegion loadTextureRegion(JsonObject spec) {
        TextureResource texRes = new TextureResource(new ResourceLocation(spec.get("texture").getAsString() + ".png"),
                spec.get("width").getAsInt(), spec.get("height").getAsInt());
        if (!spec.has("region_x")) {
            return texRes.asRegion();
        }
        return texRes.getRegion(spec.get("region_x").getAsInt(), spec.get("region_y").getAsInt(),
                spec.get("region_width").getAsInt(), spec.get("region_height").getAsInt());
    }

    public static DrawOrientation loadDrawOrientation(String name) {
        try {
            return DrawOrientation.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Unknown draw orientation: " + name);
        }
    }

    public static RecipeLogic<?, ?, ?, ?, ?> loadRecipeLogic(JsonObject spec) {
        ResourceLocation recipeLogicId = new ResourceLocation(spec.get("type").getAsString());
        RecipeLogic<?, ?, ?, ?, ?> recipeLogic = CbtMod.PROXY.getRecipeLogics().lookUp(recipeLogicId);
        if (recipeLogic == null) {
            throw new ConfigException("Unknown recipe logic: " + recipeLogicId);
        }
        return recipeLogic;
    }

    public static <R, C, D> void loadRecipes(String machineId, Path recipeDir, String ignoreName,
                                             RecipeLogic<R, C, D, ?, ?> recipeLogic,
                                             C recipeConfig, D recipeDb) throws Exception {
        Files.list(recipeDir).forEach(recipeFile -> {
            try {
                if (!Files.isRegularFile(recipeFile)) {
                    return;
                }
                String recipeFileName = recipeFile.getFileName().toString();
                if (!recipeFileName.endsWith(".json")) {
                    CbtMod.LOGGER.warn("Ignoring non-JSON file in recipe directory: {}/{}", machineId, recipeFileName);
                    return;
                }
                if (recipeFileName.equals(ignoreName)) {
                    return;
                }
                String recipeId = recipeFileName.substring(0, recipeFileName.length() - 5);
                if (recipeId.isEmpty()) {
                    throw new ConfigException("Bad recipe ID: " + recipeId);
                }

                JsonObject recipeDto = readJsonFile(recipeFile).getAsJsonObject();
                recipeLogic.loadRecipe(recipeId, recipeDto, recipeConfig, recipeDb);
                CbtMod.LOGGER.debug("Loaded recipe: {}/{}", machineId, recipeId);
            } catch (Exception e) {
                CbtMod.LOGGER.warn("Could not load recipe file: " + recipeFile, e);
            }
        });
    }

    public static NBTBase loadNbt(JsonElement dto) {
        if (dto.isJsonObject()) {
            return loadNbtCompound(dto.getAsJsonObject());
        } else if (dto.isJsonArray()) {
            return loadNbtList(dto.getAsJsonArray());
        }
        JsonPrimitive primDto = dto.getAsJsonPrimitive();
        if (primDto.isBoolean()) {
            return new NBTTagByte(primDto.getAsBoolean() ? (byte)1 : (byte)0);
        } else if (primDto.isNumber()) {
            return new NBTTagInt(primDto.getAsInt()); // assume all non-tagged numbers are ints
        } else {
            String str = primDto.getAsString();
            Matcher matcher = PATTERN_INT_LITERAL.matcher(str);
            if (matcher.matches()) {
                switch (str.charAt(str.length() - 1)) {
                    case 'b':
                        return new NBTTagByte(Byte.parseByte(str));
                    case 's':
                        return new NBTTagShort(Short.parseShort(str));
                    case 'i':
                        return new NBTTagInt(Integer.parseInt(str));
                    case 'l':
                        return new NBTTagLong(Long.parseLong(str));
                }
                throw new ImpossibilityRealizedException();
            }
            matcher = PATTERN_FLOAT_LITERAL.matcher(str);
            if (matcher.matches()) {
                switch (str.charAt(str.length() - 1)) {
                    case 'f':
                        return new NBTTagFloat(Float.parseFloat(str));
                    case 'd':
                        return new NBTTagDouble(Double.parseDouble(str));
                }
                throw new ImpossibilityRealizedException();
            }
            return new NBTTagString(str);
        } // TODO parse nbt array types?
    }

    public static NBTTagCompound loadNbtCompound(JsonObject dto) {
        NBTTagCompound tag = new NBTTagCompound();
        for (Map.Entry<String, JsonElement> entry : dto.entrySet()) {
            tag.setTag(entry.getKey(), loadNbt(entry.getValue()));
        }
        return tag;
    }

    public static NBTTagList loadNbtList(JsonArray dto) {
        NBTTagList tag = new NBTTagList();
        byte tagType = 0;
        for (JsonElement elemDto : dto) {
            NBTBase elemTag = loadNbt(elemDto);
            if (tagType == 0) {
                tagType = elemTag.getId();
            } else if (tagType != elemTag.getId()) {
                throw new IllegalStateException("NBT lists cannot have multiple entry types: " + dto);
            }
            tag.appendTag(elemTag);
        }
        return tag;
    }

}
