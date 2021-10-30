package xyz.phanta.cbtweaker.gui;

import io.github.phantamanta44.libnine.InitMe;
import io.github.phantamanta44.libnine.LibNine;
import io.github.phantamanta44.libnine.gui.GuiIdentity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.hatch.HatchContainer;
import xyz.phanta.cbtweaker.hatch.HatchGui;
import xyz.phanta.cbtweaker.hatch.HatchTileEntity;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerContainer;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerGui;
import xyz.phanta.cbtweaker.multiblock.MultiBlockControllerTileEntity;
import xyz.phanta.cbtweaker.singleblock.SingleBlockMachineContainer;
import xyz.phanta.cbtweaker.singleblock.SingleBlockMachineGui;
import xyz.phanta.cbtweaker.singleblock.SingleBlockMachineTileEntity;

public class CbtGuiIds {

    public static final GuiIdentity<HatchContainer, HatchGui> HATCH = new GuiIdentity<>("hatch", HatchContainer.class);
    public static final GuiIdentity<MultiBlockControllerContainer, MultiBlockControllerGui> MB_CONTROLLER
            = new GuiIdentity<>("mb_ctrl", MultiBlockControllerContainer.class);
    public static final GuiIdentity<SingleBlockMachineContainer, SingleBlockMachineGui> SB_MACHINE
            = new GuiIdentity<>("sb_machine", SingleBlockMachineContainer.class);

    @InitMe(CbtMod.MOD_ID)
    public static void init() {
        LibNine.PROXY.getRegistrar().queueGuiServerReg(HATCH, (player, world, x, y, z) -> {
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof HatchTileEntity)) {
                return null;
            }
            return new HatchContainer((HatchTileEntity)tile, player.inventory);
        });
        LibNine.PROXY.getRegistrar().queueGuiServerReg(MB_CONTROLLER, (player, world, x, y, z) -> {
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof MultiBlockControllerTileEntity)) {
                return null;
            }
            return new MultiBlockControllerContainer((MultiBlockControllerTileEntity)tile, player.inventory);
        });
        LibNine.PROXY.getRegistrar().queueGuiServerReg(SB_MACHINE, (player, world, x, y, z) -> {
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (!(tile instanceof SingleBlockMachineTileEntity)) {
                return null;
            }
            return new SingleBlockMachineContainer((SingleBlockMachineTileEntity)tile, player.inventory);
        });
    }

    @SideOnly(Side.CLIENT)
    @InitMe(value = CbtMod.MOD_ID, sides = Side.CLIENT)
    public static void initClient() {
        LibNine.PROXY.getRegistrar().queueGuiClientReg(HATCH, (cont, player, world, x, y, z) -> new HatchGui(cont));
        LibNine.PROXY.getRegistrar().queueGuiClientReg(MB_CONTROLLER,
                (cont, player, world, x, y, z) -> new MultiBlockControllerGui(cont));
        LibNine.PROXY.getRegistrar().queueGuiClientReg(SB_MACHINE,
                (cont, player, world, x, y, z) -> new SingleBlockMachineGui(cont));
    }

}
