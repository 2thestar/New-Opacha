package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.RefreshRateDetector;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.stuff.*;

/** Regression scenarios for hidden cargo, destructive routes and capture order. */
public final class WeaponUsabilityChecks {
    private final GameController controller;
    private ObjectsLayer layer;
    private Planet factory, secondFactory, front, enemy;
    private Link route;
    private int checks;

    private WeaponUsabilityChecks(GameController controller) { this.controller = controller; }
    public static void run(GameController controller) {
        WeaponUsabilityChecks suite = new WeaponUsabilityChecks(controller);
        float velocity = VelocityManager.getInstance().value;
        float refresh = RefreshRateDetector.getInstance().multiplier;
        try {
            VelocityManager.getInstance().value = 1; RefreshRateDetector.getInstance().multiplier = 1;
            suite.conversion(); suite.routing(); suite.detonation(); suite.mines(); suite.saveLoad(); suite.ai();
            System.out.println("WEAPON RULES PASSED: " + suite.checks + " assertions");
        } finally { VelocityManager.getInstance().value = velocity; RefreshRateDetector.getInstance().multiplier = refresh; }
    }
    private void reset() {
        GameRules.features = RuleFeatures.ALL; GameRules.playType = PlayType.conquest;
        GameRules.aiOnlyMode = false; GameRules.aiTypes.clear(); controller.gameMode = GameMode.skirmish;
        controller.createGameObjects(); layer = controller.objectsLayer;
        factory = node(FractionType.green, .15, .5); factory.setType(PlanetType.economic);
        secondFactory = node(FractionType.green, .15, .8); secondFactory.setType(PlanetType.economic);
        front = node(FractionType.green, .45, .5); enemy = node(FractionType.red, .75, .5);
        route = layer.linksManager.addLink(factory, front);
        layer.linksManager.addLink(front, enemy);
    }
    private Planet node(FractionType owner, double x, double y) {
        Planet node = layer.planetsManager.spawnPlanet(new CircleYio(x * GraphicsYio.width, y * GraphicsYio.width,
                .05 * GraphicsYio.width), owner);
        node.setUnitsInside(0); node.setCurrentDefense(0); return node;
    }
    private Unit fleet(Planet start, Planet target, int count, float progress) {
        layer.unitManager.launchUnit(start, target, count);
        Unit unit = layer.unitManager.units.get(layer.unitManager.units.size() - 1);
        unit.acceleratableFactor.value = progress; return unit;
    }
    private void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private void near(double a, double b, String message) { check(Math.abs(a - b) < .001, message + ": " + a + " != " + b); }
    private void moveBombs(int ticks) { for (int i = 0; i < ticks; i++) for (MobileBomb bomb : layer.tactics.mobileBombs) bomb.move(); }

    private void conversion() {
        reset(); TacticsManager t = layer.tactics;
        Unit small = fleet(factory, front, 50, .35f);
        check(!t.canConvertToBomb(small), "50 troops qualified"); small.setAlive(false);
        Unit eligible = fleet(factory, front, 51, .35f);
        eligible.heldForBomb = true; eligible.moveActually(); near(eligible.acceleratableFactor.value, .35, "Held army moved");
        secondFactory.setType(PlanetType.empty);
        check(!t.canConvertToBomb(eligible), "One factory qualified");
        secondFactory.setType(PlanetType.economic);
        Unit ordinary = fleet(front, enemy, 90, .35f);
        check(!t.canConvertToBomb(ordinary), "Ordinary node departure qualified");
        front.setType(PlanetType.economic);
        check(!t.canConvertToBomb(ordinary), "A later factory upgrade changed the departure source");
        ordinary.setAlive(false);
        front.setType(PlanetType.empty);
        eligible.acceleratableFactor.value = 0;
        check(!t.canConvertToBomb(eligible), "Node endpoint qualified"); eligible.acceleratableFactor.value = .35f;
        GameRules.features &= ~RuleFeatures.BOMBS;
        check(t.convertToBomb(eligible) == null && eligible.isAlive(), "Locked lesson consumed troops"); GameRules.features = RuleFeatures.ALL;
        MobileBomb bomb = t.convertToBomb(eligible);
        check(bomb != null && !eligible.isAlive() && !route.upWalkers.contains(eligible), "Conversion left a combat army");
        near(bomb.progress.value, .35, "Conversion teleported cargo");
        check(bomb.visibleTo(FractionType.green) && !bomb.visibleTo(FractionType.red), "Bomb visibility leaked");
        check(t.findBomb(bomb.position(), FractionType.red) == null, "Enemy hit testing revealed a bomb");
        check(t.convertToBomb(eligible) == null, "Army converted twice");
        near(layer.planetsManager.calculateThreat(front), 0, "Secret cargo appeared in troop threat");
        check(!t.buyBomb(factory) && !t.sendBomb(factory, front), "Old stock controls bypassed conversion");
    }
    private void routing() {
        reset(); MobileBomb bomb = layer.tactics.convertToBomb(fleet(factory, front, 80, .35f));
        route.directional = true;
        moveBombs(400); check(bomb.node == front && bomb.link == null, "Bomb did not wait at destination");
        front.setAutoTarget(enemy); layer.tactics.forwardBombs(front);
        check(bomb.link == front.getLink(enemy) && bomb.target == enemy, "Arrow did not route cargo first");
        near(front.unitsInside, 0, "Cargo became reinforcements");
        VelocityManager.getInstance().value = 0; float progress = bomb.progress.value;
        moveBombs(30); near(bomb.progress.value, progress, "Paused cargo moved");
        VelocityManager.getInstance().value = 1; moveBombs(400);
        check(bomb.node == enemy && !layer.tactics.activateBomb(bomb, FractionType.green), "Cargo armed an enemy node");
        reset(); route.directional = true;
        MobileBomb forward = new MobileBomb(FractionType.green, route, front, .5f);
        MobileBomb reverse = new MobileBomb(FractionType.green, route, factory, .5f);
        forward.move(); reverse.move();
        near((forward.progress.value - .5) / (.5 - reverse.progress.value), 2 / .3, "Directional cargo speed differs from troops");
    }
    private void detonation() {
        reset(); MobileBomb bomb = layer.tactics.convertToBomb(fleet(factory, front, 80, .4f));
        Unit own = fleet(factory, front, 10, .2f), hostile = fleet(front, factory, 20, .8f);
        hostile.setFraction(FractionType.red);
        Unit otherRoute = fleet(front, enemy, 15, .3f);
        factory.setAutoTarget(front); front.setAutoTarget(factory);
        check(!layer.tactics.activateBomb(bomb, FractionType.red), "Enemy detonated secret cargo");
        check(layer.tactics.activateBomb(bomb, FractionType.green), "Edge activation failed");
        check(!layer.linksManager.links.contains(route) && factory.getLink(front) == null, "Explosion retained the edge");
        check(!own.isAlive() && !hostile.isAlive() && otherRoute.isAlive(), "Explosion casualties crossed route boundaries");
        check(factory.autoTarget == null && front.autoTarget == null, "Removed edge retained arrows");
        check(layer.tactics.mobileBombs.isEmpty() && !layer.tactics.activateBomb(bomb, FractionType.green), "Destroyed cargo remained active");
    }
    private void mines() {
        reset(); TacticsManager t = layer.tactics;
        MobileBomb bomb = new MobileBomb(FractionType.green, front); t.mobileBombs.add(bomb);
        check(t.activateBomb(bomb, FractionType.green) && front.nodeBombs == 1, "Owned node did not arm");
        check(front.visibleNodeBombs(FractionType.red) == 0, "Mine placement leaked");
        front.setUnitsInside(30); front.setCurrentDefense(7);
        front.onUnitReached(fleet(enemy, front, 10, .2f));
        check(front.fraction == FractionType.green && front.nodeBombs == 1, "Failed invasion triggered mine");
        front.onUnitReached(fleet(factory, front, 3, .5f));
        check(front.nodeBombs == 1, "Friendly arrival triggered mine");
        front.takeArtilleryDamage(.2); check(front.nodeBombs == 1, "Artillery triggered mine");
        GameRules.playType = PlayType.home_flags; t.homeFlags[FractionType.green.ordinal()] = front;
        front.onUnitReached(fleet(enemy, front, 999, .2f));
        check(front.isNeutral() && front.unitsInside == 0 && front.nodeBombs == 0, "Mine did not wipe invading survivors and neutralize");
        check(t.flagWinner == null, "Failed mined occupation won the flag");
        check(front.maxDefense == 14 && front.shields.size() == 14, "Mine shield capacity missing"); near(front.currentDefense, 14, "Mine shields not filled");
        t.elapsed = 9.99; front.moveActually(); check(front.maxDefense == 14, "Mine shields expired early");
        t.elapsed = 10; front.moveActually(); check(front.maxDefense == 7 && front.currentDefense <= 7, "Mine shields survived ten game seconds");
        reset(); front.nodeBombs = 1;
        Unit invasion = fleet(enemy, front, 999, .2f); front.onUnitReached(invasion); invasion.setAlive(false);
        VelocityManager.getInstance().value = 0;
        for (int i=0;i<120;i++) { front.moveActually(); layer.tactics.moveActually(); }
        check(layer.tactics.elapsed == 0 && front.currentDefense == 14, "Paused mine shield timer advanced");
        VelocityManager.getInstance().value = 1;
        for (int i=0;i<599;i++) { front.moveActually(); layer.tactics.moveActually(); }
        check(front.maxDefense == 14, "Actual shield timer expired before ten seconds");
        for (int i=0;i<3;i++) { front.moveActually(); layer.tactics.moveActually(); }
        check(front.maxDefense == 7, "Actual shield timer did not expire");
        reset(); front.nodeBombs = 1; front.setUnitsInside(2);
        GameRules.playType = PlayType.alliances; t = layer.tactics;
        t.alliances.allied[FractionType.red.ordinal()][FractionType.green.ordinal()] = true;
        t.alliances.allied[FractionType.green.ordinal()][FractionType.red.ordinal()] = true;
        front.onUnitReached(fleet(enemy, front, 100, .2f));
        check(front.fraction == FractionType.green && front.nodeBombs == 1, "Ally triggered mine");
    }
    private void saveLoad() {
        reset(); layer.tactics.elapsed = 3;
        MobileBomb moving = layer.tactics.convertToBomb(fleet(factory, front, 80, .37f));
        layer.tactics.mobileBombs.add(new MobileBomb(FractionType.green, secondFactory));
        front.nodeBombs = 1; enemy.restoreMineShield(13); enemy.setCurrentDefense(14);
        String map = layer.exportManager.perform(); controller.createGameObjects(); controller.importManager.perform(map);
        layer = controller.objectsLayer;
        check(layer.tactics.mobileBombs.size() == 2, "Save lost cargo");
        near(layer.tactics.mobileBombs.get(0).progress.value, moving.progress.value, "Save reset travel");
        check(layer.tactics.mobileBombs.get(1).node == layer.planetsManager.planets.get(1), "Save lost waiting node");
        check(layer.planetsManager.planets.get(2).nodeBombs == 1, "Save lost mine");
        check(layer.planetsManager.planets.get(3).maxDefense == 14, "Save lost shield capacity");
        near(layer.planetsManager.planets.get(3).mineShieldUntil, 13, "Save lost expiry");
        layer.linksManager.removeLink(layer.linksManager.links.get(0));
        check(layer.tactics.mobileBombs.size() == 1, "Removing edge retained cargo");
    }
    private void ai() {
        reset(); factory.setFraction(FractionType.red); factory.setType(PlanetType.economic);
        secondFactory.setFraction(FractionType.red); secondFactory.setType(PlanetType.economic);
        Unit own = fleet(factory, front, 60, .2f), attack = fleet(front, factory, 150, .8f);
        TacticsAi.perform(layer.tactics);
        check(!own.isAlive() && layer.tactics.mobileBombs.size() == 1, "AI did not convert eligible factory army");
        TacticsAi.perform(layer.tactics);
        check(!attack.isAlive() && !layer.linksManager.links.contains(route), "AI did not detonate its own cargo");
    }
}
