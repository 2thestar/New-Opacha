package yio.tro.opacha.game.export_import;

import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.ai.AiType;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.stuff.CircleYio;
import yio.tro.opacha.stuff.GraphicsYio;
import yio.tro.opacha.stuff.PointYio;

public class ImportManager {

    GameController gameController;
    private String source;
    private ObjectsLayer objectsLayer;
    CircleYio tempCircle;
    PointYio tempPoint;


    public ImportManager(GameController gameController) {
        this.gameController = gameController;
        tempCircle = new CircleYio();
        tempPoint = new PointYio();
    }


    public void perform(String src) {
        source = src;
        objectsLayer = gameController.objectsLayer;

        applyGameRules();
        applyAiTypes();
        applyPlanets();
        applyLinks();
        objectsLayer.tactics.load(getSection("tactics"));

        finish();
    }


    private void applyLinks() {
        String linksSection = getSection("links");
        if (linksSection == null) return;

        for (String token : linksSection.split(",")) {
            applySingleLink(token);
        }
    }


    private void applySingleLink(String token) {
        String[] split = token.split(" ");
        FractionType fractionType = FractionType.valueOf(split[0]);
        double rx1 = Double.valueOf(split[1]);
        double ry1 = Double.valueOf(split[2]);
        double rx2 = Double.valueOf(split[3]);
        double ry2 = Double.valueOf(split[4]);

        PlanetsManager planetsManager = objectsLayer.planetsManager;
        tempPoint.set(rx1 * GraphicsYio.width, ry1 * GraphicsYio.width);
        Planet one = findEndpoint(planetsManager, tempPoint);
        tempPoint.set(rx2 * GraphicsYio.width, ry2 * GraphicsYio.width);
        Planet two = findEndpoint(planetsManager, tempPoint);

        Link link = objectsLayer.linksManager.addLink(one, two);
        link.setFractionType(fractionType);
        if (split.length >= 9) {
            link.directional = Boolean.parseBoolean(split[5]);
            link.builder = split[6].equals("none") ? null : FractionType.valueOf(split[6]);
            link.constructionCost = Integer.parseInt(split[7]);
            link.constructionSpent = Integer.parseInt(split[8]);
        }
    }

    /** Saved coordinates are model data; a finger target must never change map import. */
    private Planet findEndpoint(PlanetsManager manager, PointYio point) {
        Planet result = manager.findClosestPlanet(point, null);
        if (result == null || result.position.center.distanceTo(point) > .002 * GraphicsYio.width)
            throw new IllegalArgumentException("Route endpoint does not match a saved node");
        return result;
    }


    private void applyPlanets() {
        String planetsSection = getSection("planets");
        if (planetsSection == null) return;

        for (String token : planetsSection.split(",")) {
            applySinglePlanet(token);
        }
    }


    private void applySinglePlanet(String token) {
        String[] split = token.split(" ");
        FractionType fractionType = FractionType.valueOf(split[0]);
        PlanetType planetType = PlanetType.valueOf(split[1]);
        int unitsInside = Integer.valueOf(split[2]);
        double rx = Double.valueOf(split[3]);
        double ry = Double.valueOf(split[4]);

        PlanetsManager planetsManager = objectsLayer.planetsManager;
        tempCircle.setRadius(planetsManager.getPlanetRadius());
        tempCircle.center.set(rx * GraphicsYio.width, ry * GraphicsYio.width);

        Planet planet = planetsManager.spawnPlanet(tempCircle, fractionType);
        planet.setType(planetType);
        planet.setUnitsInside(unitsInside);
        planet.applyMaxShields();
        if (split.length >= 7) {
            planet.artilleryReload = Float.parseFloat(split[5]);
            planet.setCurrentDefense(Float.parseFloat(split[6]));
        }
    }


    private void applyGameRules() {
        String gameRulesSection = getSection("game_rules");
        if (gameRulesSection == null) return;

        String[] split = gameRulesSection.split(" ");

        GameRules.difficulty = Difficulty.valueOf(split[0]);
        GameRules.playType = split.length > 2 ? PlayType.valueOf(split[2]) : PlayType.conquest;
        GameRules.directionalRoutes = split.length > 3 && Boolean.parseBoolean(split[3]);
        GameRules.features = split.length > 4 ? Integer.parseInt(split[4]) & RuleFeatures.ALL : RuleFeatures.ALL;
        if (split.length > 1) {
            GameRules.aiOnlyMode = Boolean.valueOf(split[1]);
        }
    }


    private void finish() {

    }

    private void applyAiTypes() {
        GameRules.aiTypes.clear();
        String section = getSection("ai_types");
        if (section == null) return; // Original maps retain the original AI.
        for (String token : section.split(",")) {
            String[] pair = token.trim().split("\\s+");
            if (pair.length != 2) continue;
            try {
                FractionType fraction = FractionType.valueOf(pair[0]);
                if (fraction != FractionType.neutral) GameRules.aiTypes.put(fraction, AiType.valueOf(pair[1]));
            } catch (IllegalArgumentException ignored) {
                // An unknown personality in a newer map falls back to Simple.
            }
        }
    }


    public Difficulty getDifficultyFromLevelCode(String levelCode) {
        source = levelCode;
        String gameRulesSection = getSection("game_rules");
        if (gameRulesSection == null) return Difficulty.hard;

        String[] split = gameRulesSection.split(" ");

        return Difficulty.valueOf(split[0]);
    }


    public LevelSize getLevelSizeFromLevelCode(String levelCode) {
        source = levelCode;
        String generalSection = getSection("general");
        if (generalSection == null) return null;

        String[] split = generalSection.split(" ");
        return LevelSize.valueOf(split[0]);
    }


    private String getSection(String name) {
        int nameIndex = source.indexOf(name);
        if (nameIndex == -1) return null;
        int colonIndex = source.indexOf(":", nameIndex);
        int hashIndex = source.indexOf("#", colonIndex);
        if (hashIndex - colonIndex < 2) return null;

        return source.substring(colonIndex + 1, hashIndex);
    }
}
