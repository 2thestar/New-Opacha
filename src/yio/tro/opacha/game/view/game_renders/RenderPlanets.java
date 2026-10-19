package yio.tro.opacha.game.view.game_renders;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.GameMode;
import yio.tro.opacha.game.gameplay.ai.AiType;
import yio.tro.opacha.stuff.GraphicsYio;
import yio.tro.opacha.stuff.Storage3xTexture;

import java.util.HashMap;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.SettingsManager;

public class RenderPlanets extends GameRender{

    HashMap<FractionType, Storage3xTexture> mapMainTextures;
    private PlanetsManager planetsManager;
    private Storage3xTexture shadowTexture;
    HashMap<PlanetType, Storage3xTexture> mapTypeTextures;
    private Storage3xTexture selectionTexture;
    HashMap<FractionType, Storage3xTexture> mapShieldSmallTextures;
    HashMap<FractionType, Storage3xTexture> mapShieldMoreTextures;
    private Storage3xTexture autoTargetTexture;
    private Storage3xTexture highlightTexture;
    private Storage3xTexture halvedSelectionTexture;
    private AiMarkerTextures aiMarkers;


    @Override
    protected void loadTextures() {
        mapMainTextures = new HashMap<>();
        for (FractionType fractionType : FractionType.values()) {
            mapMainTextures.put(fractionType, load3xTexture("planet_" + fractionType));
        }

        mapTypeTextures = new HashMap<>();
        for (PlanetType planetType : PlanetType.values()) {
            mapTypeTextures.put(planetType, load3xTexture("type_" + (planetType == PlanetType.artillery ? PlanetType.empty : planetType)));
        }

        mapShieldSmallTextures = new HashMap<>();
        for (FractionType fractionType : FractionType.values()) {
            mapShieldSmallTextures.put(fractionType, load3xTexture("shield_" + fractionType + "_small"));
        }

        mapShieldMoreTextures = new HashMap<>();
        for (FractionType fractionType : FractionType.values()) {
            mapShieldMoreTextures.put(fractionType, load3xTexture("shield_" + fractionType + "_more"));
        }

        shadowTexture = load3xTexture("planet_shadow");
        selectionTexture = load3xTexture("selection");
        autoTargetTexture = load3xTexture("auto_target");
        highlightTexture = load3xTexture("highlight");
        halvedSelectionTexture = load3xTexture("halved_selection");
        if (aiMarkers == null) aiMarkers = new AiMarkerTextures();
    }


    @Override
    public void render() {
        planetsManager = getObjectsLayer().planetsManager;

        for (Planet planet : planetsManager.planets) {
            if (!planet.isCurrentlyVisible()) continue;
            renderSinglePlanet(planet);
        }

        for (Planet planet : planetsManager.planets) {
            if (!planet.isCurrentlyVisible()) continue;
            renderText(planet);
            renderAiMarker(planet);
        }
    }

    private void renderAiMarker(Planet planet) {
        if (gameController.gameMode == GameMode.editor) return;
        AiType type = getObjectsLayer().aiManager.getType(planet.fraction);
        if (type == null) return;
        float r = planet.viewPosition.radius;
        float size = .72f * r;
        batchMovable.draw(aiMarkers.get(type), planet.viewPosition.center.x - size / 2,
                planet.viewPosition.center.y + 1.02f * r, size, size);
    }


    public void renderShadows() {
        planetsManager = getObjectsLayer().planetsManager;

        for (Planet planet : planetsManager.planets) {
            if (!planet.isCurrentlyVisible()) continue;
            renderShadow(planet);
        }
    }


    private void renderShadow(Planet planet) {
        float r = planet.viewPosition.radius, x = planet.viewPosition.center.x, y = planet.viewPosition.center.y;
        UiStyle.get().disc(batchMovable, x + r * .10f, y - r * .20f, r * 1.12f, UiStyle.INK, .06f);
        UiStyle.get().disc(batchMovable, x + r * .07f, y - r * .14f, r, UiStyle.INK, .13f);
    }


    private void renderSinglePlanet(Planet planet) {
        if (planet.capturePulse > 0) {
            double radius = planet.viewPosition.radius * (1.1 + .5 * (1 - planet.capturePulse));
            double x = planet.viewPosition.center.x, y = planet.viewPosition.center.y;
            for (int i = 0; i < 32; i++) {
                double a = i * Math.PI / 16, b = (i + 1) * Math.PI / 16;
                UiStyle.get().line(batchMovable, x + radius * Math.cos(a), y + radius * Math.sin(a),
                        x + radius * Math.cos(b), y + radius * Math.sin(b),
                        planet.viewPosition.radius * .09, UiStyle.GOLD, planet.capturePulse * .8f);
            }
        }
        renderHighlight(planet);
        renderSelection(planet);
        renderShields(planet);
        renderAutoTarget(planet);
        renderMainPart(planet);
        renderViewType(planet);
    }


    private void renderHighlight(Planet planet) {
        if (!planet.highlightEngine.isSelected()) return;

        GraphicsYio.setBatchAlpha(batchMovable, planet.highlightEngine.getAlpha() / 2);
        GraphicsYio.drawByCircle(batchMovable, highlightTexture.getTexture(getCurrentZoomQuality()), planet.highlightViewPosition);
        GraphicsYio.setBatchAlpha(batchMovable, 1);
    }


    private void renderAutoTarget(Planet planet) {
        if (planet.atFactor.get() == 0) return;

        GraphicsYio.drawByCircle(
                batchMovable,
                autoTargetTexture.getTexture(getCurrentZoomQuality()),
                planet.atViewPosition
        );
    }


    private void renderText(Planet planet) {
        GraphicsYio.renderText(batchMovable, planet.text);
    }


    private void renderShields(Planet planet) {
        for (Shield shield : planet.shields) {
            if (!shield.isActive()) continue;
            GraphicsYio.drawByCircle(
                    batchMovable,
                    getShieldTexture(planet),
                    shield.viewPosition
            );
        }
    }


    private TextureRegion getShieldTexture(Planet planet) {
        if (planet.isDefensive()) {
            return mapShieldMoreTextures.get(planet.fraction).getTexture(getCurrentZoomQuality());
        }

        return mapShieldSmallTextures.get(planet.fraction).getTexture(getCurrentZoomQuality());
    }


    private void renderMainPart(Planet planet) {
        if (!SettingsManager.getInstance().colorblindMode) {
            float r = planet.viewPosition.radius, x = planet.viewPosition.center.x, y = planet.viewPosition.center.y;
            UiStyle skin = UiStyle.get();
            skin.disc(batchMovable, x, y, r, UiStyle.INK, 1);
            skin.disc(batchMovable, x, y + r * .055f, r * .89f, UiStyle.faction(planet.fraction), 1);
            skin.disc(batchMovable, x - r * .28f, y + r * .48f, r * .18f, UiStyle.WHITE, .40f);
            return;
        }
        GraphicsYio.drawByCircle(
                batchMovable,
                mapMainTextures.get(planet.fraction).getTexture(getCurrentZoomQuality()),
                planet.viewPosition
        );
    }


    private void renderViewType(Planet planet) {
        if (planet.viewTypeFactor.get() < 1) {
            GraphicsYio.setBatchAlpha(batchMovable, planet.viewTypeFactor.get());
        }

        GraphicsYio.drawByCircle(
                batchMovable,
                mapTypeTextures.get(planet.type).getTexture(getCurrentZoomQuality()),
                planet.viewPosition
        );

        if (planet.viewTypeFactor.get() < 1) {
            GraphicsYio.setBatchAlpha(batchMovable, 1);
        }
    }


    private void renderSelection(Planet planet) {
        if (!planet.selectionEngineYio.isSelected()) return;

        if (!planet.halvedSelectionMode) {
            float r = planet.selectionPosition.radius;
            UiStyle.get().disc(batchMovable, planet.selectionPosition.center.x, planet.selectionPosition.center.y,
                    r, UiStyle.GOLD, .7f);
            UiStyle.get().disc(batchMovable, planet.selectionPosition.center.x, planet.selectionPosition.center.y,
                    r * .90f, UiStyle.BOARD, 1);
            return;
        }

        GraphicsYio.drawByCircle(
                batchMovable,
                getSelectionTexture(planet),
                planet.selectionPosition
        );
    }


    private TextureRegion getSelectionTexture(Planet planet) {
        if (planet.halvedSelectionMode) {
            return halvedSelectionTexture.getTexture(getCurrentZoomQuality());
        }

        return selectionTexture.getTexture(getCurrentZoomQuality());
    }


    @Override
    protected void disposeTextures() {
        if (aiMarkers != null) {
            aiMarkers.dispose();
            aiMarkers = null;
        }
    }
}
