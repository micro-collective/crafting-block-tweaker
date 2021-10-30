package xyz.phanta.cbtweaker.structure;

import io.github.phantamanta44.libnine.util.helper.ItemUtils;
import io.github.phantamanta44.libnine.util.nbt.NBTUtils;
import io.github.phantamanta44.libnine.util.render.RenderUtils;
import io.github.phantamanta44.libnine.util.world.WorldBlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.IRarity;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import xyz.phanta.cbtweaker.CbtLang;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.integration.jei.CbtJeiPlugin;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerBlock;
import xyz.phanta.cbtweaker.multiblock.MultiBlockType;
import xyz.phanta.cbtweaker.structure.block.StructureBlockMatcher;
import xyz.phanta.cbtweaker.structure.block.StructureBlockVisualization;
import xyz.phanta.cbtweaker.util.Direction;
import xyz.phanta.cbtweaker.util.DummyBlockAccessor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = CbtMod.MOD_ID, value = Side.CLIENT)
public class VisualizationToolItem extends Item {

    @GameRegistry.ObjectHolder(CbtMod.MOD_ID + ":vis_tool")
    public static VisualizationToolItem ITEM;

    public VisualizationToolItem() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote) {
            ClientActions.openGui();
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    private static class ClientActions { // prevent server from loading GUI classes

        public static void openGui() {
            Minecraft.getMinecraft().displayGuiScreen(new VisualizationGui());
        }

    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            ItemStack stack = player.getHeldItem(hand);
            setBoundPos(stack, new WorldBlockPos(world, pos));
            setLevel(stack, null);
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    @Override
    public IRarity getForgeRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag tooltipFlags) {
        tooltip.add(TextFormatting.GRAY + I18n.format(CbtLang.TOOLTIP_VIS_TOOL));
        tooltip.add("");
        WorldBlockPos boundPos = getBoundPos(stack);
        if (boundPos == null) {
            tooltip.add(TextFormatting.RED + I18n.format(CbtLang.TOOLTIP_BIND_TO_BLOCK));
        } else {
            tooltip.add(String.format(TextFormatting.GOLD + "(%d, %d, %d)",
                    boundPos.getX(), boundPos.getY(), boundPos.getZ()));
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        VisualizationInfo visInfo = getVisualizationInfo(player);
        if (visInfo == null) {
            return;
        }
        BlockPos ctrlPos = visInfo.getControllerPosition();
        MultiBlockType<?, ?, ?> mbType = visInfo.getMultiBlockType();
        Direction direction = visInfo.getDirection();
        Integer level = getLevel(visInfo.getStack());

        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.pushMatrix();
        Vec3d interpPos = RenderUtils.getInterpPos(player, event.getPartialTicks());
        GlStateManager.translate(-interpPos.x, -interpPos.y, -interpPos.z);

        DummyBlockAccessor.MapBacked visWorld = new DummyBlockAccessor.MapBacked();
        for (Pair<Vec3i, StructureBlockMatcher> entry : mbType.getStructureMatcher().getVisualization()) {
            Vec3i offset = entry.getLeft();
            if (level != null && offset.getY() != level) {
                continue;
            }
            StructureBlockMatcher matcher = entry.getRight();
            BlockPos pos = ctrlPos.add(direction.transform(offset, false));
            if (!player.world.isAirBlock(pos)) {
                if (matcher.matchBlock(player.world, pos, direction) == null) {
                    GlStateManager.color(1F, 0F, 0F, 0.3F);
                    VisualizationRenderer.drawCube(pos);
                }
                continue;
            }
            StructureBlockVisualization vis = CbtJeiPlugin.getIngredientByGlobalIndex(matcher.getVisualization());
            if (vis != null) {
                visWorld.setBlockState(pos, vis.getBlockState());
            }
        }
        GlStateManager.color(1F, 1F, 1F, 1F);

        Minecraft mc = Minecraft.getMinecraft();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder vertexBuf = tess.getBuffer();
        BlockRendererDispatcher blockRenderer = mc.getBlockRendererDispatcher();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        for (BlockRenderLayer renderLayer : BlockRenderLayer.values()) {
            ForgeHooksClient.setRenderLayer(renderLayer);
            vertexBuf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            for (Map.Entry<BlockPos, IBlockState> entry : visWorld.getEntries()) {
                IBlockState state = entry.getValue();
                if (!state.getBlock().canRenderInLayer(state, renderLayer)) {
                    continue;
                }
                BlockPos pos = entry.getKey();
                GlStateManager.pushMatrix();
                GlStateManager.translate(pos.getX(), pos.getY(), pos.getZ());
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(
                        GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA);
                GL14.glBlendColor(1F, 1F, 1F, 0.3F);
                GlStateManager.depthMask(false);
                blockRenderer.renderBlock(state, pos, visWorld, vertexBuf);
                GlStateManager.popMatrix();
            }
            tess.draw();
        }
        ForgeHooksClient.setRenderLayer(null);
        GL14.glBlendColor(1F, 1F, 1F, 1F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }

    @Nullable
    public static VisualizationInfo getVisualizationInfo(EntityPlayer player) {
        EnumHand hand;
        ItemStack stack = player.getHeldItemMainhand();
        if (stack.getItem() == ITEM) {
            hand = EnumHand.MAIN_HAND;
        } else {
            stack = player.getHeldItemOffhand();
            if (stack.getItem() == ITEM) {
                hand = EnumHand.OFF_HAND;
            } else {
                return null;
            }
        }
        WorldBlockPos wbp = getBoundPos(stack);
        if (wbp == null || player.world != wbp.getWorld()) {
            return null;
        }
        World world = wbp.getWorld();
        BlockPos ctrlPos = wbp.getPos();
        IBlockState ctrlState = world.getBlockState(ctrlPos);
        Block ctrlBlock = ctrlState.getBlock();
        if (!(ctrlBlock instanceof MultiBlockControllerBlock)) {
            return null;
        }
        MultiBlockType<?, ?, ?> mbType = ((MultiBlockControllerBlock)ctrlBlock).getMultiBlockType();
        Direction direction = ctrlState.getValue(MultiBlockControllerBlock.PROP_DIRECTION);
        return new VisualizationInfo(stack, hand, ctrlPos, mbType, direction);
    }

    @Nullable
    public static WorldBlockPos getBoundPos(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = Objects.requireNonNull(stack.getTagCompound());
        if (!tag.hasKey("BoundPos", Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        return NBTUtils.deserializeWorldBlockPos(tag.getCompoundTag("BoundPos"));
    }

    public static void setBoundPos(ItemStack stack, WorldBlockPos pos) {
        ItemUtils.getOrCreateTag(stack).setTag("BoundPos", NBTUtils.serializeWorldBlockPos(pos));
    }

    @Nullable
    public static Integer getLevel(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = Objects.requireNonNull(stack.getTagCompound());
        if (!tag.hasKey("Level", Constants.NBT.TAG_SHORT)) {
            return null;
        }
        return (int)tag.getShort("Level");
    }

    public static void setLevel(ItemStack stack, @Nullable Integer level) {
        if (level == null) {
            if (stack.hasTagCompound()) {
                Objects.requireNonNull(stack.getTagCompound()).removeTag("Level");
            }
        } else {
            ItemUtils.getOrCreateTag(stack).setShort("Level", level.shortValue());
        }
    }

    public static class VisualizationInfo {

        private final ItemStack stack;
        private final EnumHand hand;
        private final BlockPos ctrlPos;
        private final MultiBlockType<?, ?, ?> mbType;
        private final Direction direction;

        private VisualizationInfo(ItemStack stack, EnumHand hand,
                                  BlockPos ctrlPos, MultiBlockType<?, ?, ?> mbType, Direction direction) {
            this.stack = stack;
            this.hand = hand;
            this.ctrlPos = ctrlPos;
            this.mbType = mbType;
            this.direction = direction;
        }

        public ItemStack getStack() {
            return stack;
        }

        public EnumHand getHand() {
            return hand;
        }

        public BlockPos getControllerPosition() {
            return ctrlPos;
        }

        public MultiBlockType<?, ?, ?> getMultiBlockType() {
            return mbType;
        }

        public Direction getDirection() {
            return direction;
        }

    }

}
