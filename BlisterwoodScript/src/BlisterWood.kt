import com.osmb.api.item.ItemGroupResult
import com.osmb.api.item.ItemID
import com.osmb.api.scene.RSObject
import com.osmb.api.script.Script
import com.osmb.api.script.ScriptDefinition
import com.osmb.api.script.SkillCategory
import com.osmb.api.utils.RandomUtils
import com.osmb.api.utils.timing.Stopwatch
import extensions.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BooleanSupplier

@ScriptDefinition(
    name = "Blisterwood Tree woodcutting",
    author = "SaMo",
    version = 1.1,
    description = "Cuts Blisterwood tree",
    skillCategory = SkillCategory.WOODCUTTING
)
class BlisterWood(core: Any) : Script(core) {
    private var logsTotalGained = 0

    override fun poll(): Int {
        when {
            isInventoryFull() -> {
                dropItemIds(setOf(ItemID.BLISTERWOOD_LOGS))
            }

            shouldChop() -> chopBlisterWoodTree()
        }

        return 0
    }

    private fun shouldChop(): Boolean {
        val root = closestObject(BLISTER_WOOD_TREE_NAME)

        return when {
            root == null -> {
                log("Can't find Blisterwood Tree, please start the script in the area...")
                false
            }

            !root.interact(CHOP_INTERACTION_NAME) -> {
                log("failed to interact...")
                false
            }

            !waitUntilReachedRoots(root) -> {
                log("failed to reach root...")
                false
            }

            else -> true
        }
    }

    private fun waitUntilReachedRoots(root: RSObject): Boolean {
        val positionChangeTimeout = RandomUtils.uniformRandom(500, 1000)

        val result = AtomicBoolean(false)

        submitTask(BooleanSupplier {
            val worldPosition = getWorldPosition()

            when {
                worldPosition == null -> false

                root.objectArea.distanceTo(worldPosition) <= 1 -> {
                    result.set(true)
                    true
                }

                lastPositionChangeMillis > positionChangeTimeout -> {
                    true
                }

                else -> false
            }

        }, RandomUtils.uniformRandom(4000, 8000))

        return result.get()
    }

    private fun chopBlisterWoodTree() {
        val stopWatch = Stopwatch(getRandomIdleTime())
        val currentLogAmount = AtomicInteger(-1)

        loopUntilTrueOrTimeout(200000, 300000) {
            val inventory = inventoryOf(setOf(ItemID.BLISTERWOOD_LOGS))

            when {
                inventory == null -> {
                    log("Failed to get inventory snapshot, trying again...")
                    false
                }

                inventory.isFull -> true

                stopWatch.hasFinished() -> {
                    log("We have been idle for too long, chop again..")
                    true
                }

                else -> {
                    onLogListener(currentLogAmount, inventory, stopWatch)
                    false
                }
            }
        }
    }

    fun onLogListener(
        currentLogAmount: AtomicInteger,
        inventorySnapshot: ItemGroupResult,
        stopwatch: Stopwatch,
    ) {
        val blisterWoodAmount = inventorySnapshot.getAmount(ItemID.BLISTERWOOD_LOGS)

        if (currentLogAmount.get() == -1) {
            currentLogAmount.set(blisterWoodAmount)
        }

        val newLogAmount = blisterWoodAmount

        if (currentLogAmount.get() < newLogAmount) {
            val gained = newLogAmount - currentLogAmount.get()
            logsTotalGained += gained
            currentLogAmount.set(newLogAmount)
            stopwatch.reset(getRandomIdleTime())
        }
    }

    private fun getRandomIdleTime(): Long = RandomUtils.uniformRandom(8000, 16000).toLong()

    override fun regionsToPrioritise(): IntArray = intArrayOf(14388)

    companion object {
        const val CHOP_INTERACTION_NAME = "Chop"
        const val BLISTER_WOOD_TREE_NAME = "Blisterwood tree"
    }
}
