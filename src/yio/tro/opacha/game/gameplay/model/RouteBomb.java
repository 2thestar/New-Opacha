package yio.tro.opacha.game.gameplay.model;

public final class RouteBomb {
    public final Link link;
    public final FractionType owner;
    public final float position;
    public boolean active = true;

    public RouteBomb(Link link, FractionType owner, float position) {
        this.link = link; this.owner = owner; this.position = position;
    }
}
