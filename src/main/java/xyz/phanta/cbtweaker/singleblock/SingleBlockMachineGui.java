package xyz.phanta.cbtweaker.singleblock;

import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;
import xyz.phanta.cbtweaker.gui.ScreenRegion;
import xyz.phanta.cbtweaker.gui.component.RedstoneControlGuiComponent;
import xyz.phanta.cbtweaker.gui.inventory.CbtCustomContainerGui;
import xyz.phanta.cbtweaker.gui.inventory.UiLayout;
import xyz.phanta.cbtweaker.util.block.MachineSideHandler;
import xyz.phanta.cbtweaker.util.block.RedstoneControlHandler;
import xyz.phanta.cbtweaker.util.block.SideConfigurationUiHandler;

import java.io.IOException;
import java.util.List;

public class SingleBlockMachineGui extends CbtCustomContainerGui<SingleBlockMachineContainer> {

    private final SideConfigurationUiHandler sideConfigHandler;

    private boolean restrictedRendering = false;

    public SingleBlockMachineGui(SingleBlockMachineContainer container) {
        super(container);
        ScreenRegion machInvRegion = container.getLayout().getMachineInventoryRegion();
        sideConfigHandler = new SideConfigurationUiHandler(this,
                machInvRegion.getX() + machInvRegion.getWidth() - 11, machInvRegion.getY() - 11,
                container::toggleBufferSide, container::toggleAutoExporting);
    }

    @Override
    public void initGui() {
        super.initGui();
        SingleBlockMachineContainer cont = getContainer();
        UiLayout layout = cont.getLayout();
        ScreenRegion machInvRegion = layout.getMachineInventoryRegion();
        RedstoneControlHandler rsHandler = cont.getSingleBlockMachine().getRedstoneHandler();
        addComponent(new RedstoneControlGuiComponent(
                machInvRegion.getX() + machInvRegion.getWidth() - 24, machInvRegion.getY() - 11,
                rsHandler::getRedstoneBehaviour, cont::setRedstoneBehaviour));
        List<SingleBlockMachineContainer.BufferUiElement> bufUiElems = cont.getBufferUiElements();
        for (int i = 0; i < bufUiElems.size(); i++) {
            SingleBlockMachineContainer.BufferUiElement elem = bufUiElems.get(i);
            ScreenRegion region = elem.getUiElement().addToGui(this, machInvRegion);
            MachineSideHandler.SideConfig<?> sideConfig = elem.getSideConfig();
            if (sideConfig != null) {
                sideConfigHandler.addConfigRegion(i, new SideConfigurationUiHandler.ConfigRegion(sideConfig, region));
            }
        }
        cont.getRecipeLogicUiElement().addToGui(this, machInvRegion);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mX, int mY) {
        sideConfigHandler.render();
        if (sideConfigHandler.isInConfig()) {
            restrictedRendering = true;
            super.drawGuiContainerForegroundLayer(mX, mY);
            restrictedRendering = false;
        } else {
            super.drawGuiContainerForegroundLayer(mX, mY);
        }
        sideConfigHandler.renderOverlay();
        sideConfigHandler.renderTooltip(mX - getOffsetX(), mY - getOffsetY());
    }

    @Override
    protected void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font) {
        if (!restrictedRendering) {
            super.drawHoveringText(textLines, x, y, font);
        }
    }

    @Override
    protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return !sideConfigHandler.isInConfig() // now this is cursed
                && super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }

    @Override
    protected void mouseClicked(int mX, int mY, int button) throws IOException {
        if (sideConfigHandler.handleClick(mX - getOffsetX(), mY - getOffsetY())) {
            return;
        }
        super.mouseClicked(mX, mY, button);
    }

    @Override
    protected void mouseClickMove(int mX, int mY, int button, long dragTime) {
        if (!sideConfigHandler.isInConfig()) {
            super.mouseClickMove(mX, mY, button, dragTime);
        }
    }

    @Override
    protected void keyTyped(char typed, int keyCode) throws IOException {
        if (sideConfigHandler.isInConfig()) {
            if (keyCode == Keyboard.KEY_ESCAPE || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
                mc.player.closeScreen();
            }
        } else {
            super.keyTyped(typed, keyCode);
        }
    }

}
