package xyz.phanta.cbtweaker.gui.component;

import io.github.phantamanta44.libnine.client.gui.component.GuiComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.gui.CbtTextureResources;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerBlock;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerTileEntity;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.util.Direction;

import java.util.Arrays;
import java.util.Collection;

public class MultiBlockStatusGuiComponent extends GuiComponent {

    private final MultiBlockControllerTileEntity mbCtrl;

    public MultiBlockStatusGuiComponent(int x, int y, MultiBlockControllerTileEntity mbCtrl) {
        super(x, y, CbtTextureResources.INFO_DISPLAY_OFF.getWidth(), CbtTextureResources.INFO_DISPLAY_OFF.getHeight());
        this.mbCtrl = mbCtrl;
    }

    @Override
    public void render(float partialTicks, int mX, int mY, boolean mouseOver) {
        if (mbCtrl.isAssembled()) {
            CbtTextureResources.INFO_DISPLAY_ON.draw(x, y);
        } else {
            CbtTextureResources.INFO_DISPLAY_OFF.draw(x, y);
        }
    }

    @Override
    public void renderTooltip(float partialTicks, int mX, int mY) {
        if (!mbCtrl.isAssembled()) {
            drawTooltip(Arrays.asList(
                            TextFormatting.RED + I18n.format(CbtLang.TOOLTIP_MB_NOT_ASSEMBLED),
                            TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_MB_VISUALIZE)),
                    mX, mY);
        } else {
            // TODO display some multiblock stats
            drawTooltip(Arrays.asList(
                            TextFormatting.GREEN + I18n.format(CbtLang.TOOLTIP_MB_ASSEMBLED),
                            TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_MB_VISUALIZE)),
                    mX, mY);
        }
    }

    @Override
    public boolean onClick(int mX, int mY, int button, boolean mouseOver) {
        if (!mouseOver || button != 0) {
            return false;
        }
        World world = mbCtrl.getWorld();
        BlockPos pos = mbCtrl.getPos();
        Direction dir = world.getBlockState(pos).getValue(MultiBlockControllerBlock.PROP_DIRECTION);
        Collection<Pair<Vec3i, StructureBlockMatcher>> visBlocks
                = mbCtrl.getMultiBlockType().getStructureMatcher().getVisualization();
        ParticleManager particleManager = Minecraft.getMinecraft().effectRenderer;
        for (Pair<Vec3i, StructureBlockMatcher> visBlock : visBlocks) {
            BlockPos structBlockPos = pos.add(dir.transform(visBlock.getLeft(), false));
            Particle particle = particleManager.spawnEffectParticle(EnumParticleTypes.DRAGON_BREATH.getParticleID(),
                    structBlockPos.getX() + 0.5D, structBlockPos.getY() + 0.5D, structBlockPos.getZ() + 0.5D,
                    0D, 0D, 0D);
            if (particle != null) {
                particle.setMaxAge(200);
                particle.multipleParticleScaleBy(1.5F);
            }
        }
        playClickSound();
        return true;
    }

}
