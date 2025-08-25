import com.osmb.api.item.ItemID
import com.osmb.api.script.Script
import com.osmb.api.script.ScriptDefinition
import com.osmb.api.script.SkillCategory
import extensions.*

@ScriptDefinition(
    name = "Blisterwood Tree woodcutting",
    author = "SaMo",
    version = 1.1,
    description = "Cuts Blisterwood tree",
    skillCategory = SkillCategory.WOODCUTTING
)
class BlisterWood(core: Any) : Script(core) {
    private var isChopping = false

    private var lastGainAt = System.currentTimeMillis()
    private var idleWindowMs = random(IDLE_LOWER_BOUND_WINDOW, IDLE_UPPER_BOUND_WINDOW)

    private var logsBaseline = 0
    private var logsTotalGained = 0

    override fun poll(): Int {
        if (refreshGains()) {
            lastGainAt = System.currentTimeMillis()
            idleWindowMs = random(IDLE_LOWER_BOUND_WINDOW, IDLE_UPPER_BOUND_WINDOW)
        }

        when {
            isInventoryFull() -> {
                afterHuman(400, 800) { dropItemIds(setOf(ItemID.BLISTERWOOD_LOGS)) }
                resetBaselines()
            }

            shouldChop() -> chopBlisterWoodTree()
        }

        return 0
    }

    private fun refreshGains(): Boolean {
        var progressed = false
        val logsNow = amountById(ItemID.BLISTERWOOD_LOGS)

        if (logsNow > logsBaseline) {
            logsTotalGained += (logsNow - logsBaseline)
            progressed = true
        }

        logsBaseline = logsNow

        return progressed
    }


    private fun resetBaselines() {
        isChopping = false
        lastGainAt = System.currentTimeMillis()
    }

    private fun shouldChop(): Boolean {
        val now = System.currentTimeMillis()
        if (isChopping && now - lastGainAt < idleWindowMs) return false
        resetBaselines()
        return closestObject(BLISTER_WOOD_TREE_NAME) != null
    }

    private fun chopBlisterWoodTree() {
        val root = closestObject(BLISTER_WOOD_TREE_NAME) ?: return

        if (isInventoryFull()) return

        afterHuman(400, 800) {
            if (!isChopping) isChopping = root.interact(CHOP_INTERACTION_NAME)
            isChopping
        }
    }

    override fun onRelog() {
        super.onRelog()
        resetBaselines()
    }

    override fun regionsToPrioritise(): IntArray = intArrayOf(14388)

    companion object {
        const val CHOP_INTERACTION_NAME = "Chop"
        const val BLISTER_WOOD_TREE_NAME = "Blisterwood tree"
        const val IDLE_LOWER_BOUND_WINDOW = 8_000
        const val IDLE_UPPER_BOUND_WINDOW = 10_000
    }
}
