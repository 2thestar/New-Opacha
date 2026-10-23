package yio.tro.opacha.game.gameplay.model;

import java.util.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.campaign.*;
import yio.tro.opacha.menu.UiHaptics;
import yio.tro.opacha.stuff.*;

/** Validate authored data against the real importer, graph and editor model. */
public final class CampaignChecks {
    private static int checks;
    public static void run(GameController controller) {
        checks = 0; int[] counts = new int[4];
        for (int index = 0; index < CampaignLevels.COUNT; index++) {
            CampaignLevels.Level level = CampaignLevels.get(index);
            controller.gameMode = GameMode.custom; controller.initLevelSize(LevelSize.tiny); controller.createGameObjects();
            controller.importManager.perform(level.code); controller.objectsLayer.onEndCreation();
            ArrayList<Planet> nodes = controller.objectsLayer.planetsManager.planets;
            check(level.index == index && GameRules.playType.toString().equals(level.mode), "Wrong mode/index " + index);
            check(GameRules.features == level.features, "Lesson features lost " + index);
            counts[GameRules.playType.ordinal()]++;
            Set<Planet> seen = new HashSet<>(); ArrayDeque<Planet> queue = new ArrayDeque<>();
            queue.add(nodes.get(0)); seen.add(nodes.get(0));
            while (!queue.isEmpty()) for (Link link : queue.remove().adjoinedLinks) {
                if (seen.add(link.one)) queue.add(link.one); if (seen.add(link.two)) queue.add(link.two);
            }
            check(seen.size() == nodes.size(), "Disconnected authored graph " + index);
            for (int a = 0; a < nodes.size(); a++) for (int b = a+1; b < nodes.size(); b++)
                check(nodes.get(a).position.center.distanceTo(nodes.get(b).position.center) > .145 * GraphicsYio.width,
                        "Overlapping touch targets in map " + index);
            for (Planet node : nodes) {
                check(controller.objectsLayer.editorWorker.isPointInValidPlace(node.position.center), "Out-of-bounds node " + index);
                check(node.isNeutral() || node.unitsInside == 0, "Nonzero starting army " + index);
            }
            if (GameRules.playType == PlayType.capture_flag) {
                Planet flag = controller.objectsLayer.tactics.flagPlanet;
                check(flag != null && flag.isNeutral() && flag.unitsInside == 50, "Neutral flag must have 50 troops " + index);
            }
            if (GameRules.playType == PlayType.home_flags) {
                for (FractionType owner : FractionType.values()) {
                    if (owner == FractionType.neutral || !controller.objectsLayer.tactics.alliances.alive(owner)) continue;
                    Planet flag = controller.objectsLayer.tactics.homeFlags[owner.ordinal()];
                    check(flag != null && flag.fraction == owner, "Missing home flag " + index);
                    for (Planet other : controller.objectsLayer.tactics.homeFlags) if (other != null && other != flag)
                        check(!flag.isLinkedTo(other), "Adjacent starting flags " + index);
                }
            }
            String exported = controller.objectsLayer.exportManager.perform();
            int routeCount = controller.objectsLayer.linksManager.links.size();
            controller.createGameObjects(); controller.importManager.perform(exported); controller.objectsLayer.onEndCreation();
            check(controller.objectsLayer.linksManager.links.size() == routeCount && GameRules.features == level.features,
                    "Authored export/import failed " + index);
        }
        for (int count : counts) check(count == 12, "Every mode needs 12 maps");
        haptics(controller); editor(controller); progression();
        System.out.println("CAMPAIGN DATA PASSED: " + checks + " assertions across 48 maps");
    }
    private static void haptics(GameController controller) {
        controller.gameMode = GameMode.custom; GameRules.aiOnlyMode = false; controller.createGameObjects();
        PlanetsManager manager = controller.objectsLayer.planetsManager;
        Planet node = manager.spawnPlanet(new CircleYio(.4 * GraphicsYio.width,.6 * GraphicsYio.width,.05 * GraphicsYio.width), FractionType.neutral);
        int[] ticks = {0}; SettingsManager.getInstance().hapticsEnabled = true; UiHaptics.setCaptureTick(() -> ticks[0]++);
        node.setFraction(FractionType.green);
        check(ticks[0] == 1 && node.capturePulse == 1, "Human capture lacks feedback");
        UiHaptics.nodeCaptured(); check(ticks[0]==1,"Simultaneous captures generated continuous vibration");
        node.setFraction(FractionType.red);
        check(ticks[0] == 1 && node.capturePulse == 0, "Enemy recapture leaked human feedback");
        UiHaptics.setCaptureTick(() -> ticks[0]++); SettingsManager.getInstance().hapticsEnabled = false;
        node.setFraction(FractionType.green); check(ticks[0] == 1, "Disabled haptics fired");
        UiHaptics.setCaptureTick(null); SettingsManager.getInstance().hapticsEnabled = true;
    }
    private static void editor(GameController controller) {
        controller.gameMode = GameMode.editor; GameRules.features = RuleFeatures.ALL; controller.createGameObjects();
        controller.importManager.perform(CampaignLevels.get(0).code); GameRules.features = RuleFeatures.ALL;
        yio.tro.opacha.game.gameplay.EditorWorker worker = controller.objectsLayer.editorWorker;
        Planet a = controller.objectsLayer.planetsManager.planets.get(0), b = controller.objectsLayer.planetsManager.planets.get(1);
        worker.nextRouteStyle = 1; worker.onLinkAdditionRequested(a,b);
        check(a.getLink(b).directional && a.getLink(b).speedFrom(a) == 2, "Editor cannot make a forward route");
        worker.nextRouteStyle = 2; worker.onLinkAdditionRequested(a,b);
        check(a.getLink(b).speedFrom(a) == .3f, "Editor cannot reverse a route");
        worker.nextRouteStyle = 3; worker.onLinkAdditionRequested(a,b);
        check(a.getLink(b).isBlueprint() && a.getLink(b).builder == FractionType.green, "Editor cannot make a blueprint");
        worker.setPlayType(PlayType.capture_flag); worker.markFlag(b);
        check(controller.objectsLayer.tactics.flagPlanet == b && b.unitsInside == 50, "Editor cannot place a neutral flag");
        GameRules.features = 0;
        check(!controller.objectsLayer.tactics.buildArtillery(a) && !controller.objectsLayer.planetsManager.canBeUpgradedToEconomic(a), "Locked lesson tools were usable");
        GameRules.features = RuleFeatures.ALL;
    }
    private static void progression() {
        com.badlogic.gdx.Preferences prefs=com.badlogic.gdx.Gdx.app.getPreferences("yio.tro.opacha.campaign.v2");
        prefs.putString("progress",""); prefs.flush(); CampaignManager.initialize();
        CampaignManager manager=CampaignManager.getInstance();
        check(manager.getLevelType(0)==yio.tro.opacha.menu.elements.customizable_list.CciType.unlocked
                && manager.getLevelType(1)==yio.tro.opacha.menu.elements.customizable_list.CciType.unknown,"Campaign skips the first concept");
        prefs.putString("progress","0 0 -1 48 nonsense 47"); prefs.flush(); manager.loadValues();
        check(manager.getNumberOfCompletedLevels()==2 && !manager.areAllLevelsCompleted(),"Invalid progress marked the campaign complete");
        check(manager.getNextLevelIndex(47)==1,"Last-map replay did not return to the first unfinished lesson");
        StringBuilder all=new StringBuilder(); for(int i=0;i<48;i++) all.append(i).append(' ');
        prefs.putString("progress",all.toString()); prefs.flush(); manager.loadValues();
        check(manager.areAllLevelsCompleted(),"Completing all 48 maps failed");
        prefs.putString("progress",""); prefs.flush(); CampaignManager.initialize();
    }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
}
