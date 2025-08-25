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
import java.util.function.BooleanSupplier
import kotlin.math.max
import kotlin.math.round

@ScriptDefinition(
    name = "Blisterwood Tree woodcutting",
    author = "SaMo",
    version = 1.2,
    description = "Cuts Blisterwood tree",
    skillCategory = SkillCategory.WOODCUTTING
)
class BlisterWood(core: Any) : Script(core) {
    private var logsTotalGained = 0

    private val hudStartMs = System.currentTimeMillis()
    private val XP_PER_LOG = 76

    override fun poll(): Int {
        when {
            isInventoryFull() -> dropItemIds(setOf(ItemID.BLISTERWOOD_LOGS))
            else -> chopBlisterWoodTree()
        }

        return 0
    }

    private fun chopBlisterWoodTree(): Boolean {
        val root = closestObject(BLISTER_WOOD_TREE_NAME) ?: run {
            log("Can't find Blisterwood Tree, start in the area…")
            return false
        }

        if (!root.interact(CHOP_INTERACTION_NAME)) {
            log("Failed to interact…")
            return false
        }

        if (!waitUntilAtTree(root)) {
            log("Failed to reach tree…")
            return false
        }

        val isPlayerAnimatingStopWatch = Stopwatch(randomIdleTimer())
        val idleStopwatch = Stopwatch(randomPlayerAnimatingTimer())

        var currentLogAmount = -1

        loopUntilTrueOrTimeout(200_000, 300_000) {
            val inventory = inventoryOf(setOf(ItemID.BLISTERWOOD_LOGS))
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
                    currentLogAmount =
                        onLogListener(currentLogAmount, inventory, idleStopwatch, isPlayerAnimatingStopWatch)
                    false
                }
            }
        }

        val shouldDelay = RandomUtils.gaussianRandom(0, 5, 2.0, 2.0) == 0
        if (shouldDelay) {
            val delayMs = RandomUtils.gaussianRandom(500, 5000, 1500.0, 1000.0)
            submitTask({ false }, delayMs)
        }

        return true
    }

    private fun waitUntilAtTree(root: RSObject): Boolean {
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

    private fun onLogListener(
        currentAmount: Int,
        inv: ItemGroupResult,
        stopwatch: Stopwatch,
        isPlayerAnimatingStopWatch: Stopwatch,
    ): Int {
        val now = inv.getAmount(ItemID.BLISTERWOOD_LOGS)
        val was = if (currentAmount < 0) now else currentAmount

        if (now > was) {
            logsTotalGained += (now - was)
            stopwatch.reset(randomIdleTimer())
            isPlayerAnimatingStopWatch.reset(randomPlayerAnimatingTimer())
        }

        return now
    }

    private fun randomIdleTimer(): Long = RandomUtils.uniformRandom(8000, 12000).toLong()

    private fun randomPlayerAnimatingTimer(): Long = RandomUtils.uniformRandom(1500, 2000).toLong()

    override fun regionsToPrioritise(): IntArray = intArrayOf(14388)

    override fun onPaint(c: com.osmb.api.visual.drawing.Canvas) {
        val elapsed = (System.currentTimeMillis() - hudStartMs).coerceAtLeast(1)
        val hours = elapsed / 3_600_000.0
        val xpGained = logsTotalGained * XP_PER_LOG
        val xpPerHour = round(xpGained / max(1e-9, hours)).toInt()

        val title = "Blisterwood WC"
        val line1 = "XP gained: $xpGained"
        val line2 = "XP/hr: $xpPerHour"

        val x = 16
        val yTop = 64
        val padX = 14
        val padY = 10
        val border = 2

        val arial = java.awt.Font("Arial", java.awt.Font.PLAIN, 12)
        val arialBold = java.awt.Font("Arial", java.awt.Font.BOLD, 13)
        val arialItalic = java.awt.Font("Arial", java.awt.Font.ITALIC, 12)

        val fmBold = c.getFontMetrics(arialBold)
        val fm = c.getFontMetrics(arial)

        val textW = maxOf(
            fmBold.stringWidth(title),
            maxOf(fm.stringWidth(line1), fm.stringWidth(line2))
        )
        val w = textW + padX * 2
        val h = padY * 2 + fmBold.height + fm.height * 2 + 10

        val bg = java.awt.Color(18, 18, 22, 200)
        val borderC = java.awt.Color(108, 92, 231)
        val titleC = java.awt.Color(236, 239, 244)
        val xpC = java.awt.Color(85, 239, 196)
        val rateC = java.awt.Color(162, 155, 254)

        c.fillRect(x - border, yTop - border, w + border * 2, h + border * 2, borderC.rgb, 1.0)
        c.fillRect(x, yTop, w, h, bg.rgb, 1.0)
        c.drawRect(x, yTop, w, h, borderC.rgb)

        var cx = x + padX
        var cy = yTop + padY + fmBold.ascent
        c.drawText(title, cx, cy, titleC.rgb, arialBold)
        cy += fmBold.height
        c.drawText(line1, cx, cy, xpC.rgb, arial)
        cy += fm.height
        c.drawText(line2, cx, cy, rateC.rgb, arialItalic)
    }

    companion object {
        const val CHOP_INTERACTION_NAME = "Chop"
        const val BLISTER_WOOD_TREE_NAME = "Blisterwood tree"
    }
}
