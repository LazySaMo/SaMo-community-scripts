package timers

import com.osmb.api.utils.RandomUtils

fun randomIdleTimer(): Long = RandomUtils.uniformRandom(8000, 12000).toLong()

fun randomPlayerAnimatingTimer(): Long = RandomUtils.uniformRandom(1500, 2000).toLong()