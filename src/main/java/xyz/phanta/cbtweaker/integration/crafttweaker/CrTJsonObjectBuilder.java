package xyz.phanta.cbtweaker.integration.crafttweaker;

import com.google.gson.JsonObject;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenConstructor;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.cbtweaker.JsonObjectBuilder")
public class CrTJsonObjectBuilder {

    private final JsonObject jsonObj = new JsonObject();

    @ZenConstructor
    public CrTJsonObjectBuilder() {
        // NO-OP
    }

    public JsonObject getJsonObject() {
        return jsonObj;
    }

    @ZenMethod("string")
    public CrTJsonObjectBuilder putString(String key, String value) {
        jsonObj.addProperty(key, value);
        return this;
    }

    @ZenMethod("integer")
    public CrTJsonObjectBuilder putInt(String key, int value) {
        jsonObj.addProperty(key, value);
        return this;
    }

    @ZenMethod("float")
    public CrTJsonObjectBuilder putFloat(String key, double value) {
        jsonObj.addProperty(key, value);
        return this;
    }

    @ZenMethod("boolean")
    public CrTJsonObjectBuilder putBoolean(String key, boolean value) {
        jsonObj.addProperty(key, value);
        return this;
    }

    @ZenMethod("object")
    public CrTJsonObjectBuilder putJsonObject(String key, CrTJsonObjectBuilder child) {
        jsonObj.add(key, child.getJsonObject());
        return this;
    }

    @ZenMethod("array")
    public CrTJsonObjectBuilder putJsonArray(String key, CrTJsonArrayBuilder child) {
        jsonObj.add(key, child.getJsonArray());
        return this;
    }

}
