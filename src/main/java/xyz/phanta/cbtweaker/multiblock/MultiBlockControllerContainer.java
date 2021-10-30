package xyz.phanta.cbtweaker.multiblock;

import io.github.phantamanta44.libnine.util.data.ByteUtils;
import io.github.phantamanta44.libnine.util.world.RedstoneBehaviour;
import net.minecraft.entity.player.InventoryPlayer;
import xyz.phanta.cbtweaker.gui.inventory.CbtCustomContainer;
import xyz.phanta.cbtweaker.gui.inventory.UiElement;

public class MultiBlockControllerContainer extends CbtCustomContainer {

    private final MultiBlockControllerTileEntity mbCtrl;
    private final UiElement.Indexed logicUiElem;

    public MultiBlockControllerContainer(MultiBlockControllerTileEntity mbCtrl, InventoryPlayer playerInv) {
        super(mbCtrl.getMultiBlockType().getUiLayout(), playerInv);
        this.mbCtrl = mbCtrl;
        this.logicUiElem = addUiElement(mbCtrl.createRecipeLogicUiElement(), getLayout().getMachineInventoryRegion());
    }

    public MultiBlockControllerTileEntity getMultiBlockController() {
        return mbCtrl;
    }

    public UiElement.Indexed getRecipeLogicUiElement() {
        return logicUiElem;
    }

    @Override
    public String getTranslationKey() {
        return mbCtrl.getMultiBlockType().getTranslationKey();
    }

    public void setRedstoneBehaviour(RedstoneBehaviour behaviour) {
        sendInteraction(new byte[] { (byte)behaviour.ordinal() });
    }

    @Override
    public void onClientInteraction(ByteUtils.Reader data) {
        int behaviourOrd = data.readByte();
        if (behaviourOrd < 0 || behaviourOrd >= RedstoneBehaviour.VALUES.size()) {
            return;
        }
        mbCtrl.getRedstoneHandler().setRedstoneBehaviour(RedstoneBehaviour.VALUES.get(behaviourOrd));
    }

}
