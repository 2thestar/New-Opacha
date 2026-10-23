package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.Planet;

/** Simple prefers whole armies, reducing the send only when a reserve is necessary. */
public class AiCustomEntity extends AiSavvyEntity {
    private static final int[] AGGRESSIVE_PERCENTAGES = {100, 50, 25};

    public AiCustomEntity(ObjectsLayer layer) { super(layer); }

    @Override protected int[] attackPercentages() { return AGGRESSIVE_PERCENTAGES; }

    protected void orderLaunch(Planet source, Planet target) {
        issueOrder(source, target, 100);
    }
}
