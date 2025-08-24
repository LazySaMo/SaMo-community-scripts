package tasks;

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import utils.Task;

public class Chop extends Task {
    RSObject infectedRoot;

    public Chop(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (script.getPixelAnalyzer().isPlayerAnimating(0.3)) return false;

        script.log("player is not animating");

        infectedRoot = script.getObjectManager().getClosestObject("Infected root");

        script.log("player has found infected root " + infectedRoot);

        return infectedRoot != null;
    }

    @Override
    public boolean execute() {
        return infectedRoot.interact("Chop");
    }
}
