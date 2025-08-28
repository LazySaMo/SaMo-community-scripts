package extensions

import com.osmb.api.location.position.types.WorldPosition
import com.osmb.api.scene.RSObject
import com.osmb.api.script.Script
import com.osmb.api.shape.Polygon

fun RSObject.interactRawText(rawTextEquals: String): Boolean =
    interact { entries ->
        entries.firstOrNull { it.rawText.contains(rawTextEquals, ignoreCase = true) }
    }

fun Script.tapProjected(
    pos: WorldPosition,
    actionText: String? = null,
    cubeHeight: Int = 160,
    scale: Double = 0.7,
    requireOnScreen: Boolean = false
): Boolean {
    val poly: Polygon = sceneProjector
        .getTileCube(pos, cubeHeight, requireOnScreen)
        ?.getResized(scale) ?: return false

    return if (actionText != null) {
        finger.tap(true, poly, { entries ->
            entries.firstOrNull { it.rawText.contains(actionText, ignoreCase = true) }
        })
    } else {
        finger.tap(true, poly)
    }
}