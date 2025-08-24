import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;

import java.awt.*;
import java.util.Collections;
import java.util.Set;

import static com.osmb.api.ui.component.tabs.skill.SkillsTabComponent.ARIAL;

@ScriptDefinition(
        name = "Demon Tear Woodcutting",
        author = "SaMo",
        version = 1.0,
        description = "Gathers demon tears by woodcutting",
        skillCategory = SkillCategory.WOODCUTTING
)
public class DemonTears extends Script {
    private static final int DEMON_TEAR_ID = ItemID.DEMON_TEAR;
    private boolean isChopping = false;

    private long lastGainAt = System.currentTimeMillis();
    private int idleWindowMs = random(12_000, 15_000);

    private int logsBaseline = 0;
    private int tearsBaseline = -1;

    private final long hudStartMs = System.currentTimeMillis();
    private static final Font ARIAL_BOLD = Font.getFont("Arial Bold", null);
    private static final Font ARIAL_ITALIC = Font.getFont("Arial Italic", null);
    private int logsTotalGained = 0;
    private int tearsTotalGained = 0;

    public DemonTears(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int poll() {
        if (refreshGains()) {
            lastGainAt = System.currentTimeMillis();
            idleWindowMs = random(12_000, 15_000);
        }

        if (shouldDrop()) {
            dropLogs();
            logsBaseline = getAmountById(ItemID.LOGS);
            tearsBaseline = getAmountById(DEMON_TEAR_ID);
            lastGainAt = System.currentTimeMillis();
            isChopping = false;
        } else if (shouldChop()) {
            chopInfectedRoot();
        }

        return 0;
    }

    private boolean shouldChop() {
        long now = System.currentTimeMillis();
        if (now - lastGainAt < idleWindowMs && isChopping) return false;
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
            int delta = logsNow - logsBaseline;
            logsTotalGained += delta;
            logsBaseline = logsNow;
            progressed = true;
        } else if (logsNow < logsBaseline) {
            logsBaseline = logsNow;
        }
        int tearsNow = getAmountById(DEMON_TEAR_ID);
        if (tearsNow > tearsBaseline) {
            int delta = tearsNow - tearsBaseline;
            tearsTotalGained += delta;
            tearsBaseline = tearsNow;
            progressed = true;
        } else if (tearsNow < tearsBaseline) {
            tearsBaseline = tearsNow;
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
    public void onPaint(Canvas c) {
        long elapsed = Math.max(1, System.currentTimeMillis() - hudStartMs);
        double hours = elapsed / 3_600_000.0;
        int tearsPerHour = (int) Math.round(tearsTotalGained / Math.max(1e-9, hours));
        int totalChops = logsTotalGained + tearsTotalGained;
        int xpGained = totalChops * 10;
        int xpPerHour = (int) Math.round(xpGained / Math.max(1e-9, hours));

        String title = "Demon Tears Woodcutting";
        String line1 = "Tears: " + tearsTotalGained;
        String line2 = "Tears/hr: " + tearsPerHour;
        String line3 = "WC XP gained: " + xpGained;
        String line4 = "WC XP/hr: " + xpPerHour;

        int x = 16;
        int yTop = 64;
        int padX = 14;
        int padY = 10;
        int border = 2;

        FontMetrics fmBold = c.getFontMetrics(ARIAL_BOLD);
        FontMetrics fm = c.getFontMetrics(ARIAL);

        int textW = Math.max(
                fmBold.stringWidth(title),
                Math.max(Math.max(fm.stringWidth(line1), fm.stringWidth(line2)),
                        Math.max(fm.stringWidth(line3), fm.stringWidth(line4)))
        );
        int w = textW + padX * 2;
        int h = padY * 2 + fmBold.getHeight() + fm.getHeight() * 4 + 12;

        Color bg = new Color(18, 18, 22, 200);
        Color borderC = new Color(108, 92, 231);
        Color titleC = new Color(236, 239, 244);
        Color tearsC = new Color(129, 236, 236);
        Color rateC = new Color(255, 203, 112);
        Color xpC = new Color(85, 239, 196);
        Color xpRateC = new Color(162, 155, 254);

        c.fillRect(x - border, yTop - border, w + border * 2, h + border * 2, borderC.getRGB(), 1);
        c.fillRect(x, yTop, w, h, bg.getRGB(), 1);
        c.drawRect(x, yTop, w, h, borderC.getRGB());

        int cx = x + padX;
        int cy = yTop + padY + fmBold.getAscent();
        c.drawText(title, cx, cy, titleC.getRGB(), ARIAL_BOLD);
        cy += fmBold.getHeight();
        c.drawText(line1, cx, cy, tearsC.getRGB(), ARIAL);
        cy += fm.getHeight();
        c.drawText(line2, cx, cy, rateC.getRGB(), ARIAL);
        cy += fm.getHeight();
        c.drawText(line3, cx, cy, xpC.getRGB(), ARIAL);
        cy += fm.getHeight();
        c.drawText(line4, cx, cy, xpRateC.getRGB(), ARIAL_ITALIC);
    }

    @Override
    public void onRelog() {
        super.onRelog();
        isChopping = false;
        lastGainAt = System.currentTimeMillis();
        logsBaseline = getAmountById(ItemID.LOGS);
        tearsBaseline = getAmountById(DEMON_TEAR_ID);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{5268};
    }
}
