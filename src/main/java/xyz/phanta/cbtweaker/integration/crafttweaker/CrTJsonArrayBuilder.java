package xyz.phanta.cbtweaker.integration.crafttweaker;

import com.google.gson.JsonArray;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenConstructor;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.cbtweaker.JsonArrayBuilder")
public class CrTJsonArrayBuilder {

    private final JsonArray jsonArr = new JsonArray();

    @ZenConstructor
    public CrTJsonArrayBuilder() {
        // NO-OP
    }

    public JsonArray getJsonArray() {
        return jsonArr;
    }

    @ZenMethod("string")
    public CrTJsonArrayBuilder addString(String value) {
        jsonArr.add(value);
        return this;
    }

    @ZenMethod("integer")
    public CrTJsonArrayBuilder addInt(int value) {
        jsonArr.add(value);
        return this;
    }

    @ZenMethod("float")
    public CrTJsonArrayBuilder addFloat(double value) {
        jsonArr.add(value);
        return this;
    }

    @ZenMethod("boolean")
    public CrTJsonArrayBuilder addBoolean(boolean value) {
        jsonArr.add(value);
        return this;
    }

    @ZenMethod("object")
    public CrTJsonArrayBuilder addJsonObject(CrTJsonObjectBuilder child) {
        jsonArr.add(child.getJsonObject());
        return this;
    }

    @ZenMethod("array")
    public CrTJsonArrayBuilder addJsonArray(CrTJsonArrayBuilder child) {
        jsonArr.add(child.getJsonArray());
        return this;
    }

}
