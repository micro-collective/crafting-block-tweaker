package xyz.phanta.cbtweaker.gui.inventory;

import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.math.Vec2i;
import io.github.phantamanta44.libnine.util.render.TextureRegion;
import io.github.phantamanta44.libnine.util.render.TextureResource;
import net.minecraft.util.ResourceLocation;
import xyz.phanta.cbtweaker.gui.ScreenRegion;

public class UiLayout {

    public static final ScreenRegion DEFAULT_MACHINE_REGION = new ScreenRegion(7, 16, 162, 55);
    public static final Vec2i DEFAULT_PLAYER_INV_POS = new Vec2i(7, 83);

    public static UiLayout fromJson(JsonObject spec) {
        ResourceLocation bgTextureLoc = new ResourceLocation(spec.get("background").getAsString() + ".png");
        int bgWidth = spec.get("background_width").getAsInt();
        int bgHeight = spec.get("background_height").getAsInt();
        ScreenRegion machineInvRegion = !spec.has("machine_x") ? DEFAULT_MACHINE_REGION : new ScreenRegion(
                spec.get("machine_x").getAsInt(),
                spec.get("machine_y").getAsInt(),
                spec.get("machine_width").getAsInt(),
                spec.get("machine_height").getAsInt());
        boolean renderMachineName = !spec.has("machine_name") || spec.get("machine_name").getAsBoolean();
        Vec2i playerInvPos = !spec.has("player_x") ? DEFAULT_PLAYER_INV_POS : new Vec2i(
                spec.get("player_x").getAsInt(),
                spec.get("player_y").getAsInt());
        boolean renderPlayerName = !spec.has("player_name") || spec.get("player_name").getAsBoolean();
        return new UiLayout(
                new TextureResource(bgTextureLoc, 256, 256).getRegion(0, 0, bgWidth, bgHeight),
                machineInvRegion, renderMachineName, playerInvPos, renderPlayerName);
    }

    private final TextureRegion bgTexture;
    private final ScreenRegion machineInvRegion;
    private final boolean renderMachineName;
    private final Vec2i playerInvPos;
    private final boolean renderPlayerInvName;

    public UiLayout(TextureRegion bgTexture,
                    ScreenRegion machineInvRegion, boolean renderMachineName,
                    Vec2i playerInvPos, boolean renderPlayerInvName) {
        this.bgTexture = bgTexture;
        this.machineInvRegion = machineInvRegion;
        this.renderMachineName = renderMachineName;
        this.playerInvPos = playerInvPos;
        this.renderPlayerInvName = renderPlayerInvName;
    }

    public TextureRegion getBackgroundTexture() {
        return bgTexture;
    }

    public ScreenRegion getMachineInventoryRegion() {
        return machineInvRegion;
    }

    public boolean shouldRenderMachineName() {
        return renderMachineName;
    }

    public Vec2i getPlayerInventoryPosition() {
        return playerInvPos;
    }

    public boolean shouldRenderPlayerInvName() {
        return renderPlayerInvName;
    }

}
