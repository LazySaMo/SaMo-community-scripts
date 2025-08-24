package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Collections;
import java.util.Set;

public class Drop extends Task {
    public Drop(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        ItemGroupResult inventory = script.getWidgetManager().getInventory().search(Collections.emptySet());
        return inventory != null && inventory.isFull();
    }

    @Override
    public boolean execute() {
        ItemGroupResult inventory = script.getWidgetManager().getInventory().search(Set.of(ItemID.LOGS));

        if (inventory == null) {
            return false;
        }

        script.getWidgetManager().getInventory().dropItems(Set.of(ItemID.LOGS));

        return false;
    }
}
