package xyz.phanta.cbtweaker.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.phantamanta44.libnine.util.math.Vec2i;
import xyz.phanta.cbtweaker.gui.ScreenRegion;

import java.util.Locale;

public abstract class Positioning {

    public static Positioning fromJson(JsonElement dto) {
        if (dto.isJsonPrimitive()) {
            String posStr = dto.getAsString();
            switch (posStr.toLowerCase(Locale.ROOT)) {
                case "left":
                    return FromLeft.LEFT;
                case "center":
                    return FromCenter.CENTER;
                case "right":
                    return FromRight.RIGHT;
                case "origin":
                    return FromOrigin.ORIGIN;
            }
            throw new ConfigException("Unknown positioning: " + posStr);
        }
        if (dto.isJsonArray()) {
            JsonArray coordsDto = dto.getAsJsonArray();
            return new FromOrigin(coordsDto.get(0).getAsInt(), coordsDto.get(1).getAsInt());
        }
        if (dto.isJsonObject()) {
            JsonObject specDto = dto.getAsJsonObject();
            String refStr = specDto.get("from").getAsString();
            switch (refStr.toLowerCase(Locale.ROOT)) {
                case "left":
                    return new FromLeft(specDto.get("x").getAsInt(), specDto.get("y").getAsInt());
                case "center":
                    return new FromCenter(specDto.get("x").getAsInt(), specDto.get("y").getAsInt());
                case "right":
                    return new FromRight(specDto.get("x").getAsInt(), specDto.get("y").getAsInt());
                case "origin":
                    return new FromOrigin(specDto.get("x").getAsInt(), specDto.get("y").getAsInt());
            }
            throw new ConfigException("Unknown reference point : " + refStr);
        }
        throw new ConfigException("Could not parse positioning: " + dto);
    }

    private Positioning() {
        // NO-OP
    }

    public abstract Vec2i computePosition(int objWidth, int objHeight, int contWidth, int contHeight);

    public Vec2i computePosition(int objWidth, int objHeight, ScreenRegion contRegion) {
        return computePosition(objWidth, objHeight, contRegion.getWidth(), contRegion.getHeight())
                .add(contRegion.getX(), contRegion.getY());
    }

    public ScreenRegion computeRegion(int objWidth, int objHeight, ScreenRegion contRegion) {
        Vec2i pos = computePosition(objWidth, objHeight, contRegion);
        return new ScreenRegion(pos.getX(), pos.getY(), objWidth, objHeight);
    }

    public static class FromLeft extends Positioning {

        public static final Positioning LEFT = new FromLeft(0, 0);

        private final int offsetX, offsetY;

        public FromLeft(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public Vec2i computePosition(int objWidth, int objHeight, int contWidth, int contHeight) {
            return new Vec2i(offsetX, (contHeight - objHeight) / 2 + offsetY);
        }

    }

    public static class FromCenter extends Positioning {

        public static final Positioning CENTER = new FromCenter(0, 0);

        private final int offsetX, offsetY;

        public FromCenter(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public Vec2i computePosition(int objWidth, int objHeight, int contWidth, int contHeight) {
            return new Vec2i((contWidth - objWidth) / 2 + offsetX, (contHeight - objHeight) / 2 + offsetY);
        }

    }

    public static class FromRight extends Positioning {

        public static final Positioning RIGHT = new FromRight(0, 0);

        private final int offsetX, offsetY;

        public FromRight(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public Vec2i computePosition(int objWidth, int objHeight, int contWidth, int contHeight) {
            return new Vec2i(contWidth - objWidth + offsetX, (contHeight - objHeight) / 2 + offsetY);
        }

    }

    public static class FromOrigin extends Positioning {

        public static final Positioning ORIGIN = new FromOrigin(0, 0);

        private final Vec2i offset;

        public FromOrigin(int x, int y) {
            this.offset = new Vec2i(x, y);
        }

        @Override
        public Vec2i computePosition(int objWidth, int objHeight, int contWidth, int contHeight) {
            return offset;
        }

    }

}
