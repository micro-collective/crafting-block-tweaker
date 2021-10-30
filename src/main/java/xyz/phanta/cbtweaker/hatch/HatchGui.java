package xyz.phanta.cbtweaker.hatch;

import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.component.AutoExportGuiComponent;
import xyz.phanta.cbtweaker.gui.inventory.CbtCustomContainerGui;
import xyz.phanta.cbtweaker.util.block.AutoExportHandler;

public class HatchGui extends CbtCustomContainerGui<HatchContainer> {

    public HatchGui(HatchContainer container) {
        super(container);
    }

    @Override
    public void initGui() {
        super.initGui();
        HatchContainer cont = getContainer();
        ScreenRegion machInvRegion = cont.getLayout().getMachineInventoryRegion();
        AutoExportHandler exportHandler = cont.getHatch().getExportHandler();
        if (exportHandler.isAutoExportSupported()) {
            addComponent(new AutoExportGuiComponent(
                    machInvRegion.getX() + machInvRegion.getWidth() - 11, machInvRegion.getY() - 11,
                    exportHandler::isAutoExporting, cont::toggleAutoExporting));
        }
        cont.getBufferUiElement().addToGui(this, machInvRegion);
    }

}
