package xyz.phanta.cbtweaker.multiblock;

import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.component.MultiBlockStatusGuiComponent;
import xyz.phanta.cbtweaker.gui.component.RedstoneControlGuiComponent;
import xyz.phanta.cbtweaker.gui.inventory.CbtCustomContainerGui;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.util.block.RedstoneControlHandler;

public class MultiBlockControllerGui extends CbtCustomContainerGui<MultiBlockControllerContainer> {

    public MultiBlockControllerGui(MultiBlockControllerContainer container) {
        super(container);
    }

    @Override
    public void initGui() {
        super.initGui();
        MultiBlockControllerContainer cont = getContainer();
        UiLayout layout = cont.getLayout();
        ScreenRegion machInvRegion = layout.getMachineInventoryRegion();
        addComponent(new MultiBlockStatusGuiComponent(
                machInvRegion.getX() + machInvRegion.getWidth() - 11, machInvRegion.getY() - 11,
                cont.getMultiBlockController()));
        RedstoneControlHandler rsHandler = cont.getMultiBlockController().getRedstoneHandler();
        addComponent(new RedstoneControlGuiComponent(
                machInvRegion.getX() + machInvRegion.getWidth() - 24, machInvRegion.getY() - 11,
                rsHandler::getRedstoneBehaviour, cont::setRedstoneBehaviour));
        cont.getRecipeLogicUiElement().addToGui(this, machInvRegion);
    }

}
