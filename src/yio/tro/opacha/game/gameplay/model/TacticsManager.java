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
    public static final int MAX_NODE_BOMBS = 3;
    public static final int ARTILLERY_PRICE = 20, SHOT_PRICE = 20, SHOT_DAMAGE = 10;
    public static final float RELOAD_SECONDS = 5;
    public final ObjectsLayer layer;
    public final AllianceManager alliances;
    public final ArrayList<RouteBomb> bombs = new ArrayList<>();
    public final ArrayList<BombCourier> couriers = new ArrayList<>();
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
        for (int i = couriers.size() - 1; i >= 0; i--) {
            BombCourier courier = couriers.get(i);
            courier.move();
            if (!courier.active) couriers.remove(i);
        }
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
        if (!RuleFeatures.has(RuleFeatures.BUILD) || start == null || target == null || start == target || start.fraction != owner
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
        if (!RuleFeatures.has(RuleFeatures.BOMBS) || node == null || !node.isEconomic() || node.isNeutral() || node.unitsInside < BOMB_PRICE) return false;
        node.decreaseUnitsInside(BOMB_PRICE);
        node.bombStock++;
        return true;
    }

    public void onLinkRemoved(Link link) {
        couriers.removeIf(courier -> courier.uses(link));
        bombs.removeIf(bomb -> bomb.link == link);
    }

    public void onLinkEdited(Link link, boolean reversed, boolean unfinished) {
        couriers.removeIf(courier -> courier.uses(link));
        if (unfinished) { bombs.removeIf(bomb -> bomb.link == link); return; }
        if (reversed) for (int i = 0; i < bombs.size(); i++) {
            RouteBomb bomb = bombs.get(i);
            if (bomb.active && bomb.link == link) bombs.set(i, new RouteBomb(link, bomb.owner, 1 - bomb.position));
        }
    }

    public void onNodeRemoved(Planet node) {
        couriers.removeIf(courier -> courier.path.contains(node) || courier.destination == node);
        if (shotSource == node || shotTarget == node) { shotSource = null; shotTarget = null; }
    }

    private boolean canDispatch(Planet depot) {
        return RuleFeatures.has(RuleFeatures.BOMBS) && depot != null && !depot.isNeutral()
                && depot.bombStock > 0 && depot.unitsInside >= BOMB_CREW;
    }

    public int reservedBombs(Planet node) {
        int count = node.nodeBombs;
        for (BombCourier courier : couriers) if (courier.active && courier.destination == node && courier.owner == node.fraction) count++;
        return count;
    }

    public boolean armNode(Planet node) {
        if (!canDispatch(node) || reservedBombs(node) >= MAX_NODE_BOMBS) return false;
        node.bombStock--; node.decreaseUnitsInside(BOMB_CREW); node.nodeBombs++;
        return true;
    }

    public boolean sendBomb(Planet depot, Planet target) {
        if (!canDispatch(depot) || target == null || target.fraction != depot.fraction
                || reservedBombs(target) >= MAX_NODE_BOMBS) return false;
        if (depot == target) return armNode(depot);
        ArrayList<Planet> path = supplyPath(depot, target);
        if (path == null) return false;
        dispatch(depot, path, target, null, 0);
        return true;
    }

    public boolean sendBomb(Planet depot, Link route, float position) {
        if (!canDispatch(depot) || route == null || route.isBlueprint() || !layer.linksManager.links.contains(route)
                || !Float.isFinite(position) || position <= 0 || position >= 1) return false;
        ArrayList<Planet> best = null;
        double bestTime = Double.MAX_VALUE;
        for (Planet end : new Planet[]{route.one, route.two}) {
            if (end.fraction != depot.fraction) continue;
            ArrayList<Planet> path = supplyPath(depot, end);
            if (path == null) continue;
            double time = travelTime(path) + route.distance * (end == route.one ? position : 1 - position) / route.speedFrom(end);
            if (time < bestTime) { bestTime = time; best = path; }
        }
        if (best == null) return false;
        dispatch(depot, best, null, route, position);
        return true;
    }

    private void dispatch(Planet depot, ArrayList<Planet> path, Planet target, Link route, float position) {
        depot.bombStock--; depot.decreaseUnitsInside(BOMB_CREW);
        couriers.add(new BombCourier(this, depot.fraction, path, target, route, position));
    }

    /** Fastest existing route through our own nodes. Neither enemies nor allies see supply orders. */
    private ArrayList<Planet> supplyPath(Planet from, Planet target) {
        HashMap<Planet, Double> distance = new HashMap<>();
        HashMap<Planet, Planet> previous = new HashMap<>();
        HashSet<Planet> visited = new HashSet<>();
        distance.put(from, 0.0);
        while (true) {
            Planet current = null;
            for (Planet node : layer.planetsManager.planets) if (distance.containsKey(node) && !visited.contains(node)
                    && (current == null || distance.get(node) < distance.get(current))) current = node;
            if (current == null) return null;
            if (current == target) break;
            visited.add(current);
            for (Link link : current.adjoinedLinks) {
                Planet next = link.getOppositePlanet(current);
                if (link.isBlueprint() || next.fraction != from.fraction || visited.contains(next)) continue;
                double d = distance.get(current) + link.distance / link.speedFrom(current);
                if (!distance.containsKey(next) || d < distance.get(next)) {
                    distance.put(next, d); previous.put(next, current);
                }
            }
        }
        ArrayList<Planet> path = new ArrayList<>();
        for (Planet at = target; at != null; at = previous.get(at)) path.add(0, at);
        return path;
    }

    private double travelTime(ArrayList<Planet> path) {
        double time = 0;
        for (int i = 1; i < path.size(); i++) {
            Planet from = path.get(i - 1); Link link = from.getLink(path.get(i));
            time += link.distance / link.speedFrom(from);
        }
        return time;
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
        if (!RuleFeatures.has(RuleFeatures.ARTILLERY) || node == null || node.isNeutral() || node.type == PlanetType.artillery || node.unitsInside < ARTILLERY_PRICE) return false;
        node.decreaseUnitsInside(ARTILLERY_PRICE);
        node.setType(PlanetType.artillery);
        node.setAutoTarget(null);
        node.artilleryReload = 0;
        return true;
    }

    public double artilleryRange() { return 3 * normalRouteLength; }

    public boolean canFire(Planet gun, Planet target) {
        return RuleFeatures.has(RuleFeatures.ARTILLERY) && gun != null && target != null && gun.type == PlanetType.artillery && !gun.isNeutral()
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
            for (FractionType other : FractionType.values()) {
                int j = other.ordinal();
                out.append("pact ").append(owner).append(' ').append(other).append(' ')
                        .append(alliances.offers[i][j]).append(' ').append(alliances.allied[i][j]).append(' ')
                        .append(alliances.broken[i][j]).append(' ').append(alliances.formedAt[i][j]).append(';');
            }
        }
        for (Planet node : layer.planetsManager.planets) if (node.bombStock > 0 || node.nodeBombs > 0) {
            out.append("depot ").append(layer.planetsManager.planets.indexOf(node)).append(' ')
                    .append(node.bombStock).append(' ').append(node.nodeBombs).append(';');
        }
        for (BombCourier courier : couriers) if (courier.active) {
            out.append("courier ").append(courier.owner).append(' ')
                    .append(layer.planetsManager.planets.indexOf(courier.destination)).append(' ')
                    .append(layer.linksManager.links.indexOf(courier.route)).append(' ').append(courier.routePosition).append(' ')
                    .append(courier.leg).append(' ').append(courier.progress.value).append(' ');
            for (Planet node : courier.path) out.append(layer.planetsManager.planets.indexOf(node)).append(',');
            out.append(';');
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
                // Older maps kept a faction-wide stock. Anchor it to an existing supply node.
                FractionType owner = FractionType.valueOf(p[1]); Planet depot = null;
                for (Planet node : layer.planetsManager.planets) if (node.fraction == owner
                        && (depot == null || node.isEconomic())) depot = node;
                if (depot != null && owner != FractionType.neutral) depot.bombStock += Math.max(0, Integer.parseInt(p[2]));
            } else if (p[0].equals("depot")) {
                Planet node = layer.planetsManager.planets.get(Integer.parseInt(p[1]));
                if (!node.isNeutral()) {
                    node.bombStock = Math.max(0, Integer.parseInt(p[2]));
                    node.nodeBombs = Math.max(0, Math.min(MAX_NODE_BOMBS, Integer.parseInt(p[3])));
                }
            } else if (p[0].equals("courier")) {
                ArrayList<Planet> path = new ArrayList<>();
                for (String index : p[7].split(",")) path.add(layer.planetsManager.planets.get(Integer.parseInt(index)));
                int node = Integer.parseInt(p[2]), route = Integer.parseInt(p[3]);
                BombCourier courier = new BombCourier(this, FractionType.valueOf(p[1]), path,
                        node < 0 ? null : layer.planetsManager.planets.get(node),
                        route < 0 ? null : layer.linksManager.links.get(route), Float.parseFloat(p[4]));
                courier.restore(Integer.parseInt(p[5]), Float.parseFloat(p[6])); couriers.add(courier);
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
            Link closest = null; float position = 0;
            double distance = .055 * GraphicsYio.width;
            for (Link link : layer.linksManager.links) {
                double dx = point.x - link.one.viewPosition.center.x, dy = point.y - link.one.viewPosition.center.y;
                float p = (float) ((dx * Math.cos(link.angle) + dy * Math.sin(link.angle)) / link.distance);
                if (p <= 0 || p >= 1) continue;
                double d = Math.abs(-dx * Math.sin(link.angle) + dy * Math.cos(link.angle));
                if (d < distance) { distance = d; closest = link; position = p; }
            }
            success = target != null ? sendBomb(source, target) : sendBomb(source, closest, position);
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
