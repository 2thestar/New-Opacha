package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.game.Difficulty;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.PlayType;
import yio.tro.opacha.game.RuleFeatures;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import java.util.*;

/** Scores visible opportunities; spends the same action allowance as the original AI. */
public class AiSavvyEntity extends AbstractAiEntity {
    private static final int[] PERCENTAGES = {25, 50, 100};
    private final Set<Planet> usedSources = new HashSet<>();
    private final Map<Planet, Double> frontDistance = new HashMap<>();

    public AiSavvyEntity(ObjectsLayer layer) {
        super(layer);
    }

    protected int[] attackPercentages() { return PERCENTAGES; }

    protected boolean replacePlannedMove() { return false; }

    protected boolean issueOrder(Planet source, Planet target, int percent) {
        if (!isActionAllowed() || source == null || target == null || source.fraction != fractionType
                || source.type == PlanetType.artillery || source.getLink(target) == null
                || source.getLink(target).isBlueprint() || amount(source, percent) < 1) return false;
        usedSources.add(source);
        if (replacePlannedMove()) return true;
        if (!getPlanetsManager().sendUnitsPercent(source, target, percent)) return false;
        onActionPerformed();
        return true;
    }

    protected void markSourceUsed(Planet source) { usedSources.add(source); }

    @Override
    protected void perform() {
        usedSources.clear();
        updateFrontDistances();
        // Explicit orders keep reserves intact; automatic full-garrison routes would undo them.
        for (Planet planet : getPlanetsList()) {
            if (!isBeyondCommand(planet)) planet.setAutoTarget(null);
        }
        while (isActionAllowed()) {
            Plan best = choosePlan();
            if (best == null) return;
            best.execute();
        }
    }

    private Plan choosePlan() {
        Plan best = null;
        for (Planet source : getPlanetsList()) {
            if (!available(source)) continue;
            best = better(best, upgradePlan(source));
            for (Link link : source.adjoinedLinks) {
                if (link.isBlueprint()) continue;
                Planet target = link.getOppositePlanet(source);
                if (target.fraction == fractionType) {
                    best = better(best, supportPlan(source, target));
                } else if (!objectsLayer.tactics.alliances.areAllied(fractionType, target.fraction)) {
                    best = better(best, attackPlan(source, target));
                }
            }
        }
        if (difficulty == Difficulty.hard && currentAllowedActionCount >= 2) {
            for (Planet target : getPlanetsList()) {
                if (objectsLayer.tactics.alliances.friendly(fractionType, target.fraction)) continue;
                best = better(best, coordinatedPlan(target));
            }
        }
        return best;
    }

    private boolean available(Planet planet) {
        return !isBeyondCommand(planet) && planet.type != PlanetType.artillery && !usedSources.contains(planet);
    }

    private Plan better(Plan one, Plan two) {
        return two != null && (one == null || two.score > one.score) ? two : one;
    }

    private Plan attackPlan(Planet source, Planet target) {
        Link link = source.getLink(target);
        int required = requiredAttack(target, link);
        if (required <= 0) return null; // A friendly wave is already sufficient.
        for (int percent : attackPercentages()) {
            int amount = amount(source, percent);
            if (amount < required || !canSpare(source, amount, target)) continue;
            return new Plan(source, target, percent, attackScore(target, amount, link) - exposureCost(source, amount, target));
        }
        return null;
    }

    private Plan coordinatedPlan(Planet target) {
        Plan best = null;
        ArrayList<Planet> sources = new ArrayList<>();
        for (Link link : target.adjoinedLinks) {
            if (link.isBlueprint()) continue;
            Planet source = link.getOppositePlanet(target);
            if (available(source) && source.unitsInside >= 1) sources.add(source);
        }
        for (int i = 0; i < sources.size(); i++) {
            for (int j = i + 1; j < sources.size(); j++) {
                Planet a = sources.get(i), b = sources.get(j);
                Link la = a.getLink(target), lb = b.getLink(target);
                // Large arrival gaps turn a pincer into two losing attacks. Build up instead.
                double ta = 1 / (la.movementMultiplier * la.speedFrom(a));
                double tb = 1 / (lb.movementMultiplier * lb.speedFrom(b));
                if (Math.max(ta, tb) > 1.5 * Math.min(ta, tb)) continue;
                Link slower = ta > tb ? la : lb;
                int required = requiredAttack(target, slower)
                        + opposingWave(target, la) + opposingWave(target, lb) - opposingWave(target, slower);
                if (required <= 0) continue;
                for (int pa : attackPercentages()) {
                    int va = amount(a, pa);
                    if (va < 1 || !canSpare(a, va, target)) continue;
                    for (int pb : attackPercentages()) {
                        int vb = amount(b, pb);
                        if (vb < 1 || va + vb < required || !canSpare(b, vb, target)) continue;
                        double score = attackScore(target, va + vb, slower) - 6
                                - exposureCost(a, va, target) - exposureCost(b, vb, target);
                        Plan candidate = new Plan(a, target, pa, score);
                        candidate.second = b;
                        candidate.secondPercent = pb;
                        best = better(best, candidate);
                    }
                }
            }
        }
        return best;
    }

    private int requiredAttack(Planet target, Link journey) {
        // Match the actual refresh-adjusted RepeatYio interval, including its
        // countdown's extra step. Movement and regeneration share game speed.
        double steps = journeySteps(journey, journey.getOppositePlanet(target));
        double refresh = yio.tro.opacha.RefreshRateDetector.getInstance().multiplier;
        double regenerationInterval = ((int) (6 / refresh) + 1) * refresh;
        double production = target.isNeutral() ? 0 : steps * 0.1 / regenerationInterval * (target.isEconomic() ? 2 : 1);
        double defenseGrowth = target.isEconomic() ? 0 : Math.min(
                Math.max(0, target.maxDefense - target.currentDefense), steps * 0.1 / regenerationInterval * (target.isDefensive() ? 3 : 1));
        int defenders = target.getFullHp() + (int) Math.ceil(production + defenseGrowth);
        defenders += incoming(target, target.fraction, steps);
        defenders += opposingWave(target, journey);
        return defenders + (target.isNeutral() && target.unitsInside >= 1 ? 0 : 3)
                - incoming(target, fractionType, steps);
    }

    private double journeySteps(Link link, Planet from) {
        return 1 / Math.max(0.00001, link.movementMultiplier * link.speedFrom(from));
    }

    private double arrivalSteps(Unit unit, Planet target) {
        double remaining = target == unit.link.two ? 1 - unit.acceleratableFactor.value : unit.acceleratableFactor.value;
        return remaining * journeySteps(unit.link, unit.link.getOppositePlanet(target));
    }

    private int opposingWave(Planet target, Link journey) {
        int value = 0;
        for (Unit unit : journey.getWalkersToTarget(journey.getOppositePlanet(target))) {
            if (unit.isAlive() && !objectsLayer.tactics.alliances.friendly(unit.fraction, fractionType)) value += unit.value;
        }
        return value;
    }

    private double attackScore(Planet target, int troops, Link link) {
        boolean objective = objectsLayer.tactics.flagPlanet == target;
        for (Planet flag : objectsLayer.tactics.homeFlags) if (flag == target && target.fraction != fractionType) objective = true;
        return (objective ? 250 : 0) + 35 + (target.isNeutral() ? 0 : 8) + (target.isEconomic() ? 10 : 0)
                + 1.5 * target.adjoinedLinks.size() - 0.12 * troops - 0.015 / Math.max(0.00001, link.movementMultiplier);
    }

    private int incoming(Planet planet, FractionType owner) {
        return incoming(planet, owner, Double.POSITIVE_INFINITY);
    }

    private int incoming(Planet planet, FractionType owner, double before) {
        int value = 0;
        for (Link link : planet.adjoinedLinks) {
            for (Unit unit : link.getWalkersToTarget(planet)) {
                if (!unit.isAlive() || arrivalSteps(unit, planet) > before) continue;
                if (unit.fraction == owner) value += unit.value;
                else if (objectsLayer.tactics.alliances.areAllied(owner, unit.fraction)) value += (int) (.7 * unit.value);
            }
        }
        return value;
    }

    private int hostileIncoming(Planet planet) {
        int value = 0;
        for (Link link : planet.adjoinedLinks) {
            for (Unit unit : link.getWalkersToTarget(planet)) {
                if (unit.isAlive() && !objectsLayer.tactics.alliances.friendly(unit.fraction, fractionType)) value += unit.value;
            }
        }
        return value;
    }

    private int reserve(Planet source, Planet attackedTarget) {
        int home = GameRules.playType == PlayType.home_flags
                && objectsLayer.tactics.homeFlags[fractionType.ordinal()] == source ? 8 : 0;
        int needed = home;
        int balance = (int) source.currentDefense;
        for (int wave : arrivalWaves(source).values()) {
            balance += wave;
            needed = Math.max(needed, 2 - balance);
        }
        return needed;
    }

    private SortedMap<Double, Integer> arrivalWaves(Planet planet) {
        SortedMap<Double, Integer> waves = new TreeMap<>();
        for (Link link : planet.adjoinedLinks) for (Unit unit : link.getWalkersToTarget(planet)) {
            if (!unit.isAlive()) continue;
            int value = unit.fraction == fractionType ? unit.value
                    : objectsLayer.tactics.alliances.areAllied(unit.fraction, fractionType)
                    ? (int) (.7 * unit.value) : -unit.value;
            double arrival = arrivalSteps(unit, planet);
            waves.put(arrival, waves.getOrDefault(arrival, 0) + value);
        }
        return waves;
    }

    private double lossDeadline(Planet planet) {
        int balance = planet.getFullHp();
        for (Map.Entry<Double, Integer> wave : arrivalWaves(planet).entrySet()) {
            balance += wave.getValue();
            if (balance <= 0 && wave.getValue() < 0) return wave.getKey();
        }
        return Double.POSITIVE_INFINITY;
    }

    private double exposureCost(Planet source, int troops, Planet attackedTarget) {
        int nearbyThreat = 0;
        for (Link link : source.adjoinedLinks) {
            Planet other = link.getOppositePlanet(source);
            if (link.isBlueprint() || other == attackedTarget || other.isNeutral() || objectsLayer.tactics.alliances.friendly(other.fraction, fractionType)) continue;
            nearbyThreat = Math.max(nearbyThreat, (int) Math.ceil(other.unitsInside * 0.3));
        }
        // A possible attack is a risk to price, not a reason to delay every expansion
        // until a discrete 50% order can also leave a reserve. Actual waves are mandatory.
        int remaining = (int) source.unitsInside - troops + (int) source.currentDefense;
        return Math.max(0, nearbyThreat - remaining) * 0.6;
    }

    private boolean canSpare(Planet source, int troops, Planet attackedTarget) {
        return (int) source.unitsInside - troops >= reserve(source, attackedTarget);
    }

    private int amount(Planet source, int percent) {
        return (int) source.unitsInside * percent / 100;
    }

    private Plan supportPlan(Planet source, Planet target) {
        double deadline = lossDeadline(target);
        if (!Double.isInfinite(deadline)) {
            if (journeySteps(source.getLink(target), source) >= deadline) return null;
            int deficit = reserve(target, null) - (int) target.unitsInside;
            Plan partial = null;
            for (int percent : PERCENTAGES) {
                int troops = amount(source, percent);
                if (troops < 1 || !canSpare(source, troops, null)) continue;
                Plan help = new Plan(source, target, percent, 100 + Math.min(deficit, troops) - 0.1 * troops);
                if (troops >= deficit) return help;
                partial = help;
            }
            if (partial != null) return partial;
        }
        double sourceDistance = frontDistance.get(source), targetDistance = frontDistance.get(target);
        if (Double.isInfinite(sourceDistance) || sourceDistance < targetDistance) return null;
        boolean rally = Math.abs(sourceDistance - targetDistance) < .01;
        if (rally && (getPlanetsManager().numberOfAdjoinedEnemyPlanets(target) == 0
                || target.unitsInside + incoming(target, fractionType) <= 1.2 * source.unitsInside)) return null;
        // Move surplus through owned territory only, always closer to the front.
        for (int percent : new int[]{100, 50, 25}) {
            int troops = amount(source, percent);
            if (troops < 4 || !canSpare(source, troops, null)) continue;
            if (RuleFeatures.has(RuleFeatures.GROW) && difficulty != Difficulty.easy && !source.isEconomic()
                    && getPlanetsManager().numberOfAdjoinedEnemyPlanets(source) == 0
                    && (int) source.unitsInside - troops < PlanetsManager.ECONOMIC_UPGRADE_PRICE) continue;
            return new Plan(source, target, percent, (rally ? 21 : 12) + Math.min(10, troops * 0.1)
                    - exposureCost(source, troops, null));
        }
        return null;
    }

    private Plan upgradePlan(Planet source) {
        if (difficulty == Difficulty.easy || source.unitsInside < 10 + reserve(source, null)) return null;
        int enemies = getPlanetsManager().numberOfAdjoinedEnemyPlanets(source);
        if (yio.tro.opacha.game.RuleFeatures.has(yio.tro.opacha.game.RuleFeatures.DEFEND)
                && !source.isDefensive() && enemies >= 2 && source.unitsInside >= 20) {
            Plan plan = new Plan(source, null, 0, 16);
            plan.upgrade = PlanetType.defensive;
            return plan;
        }
        if (yio.tro.opacha.game.RuleFeatures.has(yio.tro.opacha.game.RuleFeatures.GROW)
                && !source.isEconomic() && enemies == 0 && hostileIncoming(source) == 0) {
            // Invest early: production doubles permanently for ten troops. Waiting
            // for a larger idle garrison loses the opening economy race.
            Plan plan = new Plan(source, null, 0, 50);
            plan.upgrade = PlanetType.economic;
            return plan;
        }
        return null;
    }

    private void updateFrontDistances() {
        // Travel distance to the actual rival or objective. Neutral dead ends
        // must not attract supplies just because they are also frontiers.
        frontDistance.clear();
        Set<Planet> unvisited = new HashSet<>(getPlanetsList());
        for (Planet planet : getPlanetsList()) {
            boolean goal = !planet.isNeutral() && !objectsLayer.tactics.alliances.friendly(fractionType, planet.fraction);
            if (GameRules.playType == PlayType.capture_flag) goal = planet == objectsLayer.tactics.flagPlanet;
            if (GameRules.playType == PlayType.home_flags) {
                goal = false;
                for (Planet flag : objectsLayer.tactics.homeFlags)
                    if (flag == planet && !objectsLayer.tactics.alliances.friendly(fractionType, planet.fraction)) goal = true;
            }
            frontDistance.put(planet, goal ? 0.0 : Double.POSITIVE_INFINITY);
        }
        while (!unvisited.isEmpty()) {
            Planet planet = null;
            for (Planet candidate : getPlanetsList()) {
                if (!unvisited.contains(candidate)) continue;
                if (planet == null || frontDistance.get(candidate) < frontDistance.get(planet)) planet = candidate;
            }
            unvisited.remove(planet);
            if (Double.isInfinite(frontDistance.get(planet))) break;
            for (Link link : planet.adjoinedLinks) {
                if (link.isBlueprint()) continue;
                Planet other = link.getOppositePlanet(planet);
                if (!unvisited.contains(other)) continue;
                double distance = frontDistance.get(planet) + journeySteps(link, other);
                if (distance < frontDistance.get(other)) frontDistance.put(other, distance);
            }
        }
    }

    private class Plan {
        final Planet source, target;
        final int percent;
        final double score;
        Planet second;
        int secondPercent;
        PlanetType upgrade;

        Plan(Planet source, Planet target, int percent, double score) {
            this.source = source;
            this.target = target;
            this.percent = percent;
            this.score = score;
        }

        void execute() {
            usedSources.add(source);
            if (upgrade != null) {
                if (replacePlannedMove()) return;
                getPlanetsManager().applyUpgrade(source, upgrade);
                onActionPerformed();
                return;
            }
            issueOrder(source, target, percent);
            if (second != null) {
                usedSources.add(second);
                issueOrder(second, target, secondPercent);
            }
        }
    }
}
