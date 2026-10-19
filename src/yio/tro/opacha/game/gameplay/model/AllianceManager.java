package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.PlayType;
import yio.tro.opacha.game.gameplay.ObjectsLayer;

/** Pairwise pacts; friendship is not transitive. A broken pact cannot be renewed. */
public final class AllianceManager {
    public final boolean[][] offers = new boolean[5][5];
    public final boolean[][] allied = new boolean[5][5];
    public final boolean[][] broken = new boolean[5][5];
    public final double[][] formedAt = new double[5][5];
    private final ObjectsLayer layer;

    public AllianceManager(ObjectsLayer layer) { this.layer = layer; }

    private boolean pair(FractionType a, FractionType b) {
        return a != null && b != null && a != b && a != FractionType.neutral && b != FractionType.neutral;
    }

    public boolean areAllied(FractionType a, FractionType b) {
        return pair(a, b) && GameRules.playType == PlayType.alliances && allied[a.ordinal()][b.ordinal()];
    }

    public boolean friendly(FractionType a, FractionType b) {
        return a == b || areAllied(a, b);
    }

    public boolean offer(FractionType from, FractionType to) {
        if (!pair(from, to) || GameRules.playType != PlayType.alliances || isBroken(from, to)
                || areAllied(from, to) || !alive(from) || !alive(to)) return false;
        boolean newOffer = !offers[from.ordinal()][to.ordinal()];
        offers[from.ordinal()][to.ordinal()] = true;
        if (offers[to.ordinal()][from.ordinal()]) {
            allied[from.ordinal()][to.ordinal()] = allied[to.ordinal()][from.ordinal()] = true;
            formedAt[from.ordinal()][to.ordinal()] = formedAt[to.ordinal()][from.ordinal()] = layer.tactics.elapsed;
            offers[from.ordinal()][to.ordinal()] = offers[to.ordinal()][from.ordinal()] = false;
            announce(from, to, "pact_formed");
        } else if (newOffer && to == FractionType.green) {
            announce(from, to, "pact_received");
        }
        return true;
    }

    public boolean hasOffer(FractionType from, FractionType to) {
        return pair(from, to) && offers[from.ordinal()][to.ordinal()];
    }

    public boolean isBroken(FractionType a, FractionType b) {
        return pair(a, b) && broken[a.ordinal()][b.ordinal()];
    }

    public boolean breakPact(FractionType a, FractionType b) {
        if (!areAllied(a, b)) return false;
        allied[a.ordinal()][b.ordinal()] = allied[b.ordinal()][a.ordinal()] = false;
        broken[a.ordinal()][b.ordinal()] = broken[b.ordinal()][a.ordinal()] = true;
        offers[a.ordinal()][b.ordinal()] = offers[b.ordinal()][a.ordinal()] = false;
        announce(a, b, "pact_ended");
        return true;
    }

    private void announce(FractionType a, FractionType b, String key) {
        if (GameRules.aiOnlyMode || (a != FractionType.green && b != FractionType.green)) return;
        yio.tro.opacha.menu.LanguagesManager language = yio.tro.opacha.menu.LanguagesManager.getInstance();
        FractionType other = a == FractionType.green ? b : a;
        yio.tro.opacha.menu.scenes.Scenes.notification.show(language.getString(other.toString()) + ": " + language.getString(key));
    }

    public boolean alive(FractionType fraction) {
        if (layer.planetsManager.isFractionAlive(fraction)) return true;
        for (Unit unit : layer.unitManager.units) if (unit.isAlive() && unit.fraction == fraction) return true;
        return false;
    }

    public int aliveCount() {
        int count = 0;
        for (FractionType fraction : FractionType.values()) if (fraction != FractionType.neutral && alive(fraction)) count++;
        return count;
    }

    public boolean peacefulSurvivors() {
        for (FractionType a : FractionType.values()) {
            if (a == FractionType.neutral || !alive(a)) continue;
            for (FractionType b : FractionType.values()) {
                if (b == FractionType.neutral || a == b || !alive(b)) continue;
                if (!areAllied(a, b)) return false;
            }
        }
        return aliveCount() > 0;
    }
}
