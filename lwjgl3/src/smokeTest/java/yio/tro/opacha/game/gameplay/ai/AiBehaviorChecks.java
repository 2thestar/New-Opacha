package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.stuff.CircleYio;
import yio.tro.opacha.stuff.GraphicsYio;
import java.util.*;

/** Scenarios run against the actual troop, link, combat and import/export code. */
public final class AiBehaviorChecks {
    private final GameController controller;
    private ObjectsLayer layer;
    private int checks;

    private AiBehaviorChecks(GameController controller) { this.controller = controller; }

    public static void run(GameController controller) {
        new AiBehaviorChecks(controller).run();
    }

    private void run() {
        percentageCapture(100, 3, 25);
        percentageCapture(40, 12, 20);
        percentageCapture(20, 12, 20);
        originalFullGarrison();
        openingEconomy();
        preserveGrowthBudget();
        counterAttack();
        reserveUnderAttack();
        coordinatedCapture();
        avoidDuplicateAttack();
        forwardSupply();
        sillyCommands();
        serialization();
        System.out.println("AI SCENARIOS PASSED: " + checks + " assertions");
    }

    private void reset() {
        GameRules.aiOnlyMode = false;
        GameRules.difficulty = Difficulty.hard;
        GameRules.aiTypes.clear();
        controller.gameMode = GameMode.skirmish;
        controller.createGameObjects();
        layer = controller.objectsLayer;
    }

    private Planet planet(FractionType fraction, int troops, double x, double y) {
        CircleYio circle = new CircleYio();
        circle.setRadius(layer.planetsManager.getPlanetRadius());
        circle.center.set(x * GraphicsYio.width, y * GraphicsYio.width);
        Planet planet = layer.planetsManager.spawnPlanet(circle, fraction);
        planet.setUnitsInside(troops);
        return planet;
    }

    private void link(Planet a, Planet b) { layer.linksManager.addLink(a, b); }

    private AiSavvyEntity savvy() {
        AiSavvyEntity ai = new AiSavvyEntity(layer);
        ai.setFractionType(FractionType.red);
        ai.setDifficulty(Difficulty.hard);
        ai.currentAllowedActionCount = 2;
        return ai;
    }

    private void percentageCapture(int troops, int defenders, int expected) {
        reset();
        Planet source = planet(FractionType.red, troops, .3, .5);
        source.setType(PlanetType.economic);
        Planet target = planet(FractionType.neutral, defenders, .5, .5);
        link(source, target);
        source.halvedSelectionMode = true;
        savvy().perform();
        check(layer.unitManager.units.size() == 1, "One capture wave expected");
        check(layer.unitManager.units.get(0).value == expected, "Wrong 25/50/100% capture size");
        check((int) source.unitsInside == troops - expected, "Troops must be conserved");
        check(source.halvedSelectionMode, "AI changed human selection state");
        advance(700);
        check(target.fraction == FractionType.red, "Sized wave failed to capture its target");
    }

    private void originalFullGarrison() {
        reset();
        Planet source = planet(FractionType.red, 100, .3, .5);
        Planet target = planet(FractionType.neutral, 3, .5, .5);
        link(source, target);
        AiCustomEntity ai = new AiCustomEntity(layer);
        ai.setFractionType(FractionType.red);
        ai.setDifficulty(Difficulty.easy);
        ai.currentAllowedActionCount = 2;
        ai.perform();
        check(layer.unitManager.units.get(0).value == 100, "Simple must retain the original full-garrison attack");
        source.setUnitsInside(11);
        source.halvedSelectionMode = true;
        layer.planetsManager.sendUnits(source, target);
        check(layer.unitManager.units.get(1).value == 5, "Human half-send changed");
        check(!layer.planetsManager.sendUnitsPercent(source, source, 100), "Self-orders must be rejected");
        check(!layer.planetsManager.sendUnitsPercent(source, target, 30), "Unsupported percentages must be rejected");
        check(!layer.planetsManager.sendUnitsPercent(null, target, 100), "Null source must be rejected");
    }

    private void reserveUnderAttack() {
        reset();
        Planet source = planet(FractionType.red, 100, .3, .5);
        Planet target = planet(FractionType.neutral, 3, .5, .5);
        Planet enemy = planet(FractionType.green, 80, .3, .7);
        link(source, target); link(source, enemy);
        layer.planetsManager.sendUnitsPercent(enemy, source, 100);
        source.setAutoTarget(target);
        savvy().perform();
        check(source.unitsInside >= 75, "Savvy emptied a threatened node");
        check(source.autoTarget == null, "Automatic send would undo the reserve");
        check(layer.unitManager.units.size() == 2 && layer.unitManager.units.get(1).value == 25,
                "Only the safe quarter should attack");
    }

    private void openingEconomy() {
        reset();
        Planet source = planet(FractionType.red, 10, .3, .5);
        Planet target = planet(FractionType.neutral, 3, .5, .5);
        link(source, target);
        savvy().perform();
        check(source.isEconomic() && source.unitsInside == 0, "Safe opening should invest promptly in doubled production");
    }

    private void preserveGrowthBudget() {
        reset();
        Planet source = planet(FractionType.red, 8, .3, .5);
        Planet front = planet(FractionType.red, 2, .5, .5);
        Planet enemy = planet(FractionType.green, 100, .7, .5);
        link(source, front); link(front, enemy);
        savvy().perform();
        check(source.unitsInside == 8 && layer.unitManager.units.isEmpty(), "Supply must not starve a safe node's upgrade");
    }

    private void counterAttack() {
        reset();
        Planet source = planet(FractionType.red, 100, .3, .5);
        source.setType(PlanetType.economic);
        Planet enemy = planet(FractionType.green, 1, .5, .5);
        enemy.setCurrentDefense(0);
        link(source, enemy);
        savvy().perform();
        check(layer.unitManager.units.size() == 1 && layer.unitManager.units.get(0).value == 25,
                "An exposed enemy should be taken with a quarter, not the full army");
        advance(700);
        check(enemy.fraction == FractionType.red, "Counterattack failed against an exposed enemy");

        reset();
        source = planet(FractionType.red, 40, .3, .5);
        source.setType(PlanetType.economic);
        enemy = planet(FractionType.green, 20, .5, .5);
        enemy.setCurrentDefense(0);
        link(source, enemy);
        layer.planetsManager.sendUnitsPercent(enemy, source, 100);
        savvy().perform();
        check(layer.unitManager.units.size() == 1, "Counterattack ignored enemies that would intercept it on the link");
    }

    private void coordinatedCapture() {
        reset();
        Planet a = planet(FractionType.red, 24, .3, .4);
        Planet b = planet(FractionType.red, 24, .3, .6);
        Planet target = planet(FractionType.green, 30, .5, .5);
        link(a, target); link(b, target);
        AiSavvyEntity ai = savvy();
        ai.perform();
        check(layer.unitManager.units.size() == 2, "Two-node attack not issued");
        check(ai.currentAllowedActionCount == 0, "Pincer must cost two commands");
        advance(700);
        check(target.fraction == FractionType.red, "Coordinated attack did not win its target");
    }

    private void avoidDuplicateAttack() {
        reset();
        Planet source = planet(FractionType.red, 100, .3, .5);
        Planet target = planet(FractionType.neutral, 3, .5, .5);
        link(source, target);
        layer.planetsManager.sendUnitsPercent(source, target, 25);
        savvy().perform();
        check(layer.unitManager.units.size() == 1, "An adequate incoming wave was ignored");
    }

    private void forwardSupply() {
        reset();
        Planet rear = planet(FractionType.red, 100, .3, .5);
        Planet front = planet(FractionType.red, 20, .5, .5);
        Planet enemy = planet(FractionType.green, 100, .7, .5);
        rear.setType(PlanetType.economic);
        link(rear, front); link(front, enemy);
        savvy().perform();
        check(layer.unitManager.units.size() == 1 && rear.unitsInside == 0,
                "Rear troops should move toward the front");
        check(rear.autoTarget == null && front.autoTarget == null, "Savvy must not create supply loops");
    }

    private void sillyCommands() {
        reset();
        Planet source = planet(FractionType.red, 100, .3, .5);
        Planet target = planet(FractionType.green, 3, .5, .5);
        link(source, target);
        TrackingSilly ai = new TrackingSilly(layer);
        ai.setFractionType(FractionType.red);
        int partials = 0;
        for (int i = 0; i < 10000; i++) {
            layer.unitManager.defaultValues();
            source.adjoinedLinks.get(0).upWalkers.clear();
            source.adjoinedLinks.get(0).downWalkers.clear();
            source.setUnitsInside(100);
            ai.currentAllowedActionCount = 2;
            ai.orderLaunch(source, target);
            check(layer.unitManager.units.size() == 1, "Silly order must be legal and singular");
            int amount = layer.unitManager.units.get(0).value;
            check(amount == 25 || amount == 50 || amount == 100, "Silly order has an illegal size");
            check(source.unitsInside + amount == 100 && ai.currentAllowedActionCount == 1,
                    "Silly must conserve troops and spend one command");
            if (amount < 100) partials++;
        }
        check(ai.erratic >= 1850 && ai.erratic <= 2150, "Erratic decision rate is not about 20%");
        check(partials > 1000, "Silly did not vary its orders");
        System.out.println("Silly: " + ai.erratic + "/10000 commands replaced by random legal orders");
    }

    private void serialization() {
        reset();
        planet(FractionType.green, 20, .2, .5);
        planet(FractionType.red, 20, .4, .5);
        planet(FractionType.yellow, 20, .6, .5);
        planet(FractionType.cyan, 20, .8, .5);
        GameRules.aiTypes.put(FractionType.red, AiType.savvy);
        GameRules.aiTypes.put(FractionType.yellow, AiType.silly);
        GameRules.aiTypes.put(FractionType.cyan, AiType.simple);
        String code = layer.exportManager.perform();
        reset();
        controller.importManager.perform(code);
        layer.aiManager.onEndCreation();
        check(GameRules.aiTypes.get(FractionType.red) == AiType.savvy, "Savvy identity not restored");
        check(GameRules.aiTypes.get(FractionType.yellow) == AiType.silly, "Silly identity not restored");
        check(layer.aiManager.entities.size() == 3, "Wrong restored AI count");
        layer.aiManager.onEndCreation();
        check(layer.aiManager.entities.size() == 3, "AI controllers duplicated on reload");
        Planet red = layer.planetsManager.planets.get(1);
        red.setFraction(FractionType.yellow);
        check(layer.aiManager.getType(red.fraction) == AiType.silly, "Captured node retained its previous marker");
        check(layer.aiManager.getType(FractionType.green) == null && layer.aiManager.getType(FractionType.neutral) == null,
                "Human or neutral node has an AI marker");
        String legacy = code.replaceAll("#ai_types:[^#]*", "");
        reset();
        controller.importManager.perform(legacy);
        check(layer.aiManager.getType(FractionType.red) == AiType.simple, "Legacy map should use Simple");
        GameRules.aiOnlyMode = true;
        check(layer.aiManager.getType(FractionType.green) == AiType.simple, "AI-only green needs a marker");
    }

    private void advance(int steps) {
        for (int i = 0; i < steps; i++) {
            layer.planetsManager.moveActually();
            layer.linksManager.moveActually();
            layer.unitManager.moveActually();
        }
    }

    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static final class TrackingSilly extends AiSillyEntity {
        int erratic;
        TrackingSilly(ObjectsLayer layer) { super(layer, new Random(97123)); }
        @Override protected boolean replacePlannedMove() {
            boolean replaced = super.replacePlannedMove();
            if (replaced) erratic++;
            return replaced;
        }
    }
}
