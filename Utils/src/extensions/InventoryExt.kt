package extensions

import com.osmb.api.script.Script

fun Script.dropItemIds(ids: Set<Int>): Boolean =
    widgetManager.inventory.dropItems(ids)
