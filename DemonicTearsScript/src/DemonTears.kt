import com.osmb.api.item.ItemGroupResult
import com.osmb.api.item.ItemID
import com.osmb.api.scene.RSObject
import com.osmb.api.script.Script
import com.osmb.api.script.ScriptDefinition
import com.osmb.api.script.SkillCategory
import com.osmb.api.utils.RandomUtils
import com.osmb.api.utils.timing.Stopwatch
import extensions.*
import timers.randomIdleTimer
import timers.randomPlayerAnimatingTimer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BooleanSupplier

@ScriptDefinition(
    name = "Demon Tears Woodcutting",
    author = "SaMo",
    version = 1.3,
    description = "Gathers demon tears by woodcutting",
    skillCategory = SkillCategory.WOODCUTTING
)
class DemonTears(core: Any) : Script(core) {
    private var tearsTotalGained = 0
    private var logsTotalGained = 0

    override fun poll(): Int {
        log("not working")
        when {
            isInventoryFull() -> dropItemIds(setOf(ItemID.LOGS))
            else -> chopInfectedRoot()
        }

        return 0
    }

    private fun chopInfectedRoot(): Boolean {
        val root = closestObject(INFECTED_TREE_NAME) ?: run {
            log("Can't find infected Root, start in the area…")
            return false
        }

        if (!root.interactRawText(CHOP_INTERACTION_NAME)) {
            log("Failed to interact…")
            return false
        }

        if (!waitUntilAtRoot(root)) {
            log("Failed to reach root…")
            return false
        }

        val isPlayerAnimatingStopWatch = Stopwatch(randomIdleTimer())
        val idleStopwatch = Stopwatch(randomPlayerAnimatingTimer())

        var currentLogAmount = -1
        var currentTearAmount = -1

        loopUntilTrueOrTimeout(200_000, 300_000) {
            val inventory = inventoryOf(setOf(ItemID.LOGS, ItemID.DEMON_TEAR))
            when {
                pixelAnalyzer.isPlayerAnimating(0.3) -> {
                    isPlayerAnimatingStopWatch.reset(randomPlayerAnimatingTimer())
                    idleStopwatch.reset(randomIdleTimer())
                    false
                }

                inventory == null -> {
                    log("Failed to get inventory snapshot, retrying…")
                    false
                }

                inventory.isFull -> true

                idleStopwatch.hasFinished() || isPlayerAnimatingStopWatch.hasFinished() -> {
                    log("Idle too long, re-chop…")
                    true
                }

                else -> {
                    val (logsAmount, tearAmount) =
                        onItemListener(
                            currentLogAmount,
                            currentTearAmount,
                            inventory,
                            idleStopwatch,
                            isPlayerAnimatingStopWatch
                        )

                    currentLogAmount = logsAmount
                    currentTearAmount = tearAmount

                    false
                }
            }
        }

        return true
    }

    private fun waitUntilAtRoot(root: RSObject): Boolean {
        val positionChangeTimeout = RandomUtils.uniformRandom(500, 1000)
        val result = AtomicBoolean(false)

        submitTask(BooleanSupplier {
            val wp = worldPosition ?: return@BooleanSupplier false

            when {
                root.objectArea.distanceTo(wp) <= 1 -> {
                    result.set(true); true
                }

                lastPositionChangeMillis > positionChangeTimeout -> true
                else -> false
            }

        }, RandomUtils.uniformRandom(4000, 8000))

        return result.get()
    }

    private fun onItemListener(
        currentLogAmount: Int,
        currentTearAmount: Int,
        inv: ItemGroupResult,
        stopwatch: Stopwatch,
        isPlayerAnimatingStopWatch: Stopwatch,
    ): Pair<Int, Int> {
        val logsNow = inv.getAmount(ItemID.LOGS)
        val logsWas = if (currentLogAmount < 0) logsNow else currentLogAmount

        if (logsNow > logsWas) {
            logsTotalGained += (logsNow - logsWas)
            stopwatch.reset(randomIdleTimer())
            isPlayerAnimatingStopWatch.reset(randomPlayerAnimatingTimer())
        }

        val tearsNow = inv.getAmount(ItemID.DEMON_TEAR)
        val tearsWas = if (currentTearAmount < 0) logsNow else currentTearAmount

        if (tearsNow > tearsWas) {
            tearsTotalGained += (tearsNow - tearsWas)
            stopwatch.reset(randomIdleTimer())
            isPlayerAnimatingStopWatch.reset(randomPlayerAnimatingTimer())
        }

        return Pair(logsNow, tearsNow)
    }

    override fun regionsToPrioritise(): IntArray = intArrayOf(5268)


    companion object {
        const val INFECTED_TREE_NAME = "Strange root"
        const val CHOP_INTERACTION_NAME = "chop infected root"
    }
}