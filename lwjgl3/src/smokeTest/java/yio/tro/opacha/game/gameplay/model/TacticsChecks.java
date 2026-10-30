package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.RefreshRateDetector;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.stuff.*;

/** Deterministic combat scenarios against the actual simulation, without node regeneration. */
public final class TacticsChecks {
    private GameController controller;
    private ObjectsLayer layer;
    private int checks;
    public static void run(GameController controller) {
        TacticsChecks suite = new TacticsChecks(); suite.controller = controller;
        float velocity = VelocityManager.getInstance().value;
        float refresh = RefreshRateDetector.getInstance().multiplier;
        VelocityManager.getInstance().value = 1; RefreshRateDetector.getInstance().multiplier = 1;
        try {
            suite.routes(); WeaponUsabilityChecks.run(controller); suite.artillery(); suite.pacts(); suite.flags(); suite.serialization(); suite.aiTools(); suite.aiBalance(); suite.siegeRegeneration();
            System.out.println("TACTICS SCENARIOS PASSED: " + suite.checks + " assertions");
        } finally { VelocityManager.getInstance().value = velocity; RefreshRateDetector.getInstance().multiplier = refresh; }
    }
    private void reset(PlayType mode) {
        GameRules.features = RuleFeatures.ALL;
        GameRules.playType = mode; GameRules.aiOnlyMode = false; GameRules.difficulty = Difficulty.hard;
        GameRules.aiTypes.clear(); controller.gameMode = GameMode.skirmish; controller.createGameObjects(); layer = controller.objectsLayer;
    }
    private Planet node(FractionType owner, int size, double x, double y) {
        Planet p = layer.planetsManager.spawnPlanet(new CircleYio(x * GraphicsYio.width, y * GraphicsYio.width,
                layer.planetsManager.getPlanetRadius()), owner);
        p.setUnitsInside(size); p.setCurrentDefense(0); return p;
    }
    private Unit send(Planet from, Planet to, int amount) {
        layer.unitManager.launchUnit(from, to, amount);
        return layer.unitManager.units.get(layer.unitManager.units.size() - 1);
    }
    private void walk(int frames) {
        for (int i = 0; i < frames; i++) {
            for (Unit unit : layer.unitManager.units) unit.moveActually();
            for (Link link : layer.linksManager.links) link.checkForBattles();
        }
    }
    private void routes() {
        reset(PlayType.conquest);
        Planet a = node(FractionType.green, 200, .15, .5), b = node(FractionType.green, 0, .35, .5);
        Link normal = layer.linksManager.addLink(a, b);
        Unit standard = send(a, b, 10); standard.moveActually(); float step = standard.acceleratableFactor.value; standard.setAlive(false);
        normal.directional = true;
        Unit fast = send(a, b, 10), slow = send(b, a, 10);
        fast.moveActually(); slow.moveActually();
        near(fast.acceleratableFactor.value / step, 2, "Fast direction must be twice normal");
        near((1 - slow.acceleratableFactor.value) / step, .3, "Reverse direction must be 0.3 normal");
        fast.setAlive(false); slow.setAlive(false);
        layer.linksManager.removeLink(normal);
        layer.tactics.normalRouteLength = a.position.center.distanceTo(b.position.center);
        Link blueprint = layer.tactics.createBlueprint(a, b, FractionType.green);
        check(blueprint != null && blueprint.constructionCost == 120 && blueprint.directional, "Typical route needs 120 diggers");
        check(layer.tactics.createBlueprint(a, b, FractionType.green) == null, "Duplicate route accepted");
        check(!blueprint.canUse(b, FractionType.green) && !blueprint.canUse(a, FractionType.red), "Unfinished route is only usable by its builder, from its start");
        check(layer.planetsManager.sendUnitsPercent(a, b, 25), "Digger order rejected");
        Unit first = layer.unitManager.units.get(layer.unitManager.units.size() - 1);
        walk(500);
        check(!first.isAlive() && blueprint.constructionSpent == 50, "First 50 diggers must build 50/80 and disappear");
        check(b.unitsInside == 0 && blueprint.isBlueprint(), "Incomplete route delivered troops");
        Unit second = send(a, b, 100); walk(500);
        check(!blueprint.isBlueprint() && blueprint.constructionSpent == 120, "Second crew did not complete the route");
        near(b.unitsInside, 30, "Only the 30 surviving builders should arrive");
        check(!second.digger && blueprint.canUse(b, FractionType.red), "Completed route must become public in both directions");
        Planet c = node(FractionType.neutral, 0, .55, .5);
        Link longer = layer.tactics.createBlueprint(a, c, FractionType.green);
        check(longer.constructionCost == 240, "Twice the distance must cost twice the diggers");
        send(a, c, 40); send(a, c, 280); walk(1000);
        check(!longer.isBlueprint() && longer.constructionSpent == 240 && c.fraction == FractionType.green,
                "Concurrent crews must share work and finish");
        near(c.unitsInside, 80, "Concurrent building must conserve troops");
    }
    private void artillery() {
        reset(PlayType.conquest);
        Planet gun = node(FractionType.green, 100, .2, .5), target = node(FractionType.red, 20, .4, .5);
        layer.tactics.normalRouteLength = .2 * GraphicsYio.width;
        target.setType(PlanetType.defensive); target.setCurrentDefense(12);
        check(layer.tactics.buildArtillery(gun) && gun.unitsInside == 80 && gun.type == PlanetType.artillery, "Artillery setup must cost 20");
        check(layer.tactics.fire(gun, target) && gun.unitsInside == 60, "Shot must cost 20");
        near(target.currentDefense, 12, "Artillery changed shields"); near(target.unitsInside, 16, "Shot must kill 20 percent through shields");
        check(!layer.tactics.fire(gun, target), "Artillery ignored reload");
        for (int i = 0; i < 299; i++) layer.tactics.moveActually();
        check(!layer.tactics.canFire(gun, target), "Reload completed before five game seconds");
        layer.tactics.moveActually();
        check(layer.tactics.fire(gun, target), "Reload did not complete after five game seconds");
        near(target.currentDefense, 12, "Repeat shot changed shields"); near(target.unitsInside, 12.8, "Repeat shot did not use current population");
        target.setType(PlanetType.economic); target.setUnitsInside(4); gun.artilleryReload = 0;
        check(layer.tactics.fire(gun, target), "Final shot failed");
        check(target.fraction == FractionType.red && target.type == PlanetType.economic,
                "Artillery changed ownership or upgrade"); near(target.unitsInside, 3.2, "Small garrison must lose 20 percent");
        Planet distant = node(FractionType.red, 20, .9, .5); gun.setUnitsInside(50); gun.artilleryReload = 0;
        check(!layer.tactics.canFire(gun, distant), "Out-of-range artillery target accepted");
        near(layer.tactics.artilleryRange() / layer.tactics.normalRouteLength, 3, "Range must be three normal lengths");
        check(!layer.tactics.canFire(gun, gun), "Own artillery target accepted");
        gun.artilleryReload = 5; VelocityManager.getInstance().value = 2;
        for (int i = 0; i < 150; i++) layer.tactics.moveActually();
        near(gun.artilleryReload, 0, "Reload must follow simulation speed"); VelocityManager.getInstance().value = 1;
    }
    private void pacts() {
        reset(PlayType.alliances);
        Planet a = node(FractionType.green, 20, .2, .5), b = node(FractionType.red, 20, .4, .5);
        Planet c = node(FractionType.yellow, 20, .6, .5); Link link = layer.linksManager.addLink(a, b);
        AllianceManager p = layer.tactics.alliances;
        check(p.offer(FractionType.green, FractionType.red) && !p.areAllied(FractionType.green, FractionType.red), "Unilateral offer formed an alliance");
        check(p.offer(FractionType.red, FractionType.green) && p.areAllied(FractionType.green, FractionType.red), "Mutual offer must form an alliance");
        Unit up = send(a, b, 10), down = send(b, a, 20);
        up.acceleratableFactor.value = .6f; down.acceleratableFactor.value = .4f; link.checkForBattles();
        check(up.value == 10 && down.value == 20 && up.isAlive() && down.isAlive(), "Allied fleets collided");
        b.onUnitReached(up); near(b.unitsInside, 27, "Allied reinforcement must add 70 percent");
        check(b.fraction == FractionType.red, "Allied reinforcement captured its recipient");
        p.offer(FractionType.red, FractionType.yellow); p.offer(FractionType.yellow, FractionType.red);
        check(!p.areAllied(FractionType.green, FractionType.yellow), "Friendship must not be transitive");
        layer.tactics.bombs.add(new RouteBomb(link, FractionType.green, .5f)); up.takeDamage(2);
        RouteBomb bomb = layer.tactics.bombs.get(0); layer.tactics.checkBombs(down, .7f, .3f);
        check(bomb.active && down.value == 20, "Allied fleet triggered a bomb");
        check(p.breakPact(FractionType.red, FractionType.green), "Either partner must be able to break a pact");
        link.checkForBattles();
        check(up.isAlive() && down.isAlive(), "Fleets that already passed peacefully collided retroactively");
        check(!p.offer(FractionType.green, FractionType.red) && p.isBroken(FractionType.green, FractionType.red), "Broken alliance renewed in the same match");
        layer.tactics.checkBombs(down, .7f, .3f); check(!bomb.active && down.value == 8, "Former ally must trigger the bomb");
        Unit hostile = send(a, b, 5); hostile.acceleratableFactor.value = .5f; down.acceleratableFactor.value = .4f;
        link.checkForBattles(); check(!hostile.isAlive() && down.value == 3, "Former allies must fight on their next crossing");
        check(!p.peacefulSurvivors(), "Hostile survivors incorrectly won together");
    }
    private void flags() {
        reset(PlayType.capture_flag);
        Planet a = node(FractionType.green, 100, .15, .5), n = node(FractionType.neutral, 3, .5, .5), b = node(FractionType.red, 100, .85, .5);
        layer.linksManager.addLink(a, n); layer.linksManager.addLink(n, b); layer.tactics.onEndCreation();
        check(layer.tactics.flagPlanet == n && n.unitsInside == 50 && n.isNeutral(), "Shared flag must begin on a neutral supernode with 50 troops");
        check(n.position.radius > a.position.radius && layer.tactics.winner() == null, "Supernode size or initial victory is wrong");
        n.setFraction(FractionType.green); check(layer.tactics.winner() == FractionType.green, "Shared flag capture must win");
        n.setFraction(FractionType.red); check(layer.tactics.winner() == FractionType.green, "First flag winner changed");
        reset(PlayType.home_flags);
        a = node(FractionType.green, 10, .2, .5); b = node(FractionType.red, 10, .8, .5); layer.tactics.onEndCreation();
        check(layer.tactics.homeFlags[1] == a && layer.tactics.homeFlags[2] == b && layer.tactics.winner() == null, "Home flags must mark original starting nodes");
        b.setFraction(FractionType.green); check(layer.tactics.winner() == FractionType.green, "Enemy home flag capture must win");
    }
    private void serialization() {
        reset(PlayType.alliances);
        Planet a = node(FractionType.green, 60, .2, .5), b = node(FractionType.red, 25, .4, .5), c = node(FractionType.yellow, 3, .6, .5);
        layer.tactics.normalRouteLength = .2 * GraphicsYio.width;
        Link route = layer.tactics.createBlueprint(a, b, FractionType.green); route.constructionSpent = 23;
        a.bombStock = 2; a.nodeBombs = 3; layer.tactics.bombs.add(new RouteBomb(route, FractionType.green, .2f));
        layer.tactics.buildArtillery(a); a.artilleryReload = 3.5f; a.setCurrentDefense(2.5f);
        layer.tactics.alliances.offer(FractionType.green, FractionType.red); layer.tactics.alliances.offer(FractionType.red, FractionType.green);
        layer.tactics.alliances.offer(FractionType.green, FractionType.yellow);
        String code = layer.exportManager.perform(); controller.createGameObjects(); controller.importManager.perform(code); layer = controller.objectsLayer;
        check(GameRules.playType == PlayType.alliances && layer.linksManager.links.get(0).constructionSpent == 23, "Mode or route work lost in exported map");
        check(layer.planetsManager.planets.get(0).bombStock == 2 && layer.planetsManager.planets.get(0).nodeBombs == 1 && layer.tactics.mobileBombs.size() == 1, "Legacy bombs lost in exported map");
        a = layer.planetsManager.planets.get(0); near(a.artilleryReload, 3.5, "Reload lost in exported map"); near(a.currentDefense, 2.5, "Shields lost in exported map");
        check(layer.tactics.alliances.areAllied(FractionType.green, FractionType.red) && layer.tactics.alliances.hasOffer(FractionType.green, FractionType.yellow), "Pacts lost in exported map");
        controller.createGameObjects(); controller.importManager.perform("opacha_level_code#game_rules:hard false#planets:green empty 10 0.2 0.5,red empty 10 0.4 0.5,#links:neutral 0.2 0.5 0.4 0.5,#");
        check(GameRules.playType == PlayType.conquest && !controller.objectsLayer.linksManager.links.get(0).directional, "Legacy maps must retain conquest and normal routes");
    }
    private void aiTools() {
        reset(PlayType.conquest);
        Planet gun = node(FractionType.red, 100, .4, .5), first = node(FractionType.green, 12, .2, .5);
        node(FractionType.cyan, 20, .6, .5);
        TacticsAi.perform(layer.tactics);
        check(gun.type == PlanetType.artillery && gun.unitsInside == 80, "AI must be able to build artillery");
        TacticsAi.perform(layer.tactics);
        check(gun.unitsInside == 60 && gun.artilleryReload == 5, "AI must fire artillery at a hostile node");
        near(layer.planetsManager.planets.get(2).unitsInside, 16, "AI must target the largest garrison for percentage damage");

        reset(PlayType.conquest);
        Planet base = node(FractionType.red, 100, .2, .5), enemy = node(FractionType.green, 20, .4, .5);
        base.setType(PlanetType.economic); layer.linksManager.addLink(base, enemy);
        Unit carrier = send(base, enemy, 30), approaching = send(enemy, base, 20);
        carrier.acceleratableFactor.value = .3f; approaching.acceleratableFactor.value = .7f;
        TacticsAi.perform(layer.tactics);
        check(base.unitsInside == 100 && carrier.isAlive() && layer.tactics.mobileBombs.isEmpty(), "AI converted an ineligible army");

        reset(PlayType.conquest);
        base=node(FractionType.red,100,.4,.5); enemy=node(FractionType.green,20,.2,.5); base.setType(PlanetType.economic);
        Link incoming=layer.linksManager.addLink(enemy,base); incoming.directional=true;
        approaching=send(enemy,base,20); approaching.acceleratableFactor.value=.3f;
        TacticsAi.perform(layer.tactics);
        check(base.nodeBombs==0 && layer.tactics.mobileBombs.isEmpty(),"AI invented a mine without an eligible army");

        reset(PlayType.conquest);
        base = node(FractionType.red, 200, .2, .5); base.setType(PlanetType.economic);
        Planet rear = node(FractionType.red, 0, .2, .7), opportunity = node(FractionType.neutral, 3, .4, .5);
        node(FractionType.green, 10, .9, .9); layer.linksManager.addLink(base, rear);
        layer.tactics.normalRouteLength = .2 * GraphicsYio.width; layer.tactics.elapsed = 31;
        GameRules.aiTypes.put(FractionType.red, yio.tro.opacha.game.gameplay.ai.AiType.savvy);
        TacticsAi.perform(layer.tactics);
        check(base.getLink(opportunity) != null && base.getLink(opportunity).isBlueprint()
                && layer.unitManager.units.size() == 1 && layer.unitManager.units.get(0).digger, "Savvy must fund a useful shortcut with diggers");

        reset(PlayType.alliances);
        node(FractionType.green, 20, .2, .5); node(FractionType.red, 30, .4, .5); node(FractionType.yellow, 100, .6, .5);
        layer.tactics.elapsed = 9; layer.tactics.alliances.offer(FractionType.green, FractionType.red);
        TacticsAi.perform(layer.tactics);
        check(layer.tactics.alliances.areAllied(FractionType.green, FractionType.red), "AI must accept a useful human offer");
        for (Planet p : layer.planetsManager.planets) p.setUnitsInside(p.fraction == FractionType.red ? 400 : 1);
        layer.tactics.elapsed = 40; GameRules.aiTypes.put(FractionType.red, yio.tro.opacha.game.gameplay.ai.AiType.savvy);
        TacticsAi.perform(layer.tactics);
        check(layer.tactics.alliances.isBroken(FractionType.green, FractionType.red), "Savvy must be able to break an old pact when its partner becomes vulnerable");
    }
    private void aiBalance() {
        reset(PlayType.conquest);
        Planet gun = node(FractionType.red,100,.2,.5), vacant=node(FractionType.green,0,.4,.5);
        vacant.setType(PlanetType.defensive); vacant.setCurrentDefense(14);
        Planet foothold=node(FractionType.neutral,3,.2,.7);
        gun.setType(PlanetType.artillery); layer.linksManager.addLink(gun,foothold);
        layer.tactics.normalRouteLength=.2*GraphicsYio.width;
        TacticsAi.perform(layer.tactics);
        check(gun.artilleryReload==0 && vacant.unitsInside==0,"AI wasted a shell on an ordinary empty node");
        check(gun.unitsInside==50 && layer.unitManager.units.size()==1,"Gun reserves did not advance toward a capturable foothold");

        reset(PlayType.conquest);
        Planet economy=node(FractionType.red,100,.2,.5); economy.setType(PlanetType.economic);
        node(FractionType.green,30,.4,.5); node(FractionType.cyan,35,.4,.7);
        layer.tactics.normalRouteLength=.2*GraphicsYio.width; TacticsAi.perform(layer.tactics);
        check(economy.isEconomic(),"AI destroyed its economic node to build a gun");

        reset(PlayType.conquest);
        gun=node(FractionType.red,70,.2,.5); gun.setType(PlanetType.artillery);
        Planet enemy=node(FractionType.green,30,.4,.5); enemy.setType(PlanetType.economic);
        layer.linksManager.addLink(gun,enemy); send(enemy,gun,60);
        layer.tactics.normalRouteLength=.2*GraphicsYio.width; TacticsAi.perform(layer.tactics);
        check(gun.unitsInside==70 && gun.artilleryReload==0,"Gun spent its defence against a larger incoming fleet");

        reset(PlayType.alliances);
        node(FractionType.green,30,.2,.5); node(FractionType.red,40,.4,.5);
        node(FractionType.yellow,100,.6,.5); node(FractionType.cyan,35,.8,.5);
        layer.tactics.elapsed=9; layer.tactics.alliances.offer(FractionType.green,FractionType.red);
        TacticsAi.perform(layer.tactics);
        check(layer.tactics.alliances.areAllied(FractionType.green,FractionType.red),"Weaker players could not cooperate against a shared rival");
        layer.tactics.alliances.offer(FractionType.cyan,FractionType.red); TacticsAi.perform(layer.tactics);
        check(!layer.tactics.alliances.areAllied(FractionType.cyan,FractionType.red),"AI accepted a second simultaneous partner");
        check(!layer.tactics.alliances.areAllied(FractionType.yellow,FractionType.red),"Strongest rival joined a pointless universal pact");
    }
    private void siegeRegeneration() {
        float[] populations=new float[2];
        for (int count=1;count<=2;count++) {
            reset(PlayType.conquest);
            Planet target=node(FractionType.red,20,.4,.5); target.setType(PlanetType.defensive); target.setCurrentDefense(14);
            Planet[] guns=new Planet[count];
            for(int i=0;i<count;i++) { guns[i]=node(FractionType.green,200,.2,.5+i*.2); guns[i].setType(PlanetType.artillery); }
            layer.tactics.normalRouteLength=.2*GraphicsYio.width;
            int shots=0;
            for(int tick=0;tick<1200;tick++) {
                for(Planet gun:guns) if(layer.tactics.canFire(gun,target) && layer.tactics.fire(gun,target)) shots++;
                for(Planet planet:layer.planetsManager.planets) planet.moveActually();
                layer.tactics.moveActually();
            }
            populations[count-1]=target.unitsInside;
            check(shots==count*4,"Unexpected shot count in 20-second regenerating siege");
            check(target.type==PlanetType.defensive && target.currentDefense==14,"Percentage siege damaged the defensive upgrade or shields");
        }
        check(populations[1]<populations[0],"Synchronized guns did not reduce population more than one gun");
        System.out.println("SIEGE REGEN 20s: one gun target population="+populations[0]+", two guns="+populations[1]);
    }
    private void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private void near(double a, double b, String message) { check(Math.abs(a - b) < .001, message + ": " + a + " != " + b); }
}
