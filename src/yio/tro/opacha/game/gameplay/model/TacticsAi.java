package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.PlayType;
import yio.tro.opacha.game.gameplay.ai.AiType;
import java.util.ArrayList;
import java.util.Random;

/** Shared weapon and diplomacy decisions; personality still controls ordinary troop orders. */
final class TacticsAi {
    static void perform(TacticsManager tactics) {
        for (FractionType owner : FractionType.values()) {
            AiType type = tactics.layer.aiManager.getType(owner);
            if (type == null || !tactics.alliances.alive(owner)) continue;
            Random random = new Random(31L * (long) tactics.elapsed + owner.ordinal() * 997L);
            diplomacy(tactics, owner, type, random);
            weapons(tactics, owner, type);
            if (type == AiType.savvy && tactics.elapsed > 30) shortcut(tactics, owner);
        }
    }

    private static void weapons(TacticsManager t, FractionType owner, AiType type) {
        int guns = 0;
        for (Planet node : t.layer.planetsManager.planets) if (node.fraction == owner && node.type == PlanetType.artillery) guns++;
        for (Planet node : t.layer.planetsManager.planets) {
            if (node.fraction != owner) continue;
            Planet target = null;
            int enemies = 0;
            for (Planet other : t.layer.planetsManager.planets) {
                if (other.isNeutral() || other.getFullHp() == 0 || t.alliances.friendly(owner, other.fraction)
                        || node.position.center.distanceTo(other.position.center) > t.artilleryRange()) continue;
                if (other.type == PlanetType.empty && other.getFullHp() < 12 && other != t.flagPlanet
                        && !isHomeFlag(t, other)) continue;
                enemies++;
                if (target == null || other.getFullHp() < target.getFullHp()) target = other;
            }
            if (node.type == PlanetType.artillery) {
                if (target != null && node.unitsInside >= Math.max(30, t.layer.planetsManager.calculateThreat(node) + 20)) t.fire(node, target);
                if (node.unitsInside >= 80) releaseGunReserves(t, node);
            } else if (!node.isEconomic() && enemies >= 2 && node.unitsInside >= 65
                    && (target == null || !node.isLinkedTo(target) || target.isDefensive()
                        || node.getLink(target).speedFrom(node) < 1)
                    && guns < (type == AiType.savvy ? 2 : 1)) {
                if (t.buildArtillery(node)) guns++;
            }
            if (node.isEconomic() && node.unitsInside >= 45 && t.inventory[owner.ordinal()] < 1) t.buyBomb(node);
        }
        if (t.inventory[owner.ordinal()] == 0) return;
        for (Unit unit : t.layer.unitManager.units) {
            if (!unit.isAlive() || unit.fraction != owner || unit.value < 8 || unit.digger) continue;
            boolean armed = false;
            for (RouteBomb bomb : t.bombs) if (bomb.active && bomb.link == unit.link && bomb.owner == owner) armed = true;
            if (armed) continue;
            for (Unit other : unit.link.getWalkersToTarget(unit.link.getOppositePlanet(unit.target))) {
                if (!other.isAlive() || t.alliances.friendly(owner, other.fraction)) continue;
                float a = unit.acceleratableFactor.value, b = other.acceleratableFactor.value;
                if ((unit.target == unit.link.two && b > a) || (unit.target == unit.link.one && b < a)) {
                    t.placeBomb(unit, owner);
                    return;
                }
            }
        }
    }

    private static boolean isHomeFlag(TacticsManager t, Planet node) {
        for (Planet flag : t.homeFlags) if (flag == node) return true;
        return false;
    }

    private static void releaseGunReserves(TacticsManager t, Planet gun) {
        Planet target = null; int best = Integer.MIN_VALUE;
        for (Link link : gun.adjoinedLinks) {
            if (link.isBlueprint()) continue;
            Planet other = link.getOppositePlanet(gun);
            if (t.alliances.areAllied(gun.fraction, other.fraction)) continue;
            int score;
            if (other.fraction == gun.fraction) {
                int front = other.getNumberOfAdjoinedEnemies() + other.countAdjoinedPlanetsByFraction(FractionType.neutral);
                int ownFront = gun.getNumberOfAdjoinedEnemies() + gun.countAdjoinedPlanetsByFraction(FractionType.neutral);
                if (front <= ownFront) continue;
                score = front;
            } else {
                if (gun.unitsInside / 2 < other.getFullHp() + 12) continue;
                score = 20 + (other == t.flagPlanet || isHomeFlag(t, other) ? 100 : 0);
            }
            if (score > best) { best = score; target = other; }
        }
        if (target != null) t.layer.planetsManager.sendUnitsPercent(gun, target, 50);
    }

    private static void shortcut(TacticsManager t, FractionType owner) {
        for (Link link : t.layer.linksManager.links) if (link.isBlueprint() && link.builder == owner) return;
        for (Planet start : t.layer.planetsManager.planets) {
            if (start.fraction != owner || start.unitsInside < 130 || start.getNumberOfAdjoinedEnemies() > 0) continue;
            Planet target = null;
            double best = Double.MAX_VALUE;
            for (Planet other : t.layer.planetsManager.planets) {
                if (other == start || start.isLinkedTo(other) || t.alliances.friendly(owner, other.fraction)) continue;
                double distance = start.position.center.distanceTo(other.position.center);
                double cost = 80 * distance / t.normalRouteLength;
                if (distance > 1.5 * t.normalRouteLength || start.unitsInside < cost + other.getFullHp() + 20) continue;
                double score = cost + other.getFullHp() - (other.isEconomic() ? 20 : 0);
                if (score < best) { best = score; target = other; }
            }
            if (target == null) continue;
            Link route = t.createBlueprint(start, target, owner);
            t.layer.planetsManager.sendUnitsPercent(start, target, 100);
            start.setAutoTarget(null);
            return;
        }
    }

    private static void diplomacy(TacticsManager t, FractionType owner, AiType type, Random random) {
        if (GameRules.playType != PlayType.alliances || t.elapsed < 8) return;
        AllianceManager pacts = t.alliances;
        for (FractionType other : FractionType.values()) {
            if (other == owner || other == FractionType.neutral || !pacts.alive(other) || pacts.isBroken(owner, other)) continue;
            double own = t.power(owner), partner = t.power(other), enemy = 0;
            int allies = 0;
            for (FractionType outsider : FractionType.values()) {
                if (outsider == FractionType.neutral || outsider == owner || outsider == other) continue;
                if (pacts.areAllied(owner, outsider)) allies++;
                if (!pacts.friendly(owner, outsider) && !pacts.friendly(other, outsider))
                    enemy = Math.max(enemy, t.power(outsider));
            }
            if (pacts.areAllied(owner, other)) {
                double age = t.elapsed - pacts.formedAt[owner.ordinal()][other.ordinal()];
                if ((type == AiType.savvy && age > 20 && own > 2 * partner && enemy < own * .5)
                        || (type == AiType.silly && age > 12 && random.nextDouble() < .04)) pacts.breakPact(owner, other);
                continue;
            }
            if (pacts.aliveCount() < 3) {
                pacts.offers[other.ordinal()][owner.ordinal()] = false;
                continue;
            }
            // A pact needs a credible common rival. One partner keeps rival coalitions
            // possible instead of everyone accepting everyone at the first AI tick.
            boolean useful = allies == 0 && enemy > .7 * own && enemy > .35 * partner;
            if (type == AiType.silly) useful = useful && random.nextBoolean();
            if (pacts.hasOffer(other, owner)) {
                if (useful) pacts.offer(owner, other);
                else pacts.offers[other.ordinal()][owner.ordinal()] = false;
            } else if (useful && enemy > own * 1.25 && !pacts.hasOffer(owner, other)) {
                pacts.offer(owner, other);
            }
        }
    }
}
