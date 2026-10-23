package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.Planet;
import yio.tro.opacha.game.gameplay.model.PlanetType;
import yio.tro.opacha.game.gameplay.model.Link;
import java.util.ArrayList;
import java.util.Random;

/** Simple's planner, with a one-in-five roll on each planned command. */
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
            if (isBeyondCommand(planet) || planet.type == PlanetType.artillery
                    || (int) planet.unitsInside < 1) continue;
            boolean legal = false;
            for (Link link : planet.adjoinedLinks)
                if (!link.isBlueprint()) legal = true;
            if (!legal) continue;
            sources.add(planet);
        }
        if (sources.isEmpty()) return false;
        Planet start = sources.get(random.nextInt(sources.size()));
        ArrayList<Planet> targets = new ArrayList<>();
        for (Link link : start.adjoinedLinks)
            if (!link.isBlueprint()) targets.add(link.getOppositePlanet(start));
        Planet target = targets.get(random.nextInt(targets.size()));
        int[] percentages = {25, 50, 100};
        int percent = percentages[random.nextInt(percentages.length)];
        // Tiny garrisons still issue one legal troop order, rather than rounding to zero.
        if ((int) start.unitsInside * percent / 100 == 0) percent = 100;
        start.setAutoTarget(null);
        if (!getPlanetsManager().sendUnitsPercent(start, target, percent)) return false;
        markSourceUsed(start);
        onActionPerformed();
        return true;
    }
}
