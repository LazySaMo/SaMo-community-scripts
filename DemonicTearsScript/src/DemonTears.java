import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;

import java.util.Collections;
import java.util.Set;

@ScriptDefinition(
        name = "Demon Tear Woodcutting",
        author = "SaMo",
        version = 1.0,
        description = "Gathers demon tears by woodcutting",
        skillCategory = SkillCategory.WOODCUTTING
)
public class DemonTears extends Script {
    private static final int DEMON_TEAR_ID = ItemID.DEMON_TEAR;

    private long lastGainAt = System.currentTimeMillis();
    private int idleWindowMs = random(12000, 15000);

    private int logsBaseline = 0;
    private int tearsBaseline = -1;

    private boolean isChopping = false;

    public DemonTears(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int poll() {
        if (refreshGains()) {
            lastGainAt = System.currentTimeMillis();
            idleWindowMs = random(12000, 15000);
        }

        if (shouldDrop()) {
            dropLogs();
            isChopping = false;
        } else if (shouldChop()) {
            chopInfectedRoot();
        }

        return 0;
    }

    private boolean shouldChop() {
        long now = System.currentTimeMillis();

        if (now - lastGainAt < idleWindowMs && isChopping) {
            return false;
        }

        RSObject root = getObjectManager().getClosestObject("Strange root");
        return root != null;
    }

    private void chopInfectedRoot() {
        RSObject root = getObjectManager().getClosestObject("Strange root");
        if (root == null) return;

        ItemGroupResult all = getWidgetManager().getInventory().search(Collections.emptySet());
        if (all != null && all.isFull()) return;

        submitHumanTask(() -> {
            if (!isChopping) {
                isChopping = root.interact(getRootMenuHook());
            }

            return isChopping;
        }, random(200, 400));
    }


    private boolean shouldDrop() {
        ItemGroupResult inventory = getWidgetManager().getInventory().search(Collections.emptySet());
        return inventory != null && inventory.isFull();
    }

    private void dropLogs() {
        submitHumanTask(
                () -> getWidgetManager().getInventory().dropItems(Set.of(ItemID.LOGS)),
                random(400, 800)
        );
    }


    private boolean refreshGains() {
        boolean progressed = false;

        int logsNow = getAmountById(ItemID.LOGS);
        if (logsNow > logsBaseline) {
            logsBaseline = logsNow;
            progressed = true;
        }

        int tearsNow = getAmountById(DEMON_TEAR_ID);
        if (tearsNow > tearsBaseline) {
            tearsBaseline = tearsNow;
            progressed = true;
        }

        return progressed;
    }

    private int getAmountById(int id) {
        ItemGroupResult res = getWidgetManager().getInventory().search(Set.of(id));
        if (res == null) return 0;
        return res.getAmount(id);
    }

    private MenuHook getRootMenuHook() {
        return menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                String raw = entry.getRawText();
                if (raw != null && raw.trim().equalsIgnoreCase("chop infected root")) {
                    return entry;
                }
            }
            return null;
        };
    }

    @Override
    public void onRelog() {
        super.onRelog();
        isChopping = false;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{5268};
    }
}
