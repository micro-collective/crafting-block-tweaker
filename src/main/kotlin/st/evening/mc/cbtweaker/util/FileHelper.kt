package st.evening.mc.cbtweaker.util

import java.nio.file.Files
import java.nio.file.Path

inline fun Path.forEachFile(action: (Path) -> Unit) {
    if (!Files.exists(this)) return
    Files.newDirectoryStream(this).use {
        it.forEach(action)
    }
}
