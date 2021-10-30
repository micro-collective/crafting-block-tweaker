package xyz.phanta.cbtweaker.hatch;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import net.minecraft.entity.player.InventoryPlayer;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.gui.inventory.CbtCustomContainer;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;
import xyz.phanta.cbtweaker.util.block.AutoExportHandler;

public class HatchContainer extends CbtCustomContainer {

    private final HatchTileEntity hatch;
    private final UiElement.Indexed bufUiElem;

    public HatchContainer(HatchTileEntity hatch, InventoryPlayer playerInv) {
        super(hatch.getHatchType().getTier(hatch.getTier()).getUiLayout(), playerInv);
        this.hatch = hatch;
        this.bufUiElem = addUiElement(hatch.createUiElement(), getLayout().getMachineInventoryRegion());
    }

    public HatchTileEntity getHatch() {
        return hatch;
    }

    public UiElement.Indexed getBufferUiElement() {
        return bufUiElem;
    }

    @Override
    public String getTranslationKey() {
        return CbtMod.MOD_ID + ".hatch." + hatch.getHatchType().getId() + "." + hatch.getTier() + ".name";
    }

    public void toggleAutoExporting() {
        sendInteraction(new byte[0]);
    }

    @Override
    public void onClientInteraction(ByteUtils.Reader data) {
        AutoExportHandler exportHandler = hatch.getExportHandler();
        exportHandler.setAutoExporting(!exportHandler.isAutoExporting());
    }

}
