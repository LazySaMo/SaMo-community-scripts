package extensions

import com.osmb.api.item.ItemGroupResult
import com.osmb.api.scene.RSObject
import com.osmb.api.script.Script

fun Script.inventoryAll(): ItemGroupResult? =
    widgetManager.inventory.search(emptySet())

fun Script.inventoryOf(ids: Set<Int>): ItemGroupResult? =
    widgetManager.inventory.search(ids)

fun Script.isInventoryFull(): Boolean =
    inventoryAll()?.isFull == true

fun Script.closestObject(name: String): RSObject? =
    objectManager.getClosestObject(name)

fun Script.closestObjects(name: String): List<RSObject>? =
    objectManager.getObjects {
        it?.name?.contains(name) == true
    }