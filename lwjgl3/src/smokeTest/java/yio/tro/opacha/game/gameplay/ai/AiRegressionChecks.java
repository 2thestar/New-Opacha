package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.RefreshRateDetector;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.stuff.CircleYio;
import yio.tro.opacha.stuff.GraphicsYio;
import java.util.*;

/** Reproductions of bad decisions, checked against the production simulation. */
public final class AiRegressionChecks {
    private final GameController controller;
    private ObjectsLayer layer;
    private int checks;
    private final List<String> failures = new ArrayList<>();

    private AiRegressionChecks(GameController controller) { this.controller = controller; }

    public static void run(GameController controller) { new AiRegressionChecks(controller).run(); }

    private void run() {
        scenario("Simple counts real commands", this::commandBudget);
        scenario("Simple waits for a winning force", this::travelGrowth);
        scenario("Simple avoids duplicate attacks", this::duplicateAttack);
        scenario("Supplies work when Grow is disabled", this::disabledGrowth);
        scenario("Late waves cannot defend an earlier attack", this::lateReinforcement);
        scenario("Partial reinforcement saves a threatened node", this::partialRescue);
        scenario("Supply follows the battle rather than a dead end", this::strategicSupply);
        scenario("AI timing follows game speed and refresh rate", this::cadence);
        scenario("Late friendly supplies do not erase an immediate threat", this::lateFriendlyDefense);
        scenario("Rescue orders must arrive before the node falls", this::lateRescue);
        scenario("A winning flag capture comes before ordinary expansion", this::objectiveCapture);
        scenario("Simple can expand while keeping its starting flag reserve", this::simpleFlagExpansion);
        System.out.println("AI REGRESSIONS: " + checks + " passed, " + failures.size() + " failed");
        if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }

    private void scenario(String name, Runnable test) {
        try { test.run(); checks++; System.out.println("PASS: " + name); }
        catch (AssertionError error) { failures.add(name + ": " + error.getMessage()); System.out.println("FAIL: " + failures.get(failures.size() - 1)); }
    }

    private void reset() {
        yio.tro.opacha.SettingsManager.getInstance().soundEnabled = false;
        GameRules.aiOnlyMode = false; GameRules.difficulty = Difficulty.hard;
        GameRules.playType = PlayType.conquest; GameRules.features = RuleFeatures.ALL;
        GameRules.aiTypes.clear();
        RefreshRateDetector.getInstance().multiplier = 1;
        VelocityManager.getInstance().value = 1;
        controller.gameMode = GameMode.skirmish;
        controller.createGameObjects(); layer = controller.objectsLayer;
    }

    private Planet planet(FractionType owner, int troops, double x, double y) {
        CircleYio c = new CircleYio(); c.setRadius(layer.planetsManager.getPlanetRadius());
        c.center.set(x * GraphicsYio.width, y * GraphicsYio.width);
        Planet p = layer.planetsManager.spawnPlanet(c, owner); p.setUnitsInside(troops); return p;
    }

    private Link link(Planet a, Planet b) { return layer.linksManager.addLink(a, b); }
    private AbstractAiEntity ai(boolean simple) {
        AbstractAiEntity ai = simple ? new AiCustomEntity(layer) : new AiSavvyEntity(layer);
        ai.setFractionType(FractionType.red); ai.setDifficulty(Difficulty.hard);
        ai.currentAllowedActionCount = 2; return ai;
    }
    private void advance(int ticks) {
        for (int i = 0; i < ticks; i++) {
            layer.planetsManager.moveActually(); layer.linksManager.moveActually(); layer.unitManager.moveActually();
        }
    }
    private void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    private void commandBudget() {
        reset();
        Planet source = planet(FractionType.red, 0, .2, .5);
        source.setType(PlanetType.economic);
        Planet front = planet(FractionType.red, 0, .4, .5); front.setType(PlanetType.economic);
        link(source, front); link(front, planet(FractionType.neutral, 3, .6, .5));
        source.setAutoTarget(front);
        AbstractAiEntity ai = ai(true); ai.perform();
        check(ai.currentAllowedActionCount == 2 - layer.unitManager.units.size(), "Empty launches spent commands");
    }
    private void travelGrowth() {
        reset();
        Planet source = planet(FractionType.red, 18, .2, .5);
        Planet target = planet(FractionType.green, 10, .8, .5);
        target.setType(PlanetType.economic);
        Link l = link(target, source); l.directional = true;
        ai(true).perform();
        check(layer.unitManager.units.isEmpty(), "Sent 18 against a target that grows past 40 before arrival");
    }
    private void duplicateAttack() {
        reset();
        Planet source = planet(FractionType.red, 100, .2, .5);
        Planet target = planet(FractionType.neutral, 3, .4, .5);
        source.setType(PlanetType.economic); link(source, target);
        layer.planetsManager.sendUnitsPercent(source, target, 25);
        ai(true).perform();
        check(layer.unitManager.units.size() == 1, "A second army was sent to an already secured neutral");
    }
    private void disabledGrowth() {
        reset(); GameRules.features = 0;
        Planet rear = planet(FractionType.red, 8, .2, .5);
        Planet front = planet(FractionType.red, 2, .4, .5);
        Planet enemy = planet(FractionType.green, 100, .6, .5);
        link(rear, front); link(front, enemy);
        ai(false).perform();
        check(layer.unitManager.units.size() == 1, "Saved troops for an unavailable upgrade");
    }
    private void lateReinforcement() {
        reset();
        Planet source = planet(FractionType.red, 40, .2, .5); source.setType(PlanetType.economic);
        Planet enemy = planet(FractionType.green, 1, .4, .5); enemy.setCurrentDefense(0);
        Planet distant = planet(FractionType.green, 100, .95, .5);
        link(source, enemy); Link slow = link(distant, enemy); slow.directional = true;
        // Make the defender reinforcement much slower than our nearby capture.
        slow.movementMultiplier *= .1f;
        layer.planetsManager.sendUnitsPercent(distant, enemy, 100);
        ai(false).perform();
        check(layer.unitManager.units.size() == 2, "A distant reinforcement prevented an immediate winning capture");
        advance(150);
        check(enemy.fraction == FractionType.red, "The supposedly winning capture did not win");
    }
    private void partialRescue() {
        reset();
        Planet donor = planet(FractionType.red, 8, .4, .5); donor.setType(PlanetType.economic);
        Planet front = planet(FractionType.red, 1, .5, .5); front.setCurrentDefense(0);
        Planet enemy = planet(FractionType.green, 16, .5, .7);
        link(donor, front); link(front, enemy);
        link(donor, planet(FractionType.neutral, 500, .2, .5));
        layer.planetsManager.sendUnitsPercent(enemy, front, 100);
        ai(false).perform();
        check(layer.unitManager.units.size() == 2, "Refused useful help because it could not cover the whole deficit");
        advance(30);
        // A second nearby contribution finishes the rescue before the attack arrives.
        donor.setUnitsInside(8); ai(false).perform(); advance(100);
        check(front.fraction == FractionType.red, "Timely contributions did not preserve the node");
    }
    private void strategicSupply() {
        reset();
        Planet front = planet(FractionType.red, 40, .4, .5); front.setType(PlanetType.economic);
        Planet deadEnd = planet(FractionType.red, 100, .2, .5); deadEnd.setType(PlanetType.economic);
        Planet enemy = planet(FractionType.green, 500, .6, .5);
        Planet neutral = planet(FractionType.neutral, 500, .1, .5);
        link(front, deadEnd); link(front, enemy); link(deadEnd, neutral);
        ai(false).perform();
        check(deadEnd.unitsInside < 100 && front.unitsInside == 40, "The smaller frontline army was sent away from the battle");
    }
    private void cadence() {
        reset();
        for (int hz : new int[]{60, 120, 240}) for (float speed : new float[]{.5f, 1, 2}) {
            RefreshRateDetector.getInstance().multiplier = 60f / hz;
            VelocityManager.getInstance().value = speed;
            CountingAi ai = new CountingAi(layer); ai.setDifficulty(Difficulty.hard);
            int frames = Math.round(20 * hz / speed);
            for (int i = 0; i < frames; i++) ai.moveActually();
            check(ai.actions >= 36 && ai.actions <= 42, "20 game seconds produced " + ai.actions + " commands at " + hz + "Hz / " + speed + "x");
        }
        RefreshRateDetector.getInstance().multiplier = 1; VelocityManager.getInstance().value = 1;
    }
    private void lateFriendlyDefense() {
        reset();
        Planet source = planet(FractionType.red, 80, .3, .5); source.setType(PlanetType.economic);
        Planet neutral = planet(FractionType.neutral, 30, .5, .5);
        Planet enemy = planet(FractionType.green, 70, .3, .7);
        Planet donor = planet(FractionType.red, 100, .95, .5); donor.setType(PlanetType.economic);
        link(source, neutral); link(source, enemy); Link slow = link(donor, source); slow.movementMultiplier *= .1f;
        layer.planetsManager.sendUnitsPercent(enemy, source, 100);
        layer.planetsManager.sendUnitsPercent(donor, source, 100);
        ai(false).perform();
        check(source.unitsInside == 80, "Spent reserves on the strength of a reinforcement arriving after the enemy");
        advance(150);
        check(source.fraction == FractionType.red, "The reserve did not survive the visible attack");
    }
    private void lateRescue() {
        reset();
        Planet donor = planet(FractionType.red, 80, .1, .5); donor.setType(PlanetType.economic);
        Planet front = planet(FractionType.red, 1, .8, .5); front.setCurrentDefense(0);
        Planet enemy = planet(FractionType.green, 30, .8, .6);
        link(donor, front); link(front, enemy);
        layer.planetsManager.sendUnitsPercent(enemy, front, 100);
        ai(false).perform();
        check(layer.unitManager.units.size() == 1, "Reinforced a node that will be lost before help arrives");
    }
    private void objectiveCapture() {
        reset(); GameRules.playType = PlayType.capture_flag;
        Planet source = planet(FractionType.red, 200, .3, .5); source.setType(PlanetType.economic);
        Planet flag = planet(FractionType.neutral, 50, .5, .5);
        Planet neutral = planet(FractionType.neutral, 3, .3, .7);
        link(source, flag); link(source, neutral); layer.tactics.flagPlanet = flag;
        ai(false).perform(); advance(150);
        check(layer.tactics.flagWinner == FractionType.red, "Chose an ordinary capture instead of winning");
    }
    private void simpleFlagExpansion() {
        reset(); GameRules.playType = PlayType.home_flags;
        Planet source = planet(FractionType.red, 20, .3, .5);
        source.setType(PlanetType.economic);
        Planet target = planet(FractionType.neutral, 3, .5, .5);
        link(source, target); layer.tactics.homeFlags[FractionType.red.ordinal()] = source;
        ai(true).perform();
        check(layer.unitManager.units.size() == 1 && source.unitsInside >= 8,
                "Full-army preference prevented expansion or emptied the starting flag");
        advance(150);
        check(target.fraction == FractionType.red, "The reserved expansion did not capture its target");
    }
    private static final class CountingAi extends AbstractAiEntity {
        int actions;
        CountingAi(ObjectsLayer layer) { super(layer); }
        @Override protected void perform() { while (isActionAllowed()) { onActionPerformed(); actions++; } }
    }
}
