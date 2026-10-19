package yio.tro.opacha.game.view.game_renders;

import com.badlogic.gdx.graphics.Color;
import yio.tro.opacha.Fonts;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.stuff.*;
import yio.tro.opacha.menu.UiStyle;

public final class RenderTactics extends GameRender {
    private TacticsTextures textures;
    private final RenderableTextYio cost = new RenderableTextYio();
    @Override protected void loadTextures() {
        if (textures == null) textures = new TacticsTextures();
        cost.setFont(Fonts.unitFont);
    }
    private void line(double x1, double y1, double x2, double y2, double width) {
        GraphicsYio.drawLine(batchMovable, textures.get("pixel"), x1, y1, x2, y2, width);
    }
    private void tint(FractionType owner, float alpha) {
        switch (owner) {
            case green: batchMovable.setColor(.52f, .87f, .60f, alpha); break;
            case red: batchMovable.setColor(.88f, .51f, .56f, alpha); break;
            case cyan: batchMovable.setColor(.50f, .86f, .88f, alpha); break;
            case yellow: batchMovable.setColor(.87f, .88f, .51f, alpha); break;
            default: batchMovable.setColor(.98f, .84f, .51f, alpha);
        }
    }
    public void renderRoutes() {
        for (Link link : getObjectsLayer().linksManager.links) {
            if (!link.directional) continue;
            double x = link.one.viewPosition.center.x, y = link.one.viewPosition.center.y;
            double dx = Math.cos(link.angle), dy = Math.sin(link.angle);
            if (link.isBlueprint()) {
                tint(link.builder, .55f);
                double gap = .022 * w;
                for (double d = link.one.position.radius; d < link.distance - link.two.position.radius; d += 2 * gap) {
                    double end = Math.min(link.distance - link.two.position.radius, d + gap);
                    line(x + dx * d, y + dy * d, x + dx * end, y + dy * end, .003 * w);
                }
                tint(link.builder, .85f);
                line(x, y, x + dx * link.distance * link.constructionProgress(), y + dy * link.distance * link.constructionProgress(), .005 * w);
                // Remaining labour is shown once, beside the unbuilt route.
                cost.setString("" + (link.constructionCost - link.constructionSpent)); cost.updateMetrics();
                cost.position.set(x + dx * link.distance * .5 - cost.width / 2, y + dy * link.distance * .5 + .025 * w);
                cost.font.setColor(UiStyle.TEXT);
                GraphicsYio.renderText(batchMovable, cost);
                cost.font.setColor(UiStyle.INK);
            }
            batchMovable.setColor(UiStyle.TEXT.r, UiStyle.TEXT.g, UiStyle.TEXT.b, .65f);
            double size = .008 * w;
            for (double p = .3; p <= .7; p += .1) {
                double cx = x + dx * link.distance * p, cy = y + dy * link.distance * p;
                line(cx - dx * size - dy * size, cy - dy * size + dx * size, cx, cy, .0025 * w);
                line(cx - dx * size + dy * size, cy - dy * size - dx * size, cx, cy, .0025 * w);
            }
        }
        TacticsManager t = getObjectsLayer().tactics;
        Planet gun = t.pending == TacticsManager.Action.artillery ? t.source : getObjectsLayer().planetsManager.getCurrentlySelectedPlanet();
        if (gun != null && gun.type == PlanetType.artillery) {
            if (UiStyle.isNight()) batchMovable.setColor(UiStyle.GOLD.r, UiStyle.GOLD.g, UiStyle.GOLD.b, .50f);
            else batchMovable.setColor(.50f, .36f, .10f, .38f);
            circle(gun.viewPosition.center.x, gun.viewPosition.center.y, t.artilleryRange(), 1, true);
            if (t.pending == TacticsManager.Action.artillery) for (Planet target : getObjectsLayer().planetsManager.planets) {
                if (t.canFire(gun, target)) circle(target.viewPosition.center.x, target.viewPosition.center.y, 1.4 * target.viewPosition.radius, 1, false);
            }
        }
        batchMovable.setColor(Color.WHITE);
    }

    @Override public void render() {
        TacticsManager t = getObjectsLayer().tactics;
        for (RouteBomb bomb : t.bombs) if (bomb.active) {
            double x = bomb.link.one.viewPosition.center.x + Math.cos(bomb.link.angle) * bomb.link.distance * bomb.position;
            double y = bomb.link.one.viewPosition.center.y + Math.sin(bomb.link.angle) * bomb.link.distance * bomb.position;
            batchMovable.setColor(Color.WHITE); glyph("bomb", x, y, .045 * w);
        }
        for (Planet node : getObjectsLayer().planetsManager.planets) {
            float x = node.viewPosition.center.x, y = node.viewPosition.center.y, r = node.viewPosition.radius;
            if (node.type == PlanetType.artillery) {
                batchMovable.setColor(UiStyle.TEXT);
                glyph("gun", x, y - 1.22 * r, 1.1 * r);
                if (node.artilleryReload > 0) {
                    if (UiStyle.isNight()) batchMovable.setColor(UiStyle.GOLD);
                    else batchMovable.setColor(.58f, .40f, .08f, .9f);
                    circle(x, y, 1.13 * r, 1 - node.artilleryReload / TacticsManager.RELOAD_SECONDS, false);
                }
            }
            if (GameRules.playType == PlayType.capture_flag && node == t.flagPlanet) {
                tint(FractionType.neutral, 1); glyph("flag", x + 1.28 * r, y + .75 * r, 1.3 * r);
            }
            if (GameRules.playType == PlayType.home_flags) for (FractionType owner : FractionType.values()) {
                if (owner != FractionType.neutral && t.homeFlags[owner.ordinal()] == node) {
                    tint(owner, 1); glyph("flag", x + 1.28 * r, y + .75 * r, 1.3 * r);
                }
            }
        }
        for (Unit unit : getObjectsLayer().unitManager.units) if (unit.isAlive() && unit.digger) {
            if (UiStyle.isNight()) batchMovable.setColor(UiStyle.GOLD);
            else batchMovable.setColor(.50f, .36f, .10f, 1);
            glyph("digger", unit.viewPosition.center.x, unit.viewPosition.center.y - .02 * w, .03 * w);
        }
        if (t.shotSource != null && t.elapsed < t.shotUntil) {
            if (UiStyle.isNight()) batchMovable.setColor(UiStyle.RED);
            else batchMovable.setColor(.75f, .30f, .20f, .85f);
            PointYio a = t.shotSource.viewPosition.center, b = t.shotTarget.viewPosition.center;
            line(a.x, a.y, b.x, b.y, .006 * w);
        }
        batchMovable.setColor(Color.WHITE);
    }
    private void glyph(String key, double x, double y, double size) {
        batchMovable.draw(textures.get(key), (float) (x - size / 2), (float) (y - size / 2), (float) size, (float) size);
    }
    private void circle(double x, double y, double radius, double portion, boolean dashed) {
        int steps = 96;
        for (int i = 0; i < steps * portion; i++) {
            if (dashed && i % 3 == 2) continue;
            double a = Math.PI * 2 * i / steps, b = Math.PI * 2 * (i + 1) / steps;
            if (dashed) {
                double px = x + radius * Math.cos(a), py = y + radius * Math.sin(a);
                double qx = x + radius * Math.cos(b), qy = y + radius * Math.sin(b);
                if (px < 0 || qx < 0 || py < 0 || qy < 0 || px > gameController.boundWidth || qx > gameController.boundWidth
                        || py > gameController.boundHeight || qy > gameController.boundHeight) continue;
            }
            line(x + radius * Math.cos(a), y + radius * Math.sin(a), x + radius * Math.cos(b), y + radius * Math.sin(b), .0025 * w);
        }
    }
    @Override protected void disposeTextures() { if (textures != null) { textures.dispose(); textures = null; } }
}
