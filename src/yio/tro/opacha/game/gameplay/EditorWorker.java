package yio.tro.opacha.game.gameplay;

import yio.tro.opacha.game.LevelSize;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.PlayType;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.CircleYio;
import yio.tro.opacha.stuff.PointYio;

import java.util.ArrayList;

public class EditorWorker implements IGameplayManager{

    ObjectsLayer objectsLayer;
    double minPlanetOffset;
    CircleYio tempCircle;
    ArrayList<Link> tempLinkList;
    boolean tagFractions[];
    public int nextRouteStyle;


    public EditorWorker(ObjectsLayer objectsLayer) {
        this.objectsLayer = objectsLayer;
        minPlanetOffset = 3 * getPlanetsManager().getPlanetRadius();
        tempCircle = new CircleYio();
        tempLinkList = new ArrayList<>();
        tagFractions = new boolean[FractionType.values().length];
    }


    @Override
    public void defaultValues() {
        nextRouteStyle = 0;
    }


    @Override
    public void onEndCreation() {

    }


    public void onLinkAdditionRequested(Planet one, Planet two) {
        LinksManager linksManager = objectsLayer.linksManager;
        if (nextRouteStyle == 3 && one.isNeutral()) { Scenes.notification.show("editor_blueprint_owner"); return; }
        Link link = one.getLink(two);
        if (link == null) link = linksManager.addLink(one, two);
        else objectsLayer.tactics.onLinkEdited(link, link.one != (nextRouteStyle == 2 ? two : one), nextRouteStyle == 3);
        link.one = nextRouteStyle == 2 ? two : one;
        link.two = nextRouteStyle == 2 ? one : two;
        link.directional = nextRouteStyle != 0;
        link.builder = nextRouteStyle == 3 ? one.fraction : null;
        link.constructionCost = nextRouteStyle == 3 ? Math.max(1, (int) Math.ceil(80 * link.distance / objectsLayer.tactics.normalRouteLength)) : 0;
        link.constructionSpent = 0;
        syncLinkFraction(link);
        link.initNPoints();
    }


    public void onLinkRemovalRequested(Planet one, Planet two) {
        if (!one.isLinkedTo(two)) return;

        objectsLayer.linksManager.removeLink(one, two);
    }


    public void onPlanetRemovalRequested(PointYio pos) {
        Planet closestPlanet = findClosestPlanet(pos);
        if (closestPlanet == null) return;
        if (closestPlanet.position.center.distanceTo(pos) > 1.5 * closestPlanet.position.radius) return;

        tempLinkList.clear();
        tempLinkList.addAll(closestPlanet.adjoinedLinks);
        for (Link link : tempLinkList) {
            onLinkRemovalRequested(link.one, link.two);
        }

        getPlanetsManager().removePlanet(closestPlanet);
        if (objectsLayer.tactics.flagPlanet == closestPlanet) objectsLayer.tactics.flagPlanet = null;
        for (int i = 0; i < objectsLayer.tactics.homeFlags.length; i++)
            if (objectsLayer.tactics.homeFlags[i] == closestPlanet) objectsLayer.tactics.homeFlags[i] = null;
    }


    public void onPlanetFractionChanged(Planet planet) {
        if (objectsLayer.tactics.flagPlanet == planet && !planet.isNeutral()) objectsLayer.tactics.flagPlanet = null;
        for (int i = 0; i < objectsLayer.tactics.homeFlags.length; i++)
            if (objectsLayer.tactics.homeFlags[i] == planet && planet.fraction.ordinal() != i) objectsLayer.tactics.homeFlags[i] = null;
        for (Link adjoinedLink : planet.adjoinedLinks) {
            syncLinkFraction(adjoinedLink);
        }
    }


    public void syncLinkFraction(Link link) {
        FractionType preferredFractionTypeForLink = getPreferredFractionTypeForLink(link);
        if (link.fractionType == preferredFractionTypeForLink) return;

        link.setFractionType(preferredFractionTypeForLink);
    }


    public void onRandomGenerationRequested() {
        LevelSize initialLevelSize = objectsLayer.gameController.initialLevelSize;

        int colors = detectColorsForRandomGeneration();
        int mapSize = getMapSizeByLevelSize(initialLevelSize);
        objectsLayer.mapGenerator.generateLinkedMap(mapSize, colors);
        objectsLayer.tactics = new TacticsManager(objectsLayer);
        objectsLayer.tactics.onEndCreation();
        for (Link link : objectsLayer.linksManager.links) {
            link.updateMetrics();
            link.initNPoints();
        }
        applyMaxShields();

        objectsLayer.gameController.cameraController.applyMaxZoom();
    }


    private int detectColorsForRandomGeneration() {
        updateTagFractions();

        int counter = 0;
        for (boolean tagFraction : tagFractions) {
            if (!tagFraction) continue;
            counter++;
        }

        return Math.max(2, counter);
    }


    private void updateTagFractions() {
        clearTagFractions();
        for (Planet planet : getPlanetsManager().planets) {
            if (planet.isNeutral()) continue;
            tagFractions[planet.fraction.ordinal()] = true;
        }
    }


    private void clearTagFractions() {
        for (int i = 0; i < tagFractions.length; i++) {
            tagFractions[i] = false;
        }
    }


    private int getMapSizeByLevelSize(LevelSize levelSize) {
        switch (levelSize) {
            default:
                return 5;
            case tiny:
                return 1;
            case small:
                return 3;
            case normal:
                return 5;
        }
    }


    void applyMaxShields() {
        for (Planet planet : getPlanetsManager().planets) {
            if (planet.isEconomic()) continue;
            planet.setCurrentDefense(planet.maxDefense);
        }
    }


    private FractionType getPreferredFractionTypeForLink(Link link) {
        if (link.one.fraction != link.two.fraction) return FractionType.neutral;
        return link.one.fraction;
    }


    public void onPlanetAdditionRequested(PointYio pos) {
        if (!isPointInValidPlace(pos)) return;

        Planet closestPlanet = findClosestPlanet(pos);
        if (closestPlanet != null && closestPlanet.position.center.distanceTo(pos) < minPlanetOffset) return;

        tempCircle.center.setBy(pos);
        tempCircle.setRadius(getPlanetsManager().getPlanetRadius());
        getPlanetsManager().spawnPlanet(tempCircle, FractionType.neutral);
    }


    public void onLaunchButtonPressed() {
        if (!isLevelValidForLaunch()) { Scenes.notification.show("editor_invalid_map"); return; }
        objectsLayer.tactics.onEndCreation();
        Scenes.editorPrepareToLaunch.create();
    }


    public boolean isLevelValidForLaunch() {
        ArrayList<Planet> planets = objectsLayer.planetsManager.planets;
        if (planets.size() < 2) return false;

        updateTagFractions();
        int c = 0;
        for (boolean tag : tagFractions) {
            if (!tag) continue;
            c++;
        }

        if (c < 2 || (!GameRules.aiOnlyMode && !tagFractions[FractionType.green.ordinal()])) return false;
        if (GameRules.playType == PlayType.alliances && c < 3) return false;
        if (GameRules.playType == PlayType.capture_flag) {
            boolean neutral = false;
            for (Planet planet : planets) if (planet.isNeutral()) neutral = true;
            if (!neutral) return false;
        }
        return true;
    }

    public void setPlayType(PlayType type) {
        TacticsManager tactics = objectsLayer.tactics;
        if (tactics.flagPlanet != null) {
            tactics.flagPlanet.position.setRadius(getPlanetsManager().getPlanetRadius());
            tactics.flagPlanet.onPositionChanged();
        }
        tactics.flagPlanet = null; tactics.flagWinner = null;
        java.util.Arrays.fill(tactics.homeFlags, null);
        GameRules.playType = type;
        tactics.onEndCreation();
    }

    public void markFlag(Planet node) {
        if (GameRules.playType == PlayType.capture_flag && node.isNeutral()) {
            Planet old = objectsLayer.tactics.flagPlanet;
            if (old != null) { old.position.setRadius(getPlanetsManager().getPlanetRadius()); old.onPositionChanged(); }
            objectsLayer.tactics.flagPlanet = node; node.setUnitsInside(50);
            node.position.setRadius(1.3 * getPlanetsManager().getPlanetRadius()); node.onPositionChanged();
        } else if (GameRules.playType == PlayType.home_flags && !node.isNeutral()) {
            objectsLayer.tactics.homeFlags[node.fraction.ordinal()] = node;
        } else Scenes.notification.show("editor_flag_mode");
    }


    public void onPlanetRelocationRequested(Planet planet, PointYio targetPosition) {
        if (!isPointInValidPlace(targetPosition)) return;

        planet.setPosition(targetPosition.x, targetPosition.y, planet.position.radius);
        for (Link adjoinedLink : planet.adjoinedLinks) {
            adjoinedLink.updateMetrics();
            adjoinedLink.initNPoints();
        }
    }


    public boolean isPointInValidPlace(PointYio pos) {
        float planetRadius = getPlanetsManager().getPlanetRadius();
        if (pos.x < planetRadius) return false;
        if (pos.y < planetRadius) return false;
        if (pos.x > objectsLayer.gameController.boundWidth - planetRadius) return false;
        if (pos.y > objectsLayer.gameController.boundHeight - planetRadius) return false;
        return true;
    }


    public Planet findClosestPlanet(PointYio pos) {
        Planet result = null;
        double minDistance = 0;
        double currentDistance;
        for (Planet planet : getPlanetsManager().planets) {
            currentDistance = planet.position.center.distanceTo(pos);
            if (result == null || currentDistance < minDistance) {
                result = planet;
                minDistance = currentDistance;
            }
        }
        return result;
    }


    private PlanetsManager getPlanetsManager() {
        return objectsLayer.planetsManager;
    }


    @Override
    public void moveActually() {

    }


    @Override
    public void moveVisually() {

    }
}
