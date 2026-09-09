package st.evening.mc.cbtweaker.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import st.evening.mc.prelude.api.util.data.JsonDsl
import st.evening.mc.prelude.api.util.data.JsonObjectAction
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

internal object DataGenHelper {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    val machineBlockState: JsonObject = JsonDsl.obj {
        "forge_marker" number 1
        "defaults" obj {
            "model" string "cbtweaker:machine"
        }
        "variants" obj {
            "active" obj {
                "false" obj {}
                "true" obj {
                    "model" string "cbtweaker:machine_active"
                }
            }
            "facing" obj {
                "north" obj {}
                "east" obj { "y" number 90 }
                "south" obj { "y" number 180 }
                "west" obj { "y" number 270 }
            }
        }
    }

    val machineItemModel: JsonObject = JsonDsl.obj {
        "parent" string "cbtweaker:block/machine"
    }

    fun writeToFile(file: Path, json: JsonElement) {
        file.bufferedWriter().use { gson.toJson(json, it) }
    }

    inline fun writeToFile(file: Path, jsonAction: JsonObjectAction) {
        writeToFile(file, JsonDsl.obj(jsonAction))
    }
}
