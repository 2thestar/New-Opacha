package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;

/** Paired maps for tuning, not a promise of universal superiority. */
public final class AiMatchChecks {
    public static String run(GameController controller) {
        StringBuilder result = new StringBuilder();
        int savvyWins = 0, simpleWins = 0, unresolved = 0;
        for (int seed = 1; seed <= 12; seed++) {
            for (int swap = 0; swap < 2; swap++) {
                GameRules.aiTypes.clear();
                GameRules.aiOnlyMode = true;
                GameRules.difficulty = Difficulty.hard;
                controller.gameMode = GameMode.skirmish;
                controller.yioGdxGame.loadingManager.initLevelSize(LevelSize.values()[0]);
                controller.createGameObjects();
                ObjectsLayer layer = controller.objectsLayer;
                YioGdxGame.predictableRandom.setSeed(seed);
                YioGdxGame.random.setSeed(seed * 197L);
                layer.mapGenerator.generateLinkedMap(seed <= 6 ? 0 : 1, 2);
                FractionType opponent = layer.planetsManager.getAliveFraction(FractionType.green);
                if (opponent == null) throw new AssertionError("Benchmark map has no opponent");
                FractionType savvy = swap == 0 ? opponent : FractionType.green;
                FractionType simple = swap == 0 ? FractionType.green : opponent;
                GameRules.aiTypes.put(savvy, AiType.savvy);
                GameRules.aiTypes.put(simple, AiType.simple);
                layer.onEndCreation();
                if (layer.aiManager.entities.size() != 2) throw new AssertionError("Benchmark needs exactly two AI players");
                int tick;
                for (tick = 0; tick < 36000; tick++) {
                    layer.planetsManager.moveActually();
                    layer.linksManager.moveActually();
                    layer.unitManager.moveActually();
                    layer.aiManager.moveActually();
                    if (tick % 60 == 0 && (!alive(layer, savvy) || !alive(layer, simple))) break;
                }
                String winner;
                if (!alive(layer, simple)) { savvyWins++; winner = "savvy"; }
                else if (!alive(layer, savvy)) { simpleWins++; winner = "simple"; }
                else { unresolved++; winner = "unresolved"; }
                result.append("seed=").append(seed).append(" savvy=").append(savvy)
                        .append(" winner=").append(winner).append(" ticks=").append(tick).append('\n');
            }
        }
        result.append("Hard, six tiny and six small maps, each starting colour swapped: Savvy ").append(savvyWins)
                .append(", Simple ").append(simpleWins).append(", unresolved ").append(unresolved).append('\n');
        System.out.print(result);
        return result.toString();
    }

    private static boolean alive(ObjectsLayer layer, FractionType fraction) {
        if (layer.planetsManager.isFractionAlive(fraction)) return true;
        for (Unit unit : layer.unitManager.units) {
            if (unit.isAlive() && unit.fraction == fraction) return true;
        }
        return false;
    }

}
