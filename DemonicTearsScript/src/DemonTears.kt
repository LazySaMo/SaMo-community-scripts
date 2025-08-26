import com.osmb.api.item.ItemGroupResult
import com.osmb.api.item.ItemID
import com.osmb.api.script.Script
import com.osmb.api.script.ScriptDefinition
import com.osmb.api.script.SkillCategory
import com.osmb.api.utils.timing.Stopwatch
import com.osmb.api.visual.drawing.Canvas
import extensions.*
import timers.randomIdleTimer
import timers.randomPlayerAnimatingTimer
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import kotlin.math.max
import kotlin.math.round

@ScriptDefinition(
    name = "Demon Tears Woodcutting",
    author = "SaMo",
    version = 1.5,
    description = "Gathers demon tears by woodcutting",
    skillCategory = SkillCategory.WOODCUTTING
)
class DemonTears(core: Any) : Script(core) {
    private var tearsTotalGained = 0
    private var logsTotalGained = 0

    private val hudStartMs = System.currentTimeMillis()
    private val XP_PER_CHOP = 10

    override fun poll(): Int {
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

        val isPlayerAnimatingStopWatch = Stopwatch(randomPlayerAnimatingTimer())
        val idleStopwatch = Stopwatch(randomIdleTimer())

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
        val tearsWas = if (currentTearAmount < 0) tearsNow else currentTearAmount

        if (tearsNow > tearsWas) {
            tearsTotalGained += (tearsNow - tearsWas)
            stopwatch.reset(randomIdleTimer())
            isPlayerAnimatingStopWatch.reset(randomPlayerAnimatingTimer())
        }

        return Pair(logsNow, tearsNow)
    }

    override fun regionsToPrioritise(): IntArray = intArrayOf(5268)

    override fun onPaint(c: Canvas) {
        val elapsed = (System.currentTimeMillis() - hudStartMs).coerceAtLeast(1)
        val hours = elapsed / 3_600_000.0

        val tears = tearsTotalGained
        val tearsPerHour = round(tears / max(1e-9, hours)).toInt()

        val chops = logsTotalGained + tearsTotalGained
        val xpGained = chops * XP_PER_CHOP
        val xpPerHour = round(xpGained / max(1e-9, hours)).toInt()

        val title = "Demon Tears WC"
        val line1 = "Tears: $tears"
        val line2 = "Tears/hr: $tearsPerHour"
        val line3 = "WC XP gained: $xpGained"
        val line4 = "WC XP/hr: $xpPerHour"

        val x = 16
        val yTop = 64
        val padX = 14
        val padY = 10
        val border = 2

        val arial = Font("Arial", Font.PLAIN, 12)
        val arialBold = Font("Arial", Font.BOLD, 13)
        val arialItalic = Font("Arial", Font.ITALIC, 12)

        val fmBold: FontMetrics = c.getFontMetrics(arialBold)
        val fm: FontMetrics = c.getFontMetrics(arial)

        val textW = maxOf(
            fmBold.stringWidth(title),
            maxOf(
                maxOf(fm.stringWidth(line1), fm.stringWidth(line2)),
                maxOf(fm.stringWidth(line3), fm.stringWidth(line4))
            )
        )
        val w = textW + padX * 2
        val h = padY * 2 + fmBold.height + fm.height * 4 + 12

        val bg = Color(18, 18, 22, 200)
        val borderC = Color(108, 92, 231)
        val titleC = Color(236, 239, 244)
        val tearsC = Color(129, 236, 236)
        val rateC = Color(255, 203, 112)
        val xpC = Color(85, 239, 196)
        val xpRateC = Color(162, 155, 254)

        c.fillRect(x - border, yTop - border, w + border * 2, h + border * 2, borderC.rgb, 1.0)
        c.fillRect(x, yTop, w, h, bg.rgb, 1.0)
        c.drawRect(x, yTop, w, h, borderC.rgb)

        var cx = x + padX
        var cy = yTop + padY + fmBold.ascent
        c.drawText(title, cx, cy, titleC.rgb, arialBold)
        cy += fmBold.height
        c.drawText(line1, cx, cy, tearsC.rgb, arial)
        cy += fm.height
        c.drawText(line2, cx, cy, rateC.rgb, arial)
        cy += fm.height
        c.drawText(line3, cx, cy, xpC.rgb, arial)
        cy += fm.height
        c.drawText(line4, cx, cy, xpRateC.rgb, arialItalic)
    }

    companion object {
        const val INFECTED_TREE_NAME = "Strange root"
        const val CHOP_INTERACTION_NAME = "chop infected root"
    }
}