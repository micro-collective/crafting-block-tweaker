package xyz.phanta.cbtweaker.common;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import xyz.phanta.cbtweaker.util.ConfigException;

import javax.annotation.Nullable;
import java.util.Locale;

public enum BlockMaterial {

    DIRT(Material.GROUND, SoundType.GROUND, "shovel"),
    WOOD(Material.WOOD, SoundType.WOOD, "axe"),
    STONE(Material.ROCK, SoundType.STONE, "pickaxe"),
    METAL(Material.IRON, SoundType.METAL, "pickaxe"),
    PLANT(Material.PLANTS, SoundType.PLANT, null),
    CLOTH(Material.CLOTH, SoundType.CLOTH, null),
    SAND(Material.SAND, SoundType.SAND, "shovel"),
    GLASS(Material.REDSTONE_LIGHT, SoundType.GLASS, "pickaxe"),
    INDESTRUCTABLE(Material.IRON, SoundType.METAL, null);

    public static BlockMaterial fromString(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Unknown block material: " + name);
        }
    }

    public final Material material;
    public final SoundType soundType;
    @Nullable
    public final String harvestToolClass;

    BlockMaterial(Material material, SoundType soundType, @Nullable String harvestToolClass) {
        this.material = material;
        this.soundType = soundType;
        this.harvestToolClass = harvestToolClass;
    }

}
