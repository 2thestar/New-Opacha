package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.game.VelocityManager;
import yio.tro.opacha.stuff.PointYio;

/** Secret cargo, separate from combat troops and public AI threat calculations. */
public final class MobileBomb {
    public final FractionType owner;
    public Link link;
    public Planet target, node;
    public final AcceleratableFactor progress = new AcceleratableFactor();
    public boolean active = true;
    private final PointYio position = new PointYio();

    public MobileBomb(FractionType owner, Link link, Planet target, float position) {
        this.owner = owner;
        travel(link, target);
        progress.value = position;
    }

    public MobileBomb(FractionType owner, Planet node) {
        this.owner = owner;
        this.node = node;
    }

    private void travel(Link route, Planet destination) {
        link = route; target = destination; node = null;
        Planet start = link.getOppositePlanet(target);
        double speed = link.movementMultiplier * link.speedFrom(start);
        if (start == link.one) progress.appear(speed); else progress.destroy(speed);
    }

    public boolean followArrow() {
        if (!active || node == null || node.fraction != owner || node.autoTarget == null) return false;
        Link route = node.getLink(node.autoTarget);
        if (route == null || route.isBlueprint() || !route.canUse(node, owner)) return false;
        travel(route, node.autoTarget);
        return true;
    }

    public void move() {
        if (!active || VelocityManager.getInstance().value <= 0) return;
        if (node != null) { followArrow(); return; }
        progress.move(VelocityManager.getInstance().value);
        if (progress.needsToMove) return;
        node = target; link = null; target = null;
        followArrow();
    }

    public boolean visibleTo(FractionType viewer) { return active && owner == viewer; }

    public PointYio position() {
        if (node != null) {
            position.setBy(node.viewPosition.center);
            position.x -= .94f * node.viewPosition.radius;
            position.y += .94f * node.viewPosition.radius;
            return position;
        }
        Planet start = link.getOppositePlanet(target);
        double p = start == link.one ? progress.value : 1 - progress.value;
        double radius = .029 * yio.tro.opacha.stuff.GraphicsYio.width;
        double from = start.viewPosition.radius + radius * .82;
        double to = link.distance - target.viewPosition.radius - radius * .82;
        position.setBy(start.viewPosition.center);
        position.relocateRadial(from + p * (to - from), start.position.center.angleTo(target.position.center));
        return position;
    }
}
