package yio.tro.opacha.game.view.game_renders;

import yio.tro.opacha.Yio;
import yio.tro.opacha.game.gameplay.model.FractionType;
import yio.tro.opacha.game.gameplay.model.Link;
import yio.tro.opacha.game.gameplay.model.LinksManager;
import yio.tro.opacha.stuff.CircleYio;
import yio.tro.opacha.stuff.GraphicsYio;
import yio.tro.opacha.stuff.Storage3xTexture;

import java.util.HashMap;
import yio.tro.opacha.menu.UiStyle;

public class RenderLinks extends GameRender{

    HashMap<FractionType, Storage3xTexture> mapTextures;
    private LinksManager linksManager;
    private Storage3xTexture shadowTexture;


    @Override
    protected void loadTextures() {
        mapTextures = new HashMap<>();
        for (FractionType fractionType : FractionType.values()) {
            mapTextures.put(fractionType, load3xTexture("link_" + fractionType));
        }
        shadowTexture = load3xTexture("link_shadow");
    }


    @Override
    public void render() {
        linksManager = getObjectsLayer().linksManager;

        for (Link link : linksManager.links) {
            if (!link.isCurrentlyVisible()) continue;
            renderSingleLink(link);
        }
    }


    public void renderShadows() {
        // Nodes carry the depth; routes stay flat and easy to follow.
    }


    private void renderSingleShadow(Link link) {
        if (link.isBlueprint()) return;
        if (link.isNeutral()) return;

        batchMovable.draw(
                shadowTexture.getTexture(getCurrentZoomQuality()),
                link.one.viewPosition.center.x,
                (float) (link.one.viewPosition.center.y - 4.3f * link.thickness * 0.5),
                0f,
                4.3f * link.thickness * 0.5f,
                (float) link.distance,
                4.3f * link.thickness,
                1f,
                1f,
                (float) (180 / Math.PI * link.angle)
        );
    }


    private void renderNeutralLink(Link link) {
        double dx = Math.cos(link.angle), dy = Math.sin(link.angle);
        double start = link.one.viewPosition.radius, end = link.distance - link.two.viewPosition.radius;
        double dash = Math.max(w * .012, link.thickness * 2.2);
        for (double d = start; d < end; d += dash * 2.2) {
            double e = Math.min(end, d + dash);
            UiStyle.get().line(batchMovable, link.one.viewPosition.center.x + dx * d,
                    link.one.viewPosition.center.y + dy * d, link.one.viewPosition.center.x + dx * e,
                    link.one.viewPosition.center.y + dy * e, link.thickness * .60, UiStyle.MUTED, .55f);
        }
    }


    private void renderSingleLink(Link link) {
        if (link.isBlueprint()) return;
        if (link.isNeutral()) {
            renderNeutralLink(link);
            return;
        }

        UiStyle.get().line(batchMovable, link.one.viewPosition.center.x, link.one.viewPosition.center.y,
                link.two.viewPosition.center.x, link.two.viewPosition.center.y, link.thickness * 1.25, UiStyle.OUTLINE, .65f);
        UiStyle.get().line(batchMovable, link.one.viewPosition.center.x, link.one.viewPosition.center.y,
                link.two.viewPosition.center.x, link.two.viewPosition.center.y, link.thickness * .65,
                UiStyle.faction(link.fractionType), 1);
    }


    @Override
    protected void disposeTextures() {

    }
}
