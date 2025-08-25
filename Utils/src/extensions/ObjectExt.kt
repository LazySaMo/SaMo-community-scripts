package extensions

import com.osmb.api.scene.RSObject

fun RSObject.interactRawText(rawTextEquals: String): Boolean =
    interact { entries ->
        entries.firstOrNull { it.rawText.equals(rawTextEquals, ignoreCase = true) }
    }