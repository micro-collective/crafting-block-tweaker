package xyz.phanta.cbtweaker.singleblock;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.world.BlockSide;
import io.github.phantamanta44.libnine.util.world.RedstoneBehaviour;
import net.minecraft.entity.player.InventoryPlayer;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.inventory.CbtCustomContainer;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.util.block.AutoExportHandler;
import xyz.phanta.cbtweaker.util.block.MachineSideHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class SingleBlockMachineContainer extends CbtCustomContainer {

    private final SingleBlockMachineTileEntity sbMachine;
    private final List<BufferUiElement> bufUiElems;
    private final UiElement.Indexed logicUiElem;

    public SingleBlockMachineContainer(SingleBlockMachineTileEntity sbMachine, InventoryPlayer playerInv) {
        super(sbMachine.getSingleBlockType().getUiLayout(), playerInv);
        this.sbMachine = sbMachine;
        ScreenRegion machInvRegion = getLayout().getMachineInventoryRegion();
        MachineSideHandler sideHandler = sbMachine.getSideHandler();
        this.bufUiElems = sbMachine.createBufferUiElements().stream()
                .map(e -> new BufferUiElement(
                        addUiElement(e.getRight(), machInvRegion), sideHandler.getSideConfig(e.getLeft())))
                .collect(Collectors.toList());
        this.logicUiElem = addUiElement(sbMachine.createRecipeLogicUiElement(), machInvRegion);
    }

    public SingleBlockMachineTileEntity getSingleBlockMachine() {
        return sbMachine;
    }

    public List<BufferUiElement> getBufferUiElements() {
        return bufUiElems;
    }

    public UiElement.Indexed getRecipeLogicUiElement() {
        return logicUiElem;
    }

    @Override
    public String getTranslationKey() {
        return sbMachine.getSingleBlockType().getTranslationKey();
    }

    public void setRedstoneBehaviour(RedstoneBehaviour behaviour) {
        sendInteraction(new byte[] { (byte)0, (byte)behaviour.ordinal() });
    }

    public void toggleBufferSide(BlockSide side, int index) {
        sendInteraction(new byte[] { (byte)1, (byte)index, (byte)side.ordinal() });
    }

    public void toggleAutoExporting(int index) {
        sendInteraction(new byte[] { (byte)2, (byte)index });
    }

    @Override
    public void onClientInteraction(ByteUtils.Reader data) {
        switch (data.readByte()) {
            case 0: {
                int behaviourNdx = data.readByte();
                if (behaviourNdx < 0 || behaviourNdx >= RedstoneBehaviour.VALUES.size()) {
                    return;
                }
                sbMachine.getRedstoneHandler().setRedstoneBehaviour(RedstoneBehaviour.VALUES.get(behaviourNdx));
                break;
            }
            case 1: {
                int index = data.readByte();
                if (index < 0 || index >= bufUiElems.size()) {
                    return;
                }
                int sideNdx = data.readByte();
                if (sideNdx < 0 || sideNdx >= BlockSide.VALUES.size()) {
                    return;
                }
                MachineSideHandler.SideConfig<?> sideConfig = bufUiElems.get(index).getSideConfig();
                if (sideConfig != null) {
                    sideConfig.toggle(BlockSide.VALUES.get(sideNdx));
                }
                break;
            }
            case 2: {
                int index = data.readByte();
                if (index < 0 || index >= bufUiElems.size()) {
                    return;
                }
                MachineSideHandler.SideConfig<?> sideConfig = bufUiElems.get(index).getSideConfig();
                if (sideConfig != null) {
                    AutoExportHandler exportHandler = sideConfig.getExportHandler();
                    exportHandler.setAutoExporting(!exportHandler.isAutoExporting());
                }
                break;
            }
        }
    }

    public static class BufferUiElement {

        private final UiElement.Indexed uiElement;
        @Nullable
        private final MachineSideHandler.SideConfig<?> sideConfig;

        public BufferUiElement(UiElement.Indexed uiElement, @Nullable MachineSideHandler.SideConfig<?> sideConfig) {
            this.uiElement = uiElement;
            this.sideConfig = sideConfig;
        }

        public UiElement.Indexed getUiElement() {
            return uiElement;
        }

        @Nullable
        public MachineSideHandler.SideConfig<?> getSideConfig() {
            return sideConfig;
        }

    }

}
