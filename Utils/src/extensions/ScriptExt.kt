package extensions

import com.osmb.api.item.ItemGroupResult
import com.osmb.api.scene.RSObject
import com.osmb.api.script.Script

fun Script.inventoryAll(): ItemGroupResult? =
    widgetManager.inventory.search(emptySet())

fun Script.inventoryOf(id: Int): ItemGroupResult? =
    widgetManager.inventory.search(setOf(id))

fun Script.isInventoryFull(): Boolean =
    inventoryAll()?.isFull == true

fun Script.amountById(id: Int): Int =
    inventoryOf(id)?.getAmount(id) ?: 0

fun Script.closestObject(name: String): RSObject? =
    objectManager.getClosestObject(name)