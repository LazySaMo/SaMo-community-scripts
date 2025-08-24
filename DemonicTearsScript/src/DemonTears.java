import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import tasks.Chop;
import tasks.Drop;
import utils.Task;

import java.util.Arrays;
import java.util.List;

@ScriptDefinition(
        name = "Demon Tear Woodcutting",
        author = "SaMo",
        version = 1.0,
        description = "Gathers demon tears by woodcutting",
        skillCategory = SkillCategory.WOODCUTTING
)
public class DemonTears extends Script {
    private final List<Task> tasks = Arrays.asList(
            new Drop(this),
            new Chop(this)
    );

    public DemonTears(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int poll() {
        for (Task task : tasks) {
            if (task.activate()) {
                submitHumanTask(task::execute, this.random(400, 800));
                return 0;
            }
        }

        return 0;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{5268};
    }
}
