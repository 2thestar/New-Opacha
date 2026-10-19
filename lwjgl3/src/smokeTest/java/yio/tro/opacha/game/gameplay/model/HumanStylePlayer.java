package yio.tro.opacha.game.gameplay.model;

import java.util.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;

/** Deliberately limited test controller: visible state, one command, reaction delay, half/all sends. */
public final class HumanStylePlayer {
    public enum Style { expand, economy, rush, siege, diplomat, hesitant, planner }
    public final Style style;
    public final FractionType owner;
    public int orders, upgrades, tools, pactActions;
    private final Random random;
    private double nextAction;
    private boolean secondCommand;

    public HumanStylePlayer(Style style, FractionType owner, long seed) {
        this.style = style; this.owner = owner; random = new Random(seed);
        nextAction = style == Style.hesitant ? 4 : 1.5;
    }

    public void move(ObjectsLayer layer) {
        TacticsManager t = layer.tactics;
        if (t.elapsed < nextAction || !t.alliances.alive(owner)) return;
        nextAction = t.elapsed + (style == Style.hesitant ? 3.5 : 1.2) + random.nextDouble() * .8;
        boolean mistake = random.nextDouble() < (style == Style.hesitant ? .18 : .06);
        if (mistake && random.nextBoolean()) return;
        boolean hasPartner = false;
        for (FractionType faction : FractionType.values()) if (t.alliances.areAllied(owner,faction)) hasPartner=true;
        if (style == Style.diplomat && GameRules.playType == PlayType.alliances && t.elapsed > 3 && !hasPartner) {
            for (FractionType other : FractionType.values()) {
                if (other == FractionType.neutral || other == owner || !t.alliances.alive(other)
                        || t.alliances.areAllied(owner, other) || t.alliances.isBroken(owner, other)) continue;
                if (!t.alliances.hasOffer(owner, other) && t.alliances.offer(owner, other)) { pactActions++; return; }
            }
        }
        if (style == Style.siege && weapon(layer)) return;
        if (style == Style.siege && t.elapsed > 3 && dig(layer)) return;
        if (style == Style.planner && rallyForFlagRoute(layer)) return;
        if ((style == Style.economy || style == Style.hesitant || style == Style.planner) && t.elapsed > 3) {
            for (Planet node : layer.planetsManager.planets) {
                if (node.fraction != owner || node.unitsInside < 22 || incomingHostile(node, t) > 0
                        || node.getNumberOfAdjoinedEnemies() > 0 || node.isEconomic()) continue;
                if (layer.planetsManager.canBeUpgradedToEconomic(node)) {
                    layer.planetsManager.applyUpgrade(node, PlanetType.economic); upgrades++; return;
                }
            }
        }
        Planet bestSource = null, bestTarget = null;
        int bestPercent = 100;
        double bestScore = -Double.MAX_VALUE;
        for (Planet source : layer.planetsManager.planets) {
            if (source.fraction != owner || source.unitsInside < 2) continue;
            if (style == Style.economy && t.elapsed < 14 && source.unitsInside < 30) continue;
            for (Link link : source.adjoinedLinks) {
                if (!link.canUse(source, owner)) continue;
                Planet target = link.getOppositePlanet(source);
                if (t.alliances.areAllied(owner, target.fraction)) {
                    if (style == Style.diplomat && incomingHostile(target,t) > target.getFullHp()
                            && source.unitsInside > 15 && bestScore < 90) {
                        bestSource=source; bestTarget=target; bestPercent=50; bestScore=90;
                    }
                    continue;
                }
                boolean friendly = target.fraction == owner;
                double hostile = incomingHostile(source, t);
                double sent = incomingOwn(target);
                int construction = link.isBlueprint() ? link.constructionCost-link.constructionSpent : 0;
                int percent = source.unitsInside / 2 > target.getFullHp() + construction + 5 ? 50 : 100;
                if (style == Style.planner && GameRules.playType == PlayType.home_flags && t.homeFlags[owner.ordinal()] == source
                        && !enemyHome(target, t)) percent = 50;
                if (!RuleFeatures.has(RuleFeatures.HALF)) percent = 100;
                int amount = (int) source.unitsInside * percent / 100;
                double score;
                if (friendly) {
                    int a = frontDistance(source, layer), b = frontDistance(target, layer);
                    if (style == Style.planner && GameRules.isFlagMode()) {
                        a = objectiveDistance(source, layer); b = objectiveDistance(target, layer);
                    }
                    boolean rally = b == a && b < 2 && target.unitsInside > source.unitsInside * 1.2;
                    if ((!rally && b >= a) || source.unitsInside < 8) continue;
                    score = (rally ? 8 : 3) + (a - b) * 4 - hostile;
                } else {
                    double growth = target.isNeutral() ? 0 : (target.isEconomic() ? 2 : 1)
                            * .1 / (7 * Math.max(.00001, link.movementMultiplier * link.speedFrom(source)));
                    if (!mistake && (amount + sent < target.getFullHp() + growth + construction + 2 || sent > target.getFullHp() + growth + construction + 4)) continue;
                    score = 24 - target.getFullHp() * .15 - link.distance / 30 + (target.isEconomic() ? 5 : 0);
                    if (target == t.flagPlanet || enemyHome(target, t)) score += 100;
                    if (style == Style.rush) score += target.isNeutral() ? 0 : 12;
                    if (GameRules.isFlagMode()) score -= objectiveDistance(target, layer) * 8;
                    score -= Math.max(0, hostile - (source.unitsInside - amount)) * .7;
                }
                score += random.nextDouble() * 1.5;
                if (score > bestScore) { bestScore = score; bestSource = source; bestTarget = target; bestPercent = mistake && RuleFeatures.has(RuleFeatures.HALF) ? 50 : percent; }
            }
        }
        if (bestSource != null && layer.planetsManager.sendUnitsPercent(bestSource, bestTarget, bestPercent)) {
            orders++;
            if (style == Style.planner && !secondCommand) {
                secondCommand = true; nextAction = t.elapsed; move(layer); secondCommand = false;
            }
            return;
        }
        if (style == Style.siege || style == Style.rush) dig(layer);
    }

    private boolean weapon(ObjectsLayer layer) {
        TacticsManager t = layer.tactics;
        for (Planet node : layer.planetsManager.planets) {
            if (node.fraction != owner) continue;
            Planet target = null;
            for (Planet other : layer.planetsManager.planets) {
                if (other.isNeutral() || other.getFullHp()==0 || t.alliances.friendly(owner, other.fraction)
                        || node.position.center.distanceTo(other.position.center) > t.artilleryRange()) continue;
                if (other.type==PlanetType.empty && other.getFullHp()<12 && other!=t.flagPlanet && !enemyHome(other,t)) continue;
                if (target == null || other.getFullHp() < target.getFullHp()) target = other;
            }
            if (target != null && node.type == PlanetType.artillery && node.unitsInside >= 30 && t.fire(node, target)) { tools++; return true; }
            if (target != null && target.getFullHp() > 35 && node.unitsInside > 90 && node.type != PlanetType.artillery
                    && t.buildArtillery(node)) { tools++; return true; }
            if (node.isEconomic() && node.unitsInside > 55 && node.bombStock == 0 && t.buyBomb(node)) { tools++; return true; }
        }
        for (Planet depot : layer.planetsManager.planets) {
            if (depot.fraction != owner || depot.bombStock == 0) continue;
            for (Unit enemy : layer.unitManager.units) {
                if (!enemy.isAlive() || t.alliances.friendly(owner, enemy.fraction) || enemy.target.fraction != owner) continue;
                if (t.reservedBombs(enemy.target) < 2 && t.sendBomb(depot, enemy.target)) { tools++; return true; }
            }
        }
        return false;
    }

    /** A visible unfinished shortcut is a saving/combining decision, not a reason to leak its crew elsewhere. */
    private boolean rallyForFlagRoute(ObjectsLayer layer) {
        for (Link route : layer.linksManager.links) {
            if (!route.isBlueprint() || route.builder != owner || route.two != layer.tactics.flagPlanet
                    || route.one.fraction != owner) continue;
            Planet start = route.one;
            double needed = route.constructionCost-route.constructionSpent + route.two.getFullHp()+3;
            if (start.unitsInside >= needed) continue;
            if (incomingOwn(start) + start.unitsInside >= needed) return true;
            for (Link supply : start.adjoinedLinks) {
                if (supply.isBlueprint()) continue;
                Planet rear = supply.getOppositePlanet(start);
                if (rear.fraction == owner && rear.unitsInside >= 8 && incomingHostile(rear,layer.tactics)==0) {
                    if (layer.planetsManager.sendUnitsPercent(rear,start,100)) { orders++; return true; }
                }
            }
            double production = start.isEconomic() ? 2 : 1;
            if ((needed-start.unitsInside)/production <= 8 && incomingHostile(start,layer.tactics)==0) return true;
        }
        return false;
    }

    private boolean dig(ObjectsLayer layer) {
        TacticsManager t = layer.tactics;
        for (Link route : layer.linksManager.links) if (route.isBlueprint() && route.builder == owner) return false;
        for (Planet source : layer.planetsManager.planets) {
            if (source.fraction != owner || source.unitsInside < 110 || incomingHostile(source, t) > 0) continue;
            for (Planet target : layer.planetsManager.planets) {
                if (source == target || source.isLinkedTo(target) || t.alliances.friendly(owner, target.fraction)) continue;
                double distance = source.position.center.distanceTo(target.position.center);
                int cost = (int) Math.ceil(80 * distance / t.normalRouteLength);
                if (distance > 1.5 * t.normalRouteLength || source.unitsInside < cost + target.getFullHp() + 15) continue;
                if (t.createBlueprint(source, target, owner) != null) {
                    layer.planetsManager.sendUnitsPercent(source, target, 100); tools++; orders++; return true;
                }
            }
        }
        return false;
    }

    private boolean enemyHome(Planet node, TacticsManager t) {
        for (FractionType faction : FractionType.values()) if (faction != owner && t.homeFlags[faction.ordinal()] == node) return true;
        return false;
    }
    private double incomingOwn(Planet target) {
        double sum = 0;
        for (Link link : target.adjoinedLinks) for (Unit unit : link.getWalkersToTarget(target)) if (unit.isAlive() && unit.fraction == owner) sum += unit.value;
        return sum;
    }
    private double incomingHostile(Planet target, TacticsManager t) {
        double sum = 0;
        for (Link link : target.adjoinedLinks) for (Unit unit : link.getWalkersToTarget(target)) if (unit.isAlive() && !t.alliances.friendly(owner, unit.fraction)) sum += unit.value;
        return sum;
    }
    private int objectiveDistance(Planet start, ObjectsLayer layer) {
        return distance(start, layer, true);
    }
    private int frontDistance(Planet start, ObjectsLayer layer) {
        return distance(start, layer, false);
    }
    private int distance(Planet start, ObjectsLayer layer, boolean objective) {
        ArrayDeque<Planet> queue = new ArrayDeque<>(); Map<Planet, Integer> depths = new HashMap<>();
        queue.add(start); depths.put(start, 0);
        while (!queue.isEmpty()) {
            Planet at = queue.remove(); int depth = depths.get(at);
            if (objective ? at == layer.tactics.flagPlanet || enemyHome(at, layer.tactics)
                    : at.fraction != owner && !layer.tactics.alliances.areAllied(owner, at.fraction)) return depth;
            for (Link link : at.adjoinedLinks) {
                if (link.isBlueprint()) continue;
                Planet next = link.getOppositePlanet(at);
                if (!depths.containsKey(next)) { depths.put(next, depth + 1); queue.add(next); }
            }
        }
        return 20;
    }
}
