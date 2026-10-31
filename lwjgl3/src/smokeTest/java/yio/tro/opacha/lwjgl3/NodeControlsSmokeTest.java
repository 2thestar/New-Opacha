package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.touch_modes.TouchMode;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;

/** Regression walkthrough through the production pointer dispatcher, model and renderer. */
public final class NodeControlsSmokeTest extends PlaytestLab {
    private int frames, stage, assertions;
    private boolean complete;
    private Planet source, friendly, enemy, rear;
    private Link route;
    private long heldAt;
    private final long deadline = System.nanoTime() + 85_000_000_000L;

    private NodeControlsSmokeTest(String path) { super(path, 9); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        NodeControlsSmokeTest game = new NodeControlsSmokeTest(args[2]);
        Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.configuration(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        config.setTitle("Opacha node control checks"); config.setInitialVisible(false); config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs").path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, config);
        game.check(game.complete, "Node walkthrough ended early");
        System.out.println("NODE CONTROLS PASSED: " + args[0] + " x " + args[1] + ", " + game.assertions + " assertions");
    }

    @Override public void render() {
        super.render();
        if (gameController == null || splashVisible || complete) return;
        if (System.nanoTime() >= deadline) throw new AssertionError("Timeout at stage " + stage);
        if (Scenes.catchedException.isCurrentlyVisible()) throw new AssertionError("Exception at stage " + stage);
        if (gameView.isInMotion() || ++frames % 18 != 0) return;
        ObjectsLayer l = gameController.objectsLayer;
        switch (stage++) {
            case 0: Scenes.campaignBrief.destroy(); fixture(); break;
            case 1:
                swipe(source, friendly);
                check(source.autoTarget == friendly, "Swipe failed to start continuous sending");
                // Reproduce the reported failure even when no node was selected before the tap.
                l.planetsManager.deselect(); click(source);
                check(source.autoTarget == null, "Tapping an unselected sending node must stop its arrow");
                check(l.planetsManager.getCurrentlySelectedPlanet() == source, "Stopped source should stay available");
                break;
            case 2:
                click(source); check(source.halvedSelectionMode, "Repeated tap did not select half");
                swipe(source, friendly); break;
            case 3:
                capture("01-half-sending");
                l.unitManager.launchUnit(source, friendly, 7); Unit travelling = lastUnit();
                click(source);
                check(source.autoTarget == null && source.halvedSelectionMode, "Stop tap toggled half mode or left arrow running");
                check(travelling.isAlive() && travelling.value == 7, "Stop destroyed troops already travelling");
                int count = l.unitManager.units.size();
                for (int i = 0; i < 240; i++) source.moveActually();
                check(l.unitManager.units.size() == count, "Stopped node kept launching troops");
                break;
            case 4:
                capture("02-stopped");
                click(source); check(!source.halvedSelectionMode, "Next tap did not restore full send");
                friendly.setAutoTarget(enemy); source.setUnitsInside(40); click(friendly);
                check(source.unitsInside == 0 && lastUnit().value == 40 && source.autoTarget == null, "Tap target must send once");
                check(friendly.autoTarget == enemy, "Sending to a friendly node stopped that target's own route");
                check(l.planetsManager.getCurrentlySelectedPlanet() == null, "One-shot selection remained active");
                click(source); click(rear);
                check(l.planetsManager.getCurrentlySelectedPlanet() == rear, "Unlinked friendly tap must switch selection");
                l.planetsManager.deselect(); click(source); source.setUnitsInside(40); click(source); click(friendly);
                check(source.unitsInside == 20 && lastUnit().value == 20 && source.autoTarget == null, "Half tap send incorrect");
                break;
            case 5:
                l.planetsManager.deselect(); swipe(source, friendly);
                check(source.autoTarget == friendly, "Restart failed"); swipe(source, enemy);
                check(source.autoTarget == enemy, "Redirect failed");
                source.setAutoTarget(null); l.planetsManager.deselect();
                route.directional = true;
                check(route.canUse(source, source.fraction) && route.canUse(friendly, friendly.fraction), "Directed speed advantage must retain both travel directions");
                route.constructionCost = 120; route.builder = FractionType.red;
                click(source); click(friendly);
                check(source.unitsInside == 20 && source.autoTarget == null, "Tap used a hostile blueprint");
                swipe(source, friendly); check(source.autoTarget == null, "Swipe used a hostile blueprint");
                route.directional = false; route.constructionCost = 0; route.builder = null; l.planetsManager.deselect();
                down(source); drag(friendly); drag(source); up(source);
                check(source.autoTarget == null, "Return-to-source gesture issued an order");
                down(source); drag(friendly); TouchMode.tmDefault.onReachedTwoFingerState(); up(friendly);
                check(source.autoTarget == null, "Two-finger gesture issued an order");
                down(source); drag(friendly);
                Gdx.input.getInputProcessor().touchCancelled(x(friendly.viewPosition.center), y(friendly.viewPosition.center), 0, Input.Buttons.LEFT);
                check(source.autoTarget == null && gameController.currentTouchCount == 0, "Cancelled pointer issued or stranded an order");
                break;
            case 6:
                fixture(); source.setType(PlanetType.artillery); stage=60; break;
            case 60: click(source); click(source); click(source); stage=7; break;
            case 7:
                l = gameController.objectsLayer;
                check(l.tactics.pending == TacticsManager.Action.artillery, "Third gun tap did not arm");
                capture("03-cannon-armed"); click(source);
                check(l.tactics.pending == TacticsManager.Action.none && !source.halvedSelectionMode, "Fourth gun tap did not cancel to full");
                click(source); click(source); click(enemy);
                check(source.unitsInside == 80 && enemy.unitsInside == 80 && source.autoTarget == null, "Tap shot also sent troops or repeated damage");
                source.artilleryReload = 0; swipe(source, friendly); click(source);
                check(source.autoTarget == null && l.tactics.pending == TacticsManager.Action.none, "Gun stop tap armed cannon");
                click(source); click(source); click(source);
                break;
            case 8:
                clickAt(empty());
                check(l.tactics.pending == TacticsManager.Action.none, "Empty board tap did not cancel targeting");
                source.setAutoTarget(friendly); l.tactics.begin(TacticsManager.Action.artillery, source);
                check(source.autoTarget == null, "Armed cannon kept auto-sending ammunition");
                break;
            case 9:
                click(Scenes.gameOverlay.cancelButton); stage=91; break;
            case 91:
                check(l.tactics.pending == TacticsManager.Action.none, "Cancel control failed");
                click(source); click(source); click(source);
                gameController.onEscapedToPauseMenu();
                check(l.tactics.pending == TacticsManager.Action.none && gameController.currentTouchCount == 0, "Pause did not clear targeting/gesture");
                fixture(); stage=90; break;
            case 90: down(source); heldAt = System.nanoTime(); stage=10; break;
            case 10:
                if (System.nanoTime() - heldAt < 650_000_000L) { stage--; break; }
                up(source);
                check(source.halvedSelectionMode && source.autoTarget == null, "Long press did not select half exactly once");
                break;
            case 11:
                UiStyle.applyTheme(true); swipe(source, friendly); break;
            case 12:
                capture("04-night-sending");
                click(source); check(source.autoTarget == null && source.halvedSelectionMode, "Night/pause stop failed");
                source.setFraction(FractionType.red); click(source);
                check(l.planetsManager.getCurrentlySelectedPlanet() == null && source.autoTarget == null, "Lost node remained controllable");
                UiStyle.applyTheme(false); fixture(); stage=120; break;
            case 120: click(source); stage=13; break;
            case 13:
                capture("05-node-actions"); click(Scenes.selectionOverlay.bEconomic); stage=131; break;
            case 131:
                check(source.isEconomic() && source.unitsInside == 90, "Grow upgrade failed/cost changed");
                stage=14; break;
            case 14:
                swipe(source, friendly); click(source);
                check(source.autoTarget == null, "Factory cannot stop");
                click(source); stage=140; break;
            case 140: click(Scenes.selectionOverlay.bDefensive); stage=141; break;
            case 141:
                check(source.isDefensive() && source.unitsInside == 80, "Tower upgrade failed/cost changed"); stage=15; break;
            case 15:
                swipe(source, friendly); click(source);
                check(source.autoTarget == null, "Tower cannot stop");
                swipe(source, friendly); break;
            case 16:
                check(Scenes.selectionOverlay.stopSendingButton.getFactor().isInAppearState(), "Active route has no Stop control");
                check(!overlap(Scenes.selectionOverlay.halvedModeButton,Scenes.selectionOverlay.stopSendingButton), "Order control touch targets overlap");
                capture("06-tower-sending"); click(Scenes.selectionOverlay.stopSendingButton);
                stage=161; break;
            case 161:
                check(source.autoTarget == null, "Stop sending button failed");
                SettingsManager.getInstance().halvedModeButtonEnabled=false;
                SettingsManager.getInstance().thinBezels=true; swipe(source,friendly); stage=17; break;
            case 17:
                check(!Scenes.selectionOverlay.halvedModeButton.getFactor().isInAppearState(), "Hidden half control reappeared");
                check(Scenes.selectionOverlay.stopSendingButton.getFactor().isInAppearState(), "Hiding half also hid Stop");
                check(Math.abs(Scenes.selectionOverlay.stopSendingButton.getPosition().y - .135*h) < 1, "Thin-bezel offset did not apply to Stop");
                capture("07-stop-without-half"); click(Scenes.selectionOverlay.stopSendingButton);
                stage=171; break;
            case 171:
                check(source.autoTarget == null, "Standalone Stop failed");
                SettingsManager.getInstance().halvedModeButtonEnabled=true;
                SettingsManager.getInstance().thinBezels=false; swipe(source,friendly);
                l.linksManager.removeLink(route);
                check(source.autoTarget == null && !Scenes.selectionOverlay.stopSendingButton.getFactor().isInAppearState(), "Deleted route left a stale Stop control");
                stage=18; break;
            case 18:
                capture("08-route-removed"); click(Scenes.selectionOverlay.actionsButton); break;
            case 19:
                check(Scenes.nodeActions.isCurrentlyVisible() && Scenes.nodeActions.node == source, "Node Actions did not open for source");
                capture("09-actions-open"); click(Scenes.nodeActions.doneButton); break;
            case 20:
                check(!Scenes.nodeActions.isCurrentlyVisible(), "Done did not dismiss node actions");
                complete = true; exitApp(); break;
            default: throw new AssertionError("Unexpected node stage");
        }
    }

    private void fixture() {
        Scenes.selectionOverlay.destroy(); Scenes.nodeActions.destroy(); GameRules.features = RuleFeatures.ALL;
        GameRules.playType = PlayType.conquest; GameRules.aiOnlyMode = false;
        gameController.gameMode = GameMode.skirmish; gameController.createGameObjects(); gameController.initLevelSize(LevelSize.tiny);
        ObjectsLayer l = gameController.objectsLayer;
        source = node(.24,.72,FractionType.green); friendly = node(.76,.72,FractionType.green);
        enemy = node(.76,1.23,FractionType.red); rear = node(.24,1.23,FractionType.green);
        route = l.linksManager.addLink(source, friendly); l.linksManager.addLink(source, enemy); l.linksManager.addLink(friendly, enemy);
        gameController.onEndCreation(); l.aiManager.entities.clear(); gameController.speedManager.setSpeed(0);
        Scenes.speedControls.onSpeedStateChanged(); VelocityManager.getInstance().value = 1;
        setGamePaused(false); gameView.appear(); gameController.createMenuOverlay();
        Scenes.quickVelocityTutorial.destroy(); menuControllerYio.forceDyingElementsToEnd();
    }
    private Planet node(double x,double y,FractionType faction) {
        Planet p = gameController.objectsLayer.planetsManager.spawnPlanet(new CircleYio(x*w,y*w,.05*w),faction);
        p.setUnitsInside(100); p.setCurrentDefense(0); return p;
    }
    private Unit lastUnit() { java.util.ArrayList<Unit> units = gameController.objectsLayer.unitManager.units; return units.get(units.size()-1); }
    private boolean overlap(InterfaceElement a,InterfaceElement b) {
        RectangleYio x=a.getViewPosition(), y=b.getViewPosition();
        double margin=.008*w;
        return x.x < y.x+y.width+margin && x.x+x.width+margin > y.x && x.y < y.y+y.height+margin && x.y+x.height+margin > y.y;
    }
    private PointYio empty() { PointYio p=new PointYio(); p.set(.49*w,.95*w); return p; }
    private int x(PointYio p) { OrthographicCamera c=gameController.cameraController.orthoCam; return Math.round((p.x-c.position.x)/c.zoom+w/2f); }
    private int y(PointYio p) { OrthographicCamera c=gameController.cameraController.orthoCam; return h-Math.round((p.y-c.position.y)/c.zoom+h/2f); }
    private void click(Planet p) { down(p); up(p); }
    private void click(InterfaceElement e) { RectangleYio r=e.getViewPosition(); pointerDown(Math.round(r.x+r.width/2),h-Math.round(r.y+r.height/2)); pointerUp(Math.round(r.x+r.width/2),h-Math.round(r.y+r.height/2)); }
    private void clickAt(PointYio p) { pointerDown(x(p),y(p)); pointerUp(x(p),y(p)); }
    private void down(Planet p) { pointerDown(x(p.viewPosition.center),y(p.viewPosition.center)); }
    private void drag(Planet p) { Gdx.input.getInputProcessor().touchDragged(x(p.viewPosition.center),y(p.viewPosition.center),0); }
    private void up(Planet p) { pointerUp(x(p.viewPosition.center),y(p.viewPosition.center)); }
    private void swipe(Planet a,Planet b) { down(a); drag(b); up(b); }
    private void pointerDown(int x,int y) { Gdx.input.getInputProcessor().touchDown(x,y,0,Input.Buttons.LEFT); }
    private void pointerUp(int x,int y) { Gdx.input.getInputProcessor().touchUp(x,y,0,Input.Buttons.LEFT); }
    private void check(boolean ok,String message) { assertions++; if (!ok) throw new AssertionError(message); }
}
