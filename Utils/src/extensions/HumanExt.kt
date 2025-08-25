package extensions

import com.osmb.api.script.Script
import java.util.function.BooleanSupplier

fun Script.loopUntilTrueOrTimeout(min: Int, max: Int, block: () -> Boolean) {
    submitHumanTask(BooleanSupplier { block() }, random(min, max))
}