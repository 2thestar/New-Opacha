package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.game.VelocityManager;
import yio.tro.opacha.stuff.PointYio;
import java.util.ArrayList;

/** A bomb and its two-person crew. Secret supply traffic never becomes an attack fleet. */
public final class BombCourier {
    public final FractionType owner;
    public final ArrayList<Planet> path;
    public final Planet destination;
    public final Link route;
    public final float routePosition;
    public final AcceleratableFactor progress = new AcceleratableFactor();
    public Link link;
    public int leg = 1;
    public boolean active = true;
    private final TacticsManager tactics;

    BombCourier(TacticsManager tactics, FractionType owner, ArrayList<Planet> path,
                Planet destination, Link route, float routePosition) {
        this.tactics = tactics; this.owner = owner; this.path = path;
        this.destination = destination; this.route = route; this.routePosition = routePosition;
        startLeg();
    }

    private void startLeg() {
        startLeg(true);
    }

    private void startLeg(boolean checkDeparture) {
        Planet from = path.get(Math.min(leg - 1, path.size() - 1));
        if (checkDeparture && from.fraction != owner) { active = false; return; }
        if (leg < path.size()) link = from.getLink(path.get(leg));
        else if (route != null) link = route;
        else {
            if (destination.fraction == owner && destination.nodeBombs < TacticsManager.MAX_NODE_BOMBS) destination.nodeBombs++;
            active = false; return;
        }
        if (link == null || link.isBlueprint()) { active = false; return; }
        if (link.one == from) progress.appear(link.movementMultiplier * link.speedFrom(from));
        else progress.destroy(link.movementMultiplier * link.speedFrom(from));
    }

    public void move() {
        if (!active || VelocityManager.getInstance().value <= 0) return;
        float before = progress.value;
        progress.move(VelocityManager.getInstance().value);
        boolean placing = leg >= path.size();
        boolean arrived = progress.isInAppearState() ? progress.value >= (placing ? routePosition : 1)
                : progress.value <= (placing ? routePosition : 0);
        if (arrived && placing) progress.value = routePosition;
        // Hidden couriers pass fleets, but a planted enemy route bomb still destroys them.
        for (RouteBomb bomb : tactics.bombs) {
            if (!bomb.active || bomb.link != link || tactics.alliances.friendly(owner, bomb.owner)) continue;
            if (bomb.position < Math.min(before, progress.value) - .00001f
                    || bomb.position > Math.max(before, progress.value) + .00001f) continue;
            bomb.active = false; active = false;
            tactics.layer.particlesManager.spawnExplosion(position(), 2,
                    tactics.layer.particlesManager.convertFractionIntoPaViewType(bomb.owner));
            return;
        }
        if (!arrived) return;
        if (placing) {
            tactics.bombs.add(new RouteBomb(route, owner, routePosition)); active = false;
        } else {
            if (path.get(leg).fraction != owner) { active = false; return; }
            leg++; startLeg();
        }
    }

    public PointYio position() {
        return new PointYio(link.one.viewPosition.center.x + Math.cos(link.angle) * link.distance * progress.value,
                link.one.viewPosition.center.y + Math.sin(link.angle) * link.distance * progress.value);
    }

    public boolean visibleTo(FractionType viewer) { return active && owner == viewer; }

    public boolean uses(Link route) {
        if (link == route || this.route == route) return true;
        for (int i = 1; i < path.size(); i++) {
            if (route.contains(path.get(i - 1)) && route.contains(path.get(i))) return true;
        }
        return false;
    }

    void restore(int leg, float position) {
        this.leg = leg; active = true; startLeg(false); progress.value = position;
    }
}
