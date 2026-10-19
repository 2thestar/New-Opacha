package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.ai.AiType;

/** Complete mixed-AI matches exercise weapons, construction and diplomacy together. */
public final class TacticsModeMatches {
    public static String run(GameController c) {
        StringBuilder result = new StringBuilder();
        VelocityManager.getInstance().value = .5f;
        for (PlayType mode : PlayType.values()) for (int seed = 1; seed <= 2; seed++) {
            GameRules.playType = mode; GameRules.aiOnlyMode = true; GameRules.difficulty = Difficulty.hard;
            GameRules.aiTypes.clear(); c.gameMode = GameMode.skirmish;
            c.yioGdxGame.loadingManager.initLevelSize(LevelSize.values()[0]); c.createGameObjects();
            ObjectsLayer layer = c.objectsLayer;
            YioGdxGame.predictableRandom.setSeed(seed * 3127L); YioGdxGame.random.setSeed(seed * 109L);
            layer.mapGenerator.generateLinkedMap(1, 4);
            for (FractionType owner : FractionType.values()) if (owner != FractionType.neutral) GameRules.aiTypes.put(owner, AiType.values()[(owner.ordinal() - 1) % 3]);
            int index = 0; for (Link link : layer.linksManager.links) link.directional = index++ % 4 == 0;
            layer.onEndCreation(); FractionType winner = null;
            int tick;
            for (tick = 0; tick < 60000; tick++) {
                layer.moveActually();
                if (tick % 60 != 0) continue;
                winner = layer.tactics.winner();
                if (winner == null && mode == PlayType.conquest && layer.tactics.alliances.aliveCount() == 1) {
                    for (FractionType owner : FractionType.values()) if (owner != FractionType.neutral && layer.tactics.alliances.alive(owner)) winner = owner;
                }
                if (winner != null) break;
            }
            result.append(mode).append(" seed=").append(seed).append(" winner=").append(winner).append(" ticks=").append(tick).append('\n');
            System.out.print(result.substring(result.lastIndexOf("\n", result.length() - 2) + 1));
            if (winner == null) throw new AssertionError("Mixed AI match stalled: " + mode + " seed " + seed);
        }
        return result.toString();
    }
}
