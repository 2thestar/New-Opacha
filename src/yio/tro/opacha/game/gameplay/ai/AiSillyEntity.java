package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.Planet;
import java.util.ArrayList;
import java.util.Random;

/** The original AI, with a one-in-five roll on each planned command, not each frame. */
public class AiSillyEntity extends AiCustomEntity {
    final Random random;
    private final ArrayList<Planet> sources = new ArrayList<>();

    public AiSillyEntity(ObjectsLayer layer) {
        this(layer, new Random(YioGdxGame.random.nextLong()));
    }

    AiSillyEntity(ObjectsLayer layer, Random random) {
        super(layer);
        this.random = random;
    }

    @Override
    protected boolean replacePlannedMove() {
        if (random.nextInt(5) != 0) return false;
        sources.clear();
        for (Planet planet : getPlanetsList()) {
            if (isBeyondCommand(planet) || (int) planet.unitsInside < 1 || planet.adjoinedLinks.isEmpty()) continue;
            sources.add(planet);
        }
        if (sources.isEmpty()) return false;
        Planet start = sources.get(random.nextInt(sources.size()));
        Planet target = start.adjoinedLinks.get(random.nextInt(start.adjoinedLinks.size())).getOppositePlanet(start);
        int[] percentages = {25, 50, 100};
        int percent = percentages[random.nextInt(percentages.length)];
        // Tiny garrisons still issue one legal troop order, rather than rounding to zero.
        if ((int) start.unitsInside * percent / 100 == 0) percent = 100;
        start.setAutoTarget(null);
        if (!getPlanetsManager().sendUnitsPercent(start, target, percent)) return false;
        onActionPerformed();
        return true;
    }
}
