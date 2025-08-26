import com.osmb.api.item.ItemID
import com.osmb.api.scene.RSObject
import com.osmb.api.script.Script
import com.osmb.api.script.ScriptDefinition
import com.osmb.api.script.SkillCategory
import com.osmb.api.utils.timing.Stopwatch
import com.osmb.api.visual.PixelAnalyzer
import extensions.*
import timers.randomPlayerAnimatingTimer

@ScriptDefinition(
    name = "Volcanic Ash Miner",
    author = "SaMo",
    version = 1.0,
    description = "Mine Volcanic Ash",
    skillCategory = SkillCategory.MINING
)
class VolcanicAsh(core: Any) : Script(core) {
    private val ashBlacklist: MutableMap<RSObject, Long> = mutableMapOf()

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun purgeExpiredBlacklist() {
        val t = nowMs()
        ashBlacklist.entries.removeIf { (_, expiry) -> expiry <= t }
    }

    private fun isBlacklisted(obj: RSObject): Boolean {
        purgeExpiredBlacklist()
        val expiry = ashBlacklist[obj] ?: return false
        return expiry > nowMs()
    }

    private fun blacklist(obj: RSObject, ttlMs: Long = ASH_BLACKLIST_TTL_MS) {
        ashBlacklist[obj] = nowMs() + ttlMs
    }

    override fun poll(): Int {
        when {
            isInventoryFull() -> dropItemIds(setOf(ItemID.SODA_ASH, ItemID.UNCUT_RUBY, ItemID.UNCUT_EMERALD, ItemID.UNCUT_SAPPHIRE,
                ItemID.DIAMOND))
            else -> mineAshPile()
        }
        return 0
    }

    private fun mineAshPile(): Boolean {
        val ashPiles = closestObjects(ASH_PILE_NAME)

        if (ashPiles.isNullOrEmpty()) {
            log("Can't find ash piles, start in the area…")
            return false
        }

        if (worldPosition == null) return false

        val respawnCircles =
            pixelAnalyzer.getRespawnCircleObjects(
                ashPiles,
                PixelAnalyzer.RespawnCircleDrawType.CENTER,
                1,
                8
            )

        val mineableAshPiles =
            ashPiles
                .map { ash ->
                    MineableAshPiles(
                        rsObject = ash,
                        isMineable = !respawnCircles.contains(ash) && !isBlacklisted(ash) && ash.actions.contains("Mine"),
                        distanceTiles = ash.getTileDistance(worldPosition)
                    )
                }
                .filter { it.isMineable && it.distanceTiles > 0 }  // drop unreachable
                .sortedBy { it.distanceTiles }


        mineableAshPiles.firstOrNull()?.let { ashPile ->
            if (!ashPile.isMineable || isBlacklisted(ashPile.rsObject) || !ashPile.rsObject.interact("Mine")) {
                log("Was not able to mine; blacklisting for 30s")
                blacklist(ashPile.rsObject)
                return false
            }

            val isPlayerAnimatingStopWatch = Stopwatch(randomPlayerAnimatingTimer())

            loopUntilTrueOrTimeout(200_000, 300_000) {
                val inventory = inventoryOf(setOf(ItemID.SODA_ASH))
                val currentCircles =
                    pixelAnalyzer.getRespawnCircleObjects(ashPiles, PixelAnalyzer.RespawnCircleDrawType.CENTER, 1, 8)

                when {
                    pixelAnalyzer.isPlayerAnimating(0.3) -> {
                        isPlayerAnimatingStopWatch.reset(randomPlayerAnimatingTimer())
                        false
                    }

                    inventory == null -> {
                        log("Failed to get inventory snapshot, retrying…")
                        false
                    }

                    currentCircles.contains(ashPile.rsObject) -> {
                        blacklist(ashPile.rsObject)
                        true
                    }

                    inventory.isFull -> true

                    isPlayerAnimatingStopWatch.hasFinished() -> {
                        log("Idle too long, re-mine… temporarily blacklisting this pile")
                        blacklist(ashPile.rsObject)
                        true
                    }

                    else -> false
                }
            }
        }

        return true
    }

    override fun regionsToPrioritise(): IntArray = intArrayOf(14906, 14907, 15162, 15163)

    private companion object {
        const val ASH_PILE_NAME = "Ash pile"
        const val ASH_BLACKLIST_TTL_MS = 31000L // 31 seconds
    }
}

data class MineableAshPiles(
    val rsObject: RSObject,
    val isMineable: Boolean = true,
    val distanceTiles: Int,
)
