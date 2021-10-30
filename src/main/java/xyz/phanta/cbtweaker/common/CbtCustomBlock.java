package xyz.phanta.cbtweaker.common;

import net.minecraft.block.Block;

public class CbtCustomBlock extends Block {

    private final BlockMaterial material;

    public CbtCustomBlock(BlockMaterial material) {
        super(material.material);
        this.material = material;
    }

    public void lateInit() {
        // we have to do this late because some block properties might not exist at construction time because they are
        // set in the subclass constructor. see the note in HatchBlock for details
        setSoundType(material.soundType);
        if (material.harvestToolClass != null) {
            setHarvestLevel(material.harvestToolClass, 0);
        }
        if (material == BlockMaterial.INDESTRUCTABLE) {
            setBlockUnbreakable();
            setResistance(6000000F);
        } else {
            setHardness(3F);
        }
    }

}
