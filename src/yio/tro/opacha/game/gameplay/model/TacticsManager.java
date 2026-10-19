package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.RefreshRateDetector;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;
import java.util.*;

public final class TacticsManager {
    public enum Action { none, blueprint, bomb, artillery }
    public static final int BOMB_PRICE = 20, BOMB_CREW = 2, BOMB_DAMAGE = 12;
    public static final int ARTILLERY_PRICE = 20, SHOT_PRICE = 20, SHOT_DAMAGE = 10;
    public static final float RELOAD_SECONDS = 5;
    public final ObjectsLayer layer;
    public final AllianceManager alliances;
    public final ArrayList<RouteBomb> bombs = new ArrayList<>();
    public final int[] inventory = new int[5];
    public double elapsed;
    public double normalRouteLength;
    public Planet flagPlanet;
    public final Planet[] homeFlags = new Planet[5];
    public FractionType flagWinner;
    public Action pending = Action.none;
    public Planet source;
    public Planet shotSource, shotTarget;
    public double shotUntil;
    private double nextAiAction;
    private boolean loadedReference;

    public TacticsManager(ObjectsLayer layer) {
        this.layer = layer;
        alliances = new AllianceManager(layer);
        normalRouteLength = .25 * GraphicsYio.width;
    }

    public void onEndCreation() {
        ArrayList<Double> lengths = new ArrayList<>();
        for (Link link : layer.linksManager.links) if (link.constructionCost == 0) lengths.add(link.distance);
        if (!loadedReference && !lengths.isEmpty()) {
            Collections.sort(lengths);
            normalRouteLength = lengths.get(lengths.size() / 2);
        }
        if (GameRules.playType == PlayType.capture_flag && flagPlanet == null) chooseFlag();
        if (GameRules.playType == PlayType.home_flags) {
            for (Planet planet : layer.planetsManager.planets) {
                if (!planet.isNeutral() && homeFlags[planet.fraction.ordinal()] == null) homeFlags[planet.fraction.ordinal()] = planet;
            }
        }
    }

    public void moveActually() {
        if (layer.gameController.gameMode == GameMode.editor) return;
        elapsed += RefreshRateDetector.getInstance().multiplier * VelocityManager.getInstance().value / 60.0;
        for (Planet planet : layer.planetsManager.planets) {
            planet.artilleryReload = Math.max(0, planet.artilleryReload
                    - RefreshRateDetector.getInstance().multiplier * VelocityManager.getInstance().value / 60f);
        }
        if (elapsed >= nextAiAction) {
            nextAiAction = elapsed + 2;
            TacticsAi.perform(this);
        }
        if (pending != Action.none && source != null && !source.isControlledByPlayer()) cancel();
    }

    public Link createBlueprint(Planet start, Planet target, FractionType owner) {
        if (start == null || target == null || start == target || start.fraction != owner
                || start.isLinkedTo(target) || owner == FractionType.neutral) return null;
        Link link = layer.linksManager.addLink(start, target);
        link.directional = true;
        link.builder = owner;
        link.constructionCost = Math.max(1, (int) Math.ceil(80 * link.distance / normalRouteLength));
        link.constructionSpent = 0;
        link.initNPoints();
        return link;
    }

    public boolean buyBomb(Planet node) {
        if (node == null || !node.isEconomic() || node.isNeutral() || node.unitsInside < BOMB_PRICE) return false;
        node.decreaseUnitsInside(BOMB_PRICE);
        inventory[node.fraction.ordinal()]++;
        return true;
    }

    public boolean placeBomb(Unit unit, FractionType owner) {
        if (unit == null || !unit.isAlive() || unit.fraction != owner || unit.value < BOMB_CREW
                || inventory[owner.ordinal()] == 0) return false;
        inventory[owner.ordinal()]--;
        bombs.add(new RouteBomb(unit.link, owner, unit.acceleratableFactor.value));
        unit.takeDamage(BOMB_CREW);
        return true;
    }

    public void checkBombs(Unit unit, float oldPosition, float newPosition) {
        for (RouteBomb bomb : bombs) {
            if (!unit.isAlive()) return;
            if (!bomb.active || bomb.link != unit.link || alliances.friendly(bomb.owner, unit.fraction)) continue;
            if (bomb.position < Math.min(oldPosition, newPosition) - .00001f
                    || bomb.position > Math.max(oldPosition, newPosition) + .00001f) continue;
            bomb.active = false;
            unit.takeDamage(BOMB_DAMAGE);
            layer.particlesManager.spawnExplosion(unit.viewPosition.center, 2,
                    layer.particlesManager.convertFractionIntoPaViewType(bomb.owner));
        }
    }

    public boolean buildArtillery(Planet node) {
        if (node == null || node.isNeutral() || node.type == PlanetType.artillery || node.unitsInside < ARTILLERY_PRICE) return false;
        node.decreaseUnitsInside(ARTILLERY_PRICE);
        node.setType(PlanetType.artillery);
        node.setAutoTarget(null);
        node.artilleryReload = 0;
        return true;
    }

    public double artilleryRange() { return 3 * normalRouteLength; }

    public boolean canFire(Planet gun, Planet target) {
        return gun != null && target != null && gun.type == PlanetType.artillery && !gun.isNeutral()
                && !target.isNeutral() && !alliances.friendly(gun.fraction, target.fraction)
                && gun.unitsInside >= SHOT_PRICE && gun.artilleryReload <= .0001f
                && gun.position.center.distanceTo(target.position.center) <= artilleryRange();
    }

    public boolean fire(Planet gun, Planet target) {
        if (!canFire(gun, target)) return false;
        gun.decreaseUnitsInside(SHOT_PRICE);
        gun.artilleryReload = RELOAD_SECONDS;
        target.takeArtilleryDamage(SHOT_DAMAGE);
        shotSource = gun; shotTarget = target; shotUntil = elapsed + .35;
        return true;
    }

    public void begin(Action action, Planet planet) {
        pending = action; source = planet;
        Scenes.selectionOverlay.destroy();
        Scenes.gameOverlay.setCommandMode(true);
        String message = action == Action.blueprint ? "route_pick_target" : action == Action.bomb ? "bomb_pick_troop" : "artillery_pick_target";
        Scenes.notification.show(yio.tro.opacha.menu.LanguagesManager.getInstance().getString(message));
    }

    public void cancel() {
        if (source != null) layer.planetsManager.deselect();
        pending = Action.none; source = null;
        Scenes.gameOverlay.setCommandMode(false);
    }

    /** Node/link indices refer to the preceding map sections. Old maps have no tactics section. */
    public String save() {
        StringBuilder out = new StringBuilder();
        out.append("clock ").append(elapsed).append(' ').append(normalRouteLength / GraphicsYio.width).append(';');
        out.append("flag ").append(layer.planetsManager.planets.indexOf(flagPlanet)).append(' ')
                .append(flagWinner == null ? "none" : flagWinner).append(';');
        for (FractionType owner : FractionType.values()) {
            int i = owner.ordinal();
            out.append("home ").append(owner).append(' ').append(layer.planetsManager.planets.indexOf(homeFlags[i])).append(';');
            out.append("stock ").append(owner).append(' ').append(inventory[i]).append(';');
            for (FractionType other : FractionType.values()) {
                int j = other.ordinal();
                out.append("pact ").append(owner).append(' ').append(other).append(' ')
                        .append(alliances.offers[i][j]).append(' ').append(alliances.allied[i][j]).append(' ')
                        .append(alliances.broken[i][j]).append(' ').append(alliances.formedAt[i][j]).append(';');
            }
        }
        for (RouteBomb bomb : bombs) if (bomb.active) out.append("bomb ")
                .append(layer.linksManager.links.indexOf(bomb.link)).append(' ').append(bomb.owner).append(' ').append(bomb.position).append(';');
        return out.toString();
    }

    public void load(String data) {
        if (data == null) return;
        for (String record : data.split(";")) {
            String[] p = record.split(" ");
            if (p[0].equals("clock")) {
                elapsed = Double.parseDouble(p[1]); normalRouteLength = Double.parseDouble(p[2]) * GraphicsYio.width;
                loadedReference = true; nextAiAction = elapsed + 2;
            } else if (p[0].equals("flag")) {
                int index = Integer.parseInt(p[1]);
                flagPlanet = index < 0 ? null : layer.planetsManager.planets.get(index);
                flagWinner = p[2].equals("none") ? null : FractionType.valueOf(p[2]);
                if (flagPlanet != null) {
                    flagPlanet.position.setRadius(1.3 * layer.planetsManager.getPlanetRadius());
                    flagPlanet.onPositionChanged();
                }
            } else if (p[0].equals("home")) {
                int index = Integer.parseInt(p[2]);
                homeFlags[FractionType.valueOf(p[1]).ordinal()] = index < 0 ? null : layer.planetsManager.planets.get(index);
            } else if (p[0].equals("stock")) {
                inventory[FractionType.valueOf(p[1]).ordinal()] = Integer.parseInt(p[2]);
            } else if (p[0].equals("pact")) {
                int a = FractionType.valueOf(p[1]).ordinal(), b = FractionType.valueOf(p[2]).ordinal();
                alliances.offers[a][b] = Boolean.parseBoolean(p[3]);
                alliances.allied[a][b] = Boolean.parseBoolean(p[4]);
                alliances.broken[a][b] = Boolean.parseBoolean(p[5]);
                alliances.formedAt[a][b] = Double.parseDouble(p[6]);
            } else if (p[0].equals("bomb")) {
                bombs.add(new RouteBomb(layer.linksManager.links.get(Integer.parseInt(p[1])), FractionType.valueOf(p[2]), Float.parseFloat(p[3])));
            }
        }
    }

    public boolean onWorldTap(PointYio point) {
        if (pending == Action.none) return false;
        Planet target = layer.planetsManager.findTouchedPlanet(point);
        boolean success = false;
        if (pending == Action.blueprint) success = createBlueprint(source, target, FractionType.green) != null;
        if (pending == Action.artillery) success = fire(source, target);
        if (pending == Action.bomb) {
            Unit closest = null;
            double distance = .055 * GraphicsYio.width;
            for (Unit unit : layer.unitManager.units) {
                if (!unit.isAlive() || unit.fraction != FractionType.green || !unit.isCurrentlyVisible()) continue;
                double d = point.distanceTo(unit.viewPosition.center);
                if (d < distance) { distance = d; closest = unit; }
            }
            success = placeBomb(closest, FractionType.green);
        }
        if (success) { cancel(); layer.planetsManager.deselect(); }
        else Scenes.notification.show(yio.tro.opacha.menu.LanguagesManager.getInstance().getString("invalid_target"));
        return true;
    }

    public void onCaptured(Planet planet) {
        if (planet == flagPlanet && GameRules.playType == PlayType.capture_flag && flagWinner == null && !planet.isNeutral()) {
            flagWinner = planet.fraction;
        }
        if (GameRules.playType == PlayType.home_flags && flagWinner == null && !planet.isNeutral()) {
            for (FractionType owner : FractionType.values()) {
                if (owner != FractionType.neutral && owner != planet.fraction && homeFlags[owner.ordinal()] == planet) flagWinner = planet.fraction;
            }
        }
    }

    private void chooseFlag() {
        ArrayList<Planet> starts = new ArrayList<>();
        for (Planet planet : layer.planetsManager.planets) if (!planet.isNeutral()) starts.add(planet);
        double best = Double.MAX_VALUE;
        for (Planet candidate : layer.planetsManager.planets) {
            if (!candidate.isNeutral()) continue;
            int min = Integer.MAX_VALUE, max = 0, total = 0;
            for (Planet start : starts) {
                ArrayList<Planet> way = layer.planetsManager.pathFinder.findWay(start, candidate);
                int distance = way == null ? 1000 : way.size();
                min = Math.min(min, distance); max = Math.max(max, distance); total += distance;
            }
            double score = 100 * (max - min) + total;
            if (score < best) { best = score; flagPlanet = candidate; }
        }
        if (flagPlanet != null) {
            flagPlanet.setUnitsInside(50);
            flagPlanet.position.setRadius(1.3 * layer.planetsManager.getPlanetRadius());
            flagPlanet.onPositionChanged();
        }
    }

    public FractionType winner() {
        if (GameRules.isFlagMode()) return flagWinner;
        if (GameRules.playType != PlayType.alliances || !alliances.peacefulSurvivors()) return null;
        Planet strongest = null;
        for (Planet planet : layer.planetsManager.planets) if (!planet.isNeutral()
                && (strongest == null || power(planet.fraction) > power(strongest.fraction))) strongest = planet;
        return strongest == null ? null : strongest.fraction;
    }

    public double power(FractionType faction) {
        double power = 0;
        for (Planet planet : layer.planetsManager.planets) if (planet.fraction == faction) power += 12 + planet.unitsInside + (planet.isEconomic() ? 12 : 0);
        for (Unit unit : layer.unitManager.units) if (unit.isAlive() && unit.fraction == faction) power += unit.value;
        return power;
    }
}
