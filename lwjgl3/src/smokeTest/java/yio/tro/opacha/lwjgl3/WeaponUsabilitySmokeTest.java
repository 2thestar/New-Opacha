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
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;

/** Real pointer gestures and native screenshots for the revised weapons. */
public final class WeaponUsabilitySmokeTest extends PlaytestLab {
    private int frames, stage;
    private boolean complete;
    private Planet factory, secondFactory, front, gun, enemy;
    private Unit army;
    private MobileBomb bomb;
    private PointYio heldPoint;
    private long holdStarted;
    private final long deadline = System.nanoTime() + 80_000_000_000L;

    private WeaponUsabilitySmokeTest(String path) { super(path, 9); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        WeaponUsabilitySmokeTest game = new WeaponUsabilitySmokeTest(args[2]);
        Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.configuration(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        config.setTitle("Opacha weapon usability checks"); config.setInitialVisible(false); config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs-" + System.currentTimeMillis()).path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, config);
        require(game.complete, "Weapon gesture checks ended early");
        System.out.println("WEAPON GESTURES PASSED: " + args[0] + " x " + args[1]);
    }
    @Override public void render() {
        super.render();
        if (gameController == null || splashVisible || complete) return;
        require(System.nanoTime() < deadline, "Weapon checks timeout at stage " + stage);
        require(!Scenes.catchedException.isCurrentlyVisible(), "Exception at stage " + stage);
        if (gameView.isInMotion()) return;
        if (++frames % 16 != 0) return;
        ObjectsLayer l = gameController.objectsLayer;
        switch (stage++) {
            case 0:
                Scenes.campaignBrief.destroy(); TacticsChecks.run(gameController); fixture(); break;
            case 1: click(gun.viewPosition.center); break;
            case 2:
                require(l.planetsManager.getCurrentlySelectedPlanet() == gun, "First gun tap missed selection");
                require(!gun.halvedSelectionMode && l.tactics.pending == TacticsManager.Action.none, "First gun tap is not full send");
                capture("01-full-send"); click(gun.viewPosition.center); break;
            case 3:
                require(gun.halvedSelectionMode, "Second gun tap is not half send");
                capture("02-half-send"); click(gun.viewPosition.center); break;
            case 4:
                require(l.tactics.pending == TacticsManager.Action.artillery && l.tactics.source == gun, "Third gun tap did not arm cannon");
                capture("03-cannon-ready");
                click(gun.viewPosition.center);
                require(l.tactics.pending == TacticsManager.Action.none && !gun.halvedSelectionMode, "Fourth gun tap did not return to full send");
                click(gun.viewPosition.center); click(gun.viewPosition.center);
                down(gun.viewPosition.center); drag(enemy.viewPosition.center); break;
            case 5:
                require(TouchMode.tmDefault.getDragTarget() == enemy, "Cannon swipe preview rejected a target without an edge");
                capture("04-cannon-swipe"); up(enemy.viewPosition.center);
                require(Math.abs(enemy.unitsInside - 80) < .001 && enemy.currentDefense == 14, "Swipe shot did not bypass shields");
                require(gun.unitsInside == 80 && gun.autoTarget == null, "Cannon swipe also sent troops");
                gun.artilleryReload = 0; click(gun.viewPosition.center); click(gun.viewPosition.center); click(gun.viewPosition.center);
                UiStyle.applyTheme(true);
                break;
            case 6:
                capture("05-night-cannon-ready"); click(enemy.viewPosition.center);
                require(Math.abs(enemy.unitsInside - 64) < .001 && gun.unitsInside == 60, "Tap shot repeated or failed");
                UiStyle.applyTheme(false); l.planetsManager.deselect();
                army = launch(factory, front, 80, .45f); heldPoint = army.touchPosition(); down(heldPoint); holdStarted = System.nanoTime(); break;
            case 7:
                if (System.nanoTime() - holdStarted < 650_000_000L) { stage--; break; }
                require(!army.isAlive() && l.tactics.mobileBombs.size() == 1, "Long press did not convert the factory army");
                bomb = l.tactics.mobileBombs.get(0); up(heldPoint);
                require(bomb.active, "Conversion release detonated bomb");
                l.tactics.mobileBombs.add(new MobileBomb(FractionType.red, front.getLink(enemy), front, .5f));
                break;
            case 8:
                capture("06-secret-edge-bomb");
                Unit own = launch(factory, front, 10, .2f), hostile = launch(front, factory, 20, .8f); hostile.setFraction(FractionType.red);
                click(bomb.position());
                require(factory.getLink(front) == null && !own.isAlive() && !hostile.isAlive(), "Tap did not destroy the complete edge and both armies");
                fixture(); l = gameController.objectsLayer;
                army = launch(factory, front, 70, .45f); heldPoint = army.touchPosition(); down(heldPoint); holdStarted = System.nanoTime(); break;
            case 9:
                if (System.nanoTime() - holdStarted < 650_000_000L) { stage--; break; }
                up(heldPoint); bomb = l.tactics.mobileBombs.get(0);
                bomb.progress.value = .99999f; bomb.move();
                require(bomb.node == front, "Converted bomb failed to arrive"); break;
            case 10:
                capture("07-bomb-at-node"); click(bomb.position());
                require(front.nodeBombs == 1 && !bomb.active, "Tap at own node did not plant mine"); break;
            case 11:
                capture("08-own-mine");
                Unit invading = launch(enemy, front, 400, .5f); front.onUnitReached(invading); invading.setAlive(false);
                require(front.isNeutral() && front.unitsInside == 0 && front.currentDefense == 14, "Mined capture did not wipe invasion"); break;
            case 12:
                capture("09-mine-neutral-shields");
                army = launch(factory, secondFactory, 80, .4f); heldPoint = army.touchPosition(); down(heldPoint);
                require(army.heldForBomb, "Pointer down did not freeze eligible army");
                float before = army.acceleratableFactor.value; army.moveActually();
                require(army.acceleratableFactor.value == before, "Held army moved");
                drag(enemy.viewPosition.center); up(enemy.viewPosition.center);
                require(army.isAlive() && !army.heldForBomb, "Dragging failed to release army");
                down(army.touchPosition()); TouchMode.tmDefault.onReachedTwoFingerState(); up(army.touchPosition());
                require(army.isAlive() && !army.heldForBomb, "Pinch converted or stranded army");
                down(army.touchPosition()); gameController.onEscapedToPauseMenu();
                require(!army.heldForBomb, "Pause stranded held army"); up(army.touchPosition());
                down(army.touchPosition()); Gdx.input.getInputProcessor().touchCancelled(x(army.touchPosition()),y(army.touchPosition()),0,Input.Buttons.LEFT);
                require(!army.heldForBomb && gameController.currentTouchCount==0, "Cancelled pointer stranded army");
                Scenes.editSinglePlanet.create(); Scenes.editSinglePlanet.setSelectedPlanet(factory); break;
            case 13:
                capture("10-editor-bomb-controls"); Scenes.editSinglePlanet.destroy();
                complete = true; exitApp(); break;
            default: throw new AssertionError("Unexpected weapon stage");
        }
    }
    private void fixture() {
        Scenes.selectionOverlay.destroy(); GameRules.features = RuleFeatures.ALL;
        GameRules.playType = PlayType.conquest; GameRules.aiOnlyMode = false;
        gameController.gameMode = GameMode.skirmish; gameController.createGameObjects(); gameController.initLevelSize(LevelSize.tiny);
        ObjectsLayer l = gameController.objectsLayer;
        factory = node(.22,.68,FractionType.green,PlanetType.economic,100);
        front = node(.76,.68,FractionType.green,PlanetType.empty,30);
        gun = node(.22,1.16,FractionType.green,PlanetType.artillery,100);
        enemy = node(.76,1.16,FractionType.red,PlanetType.defensive,100); enemy.setCurrentDefense(14);
        secondFactory = node(.5,1.48,FractionType.green,PlanetType.economic,25);
        l.linksManager.addLink(factory, front); l.linksManager.addLink(front, enemy); l.linksManager.addLink(factory, secondFactory);
        gameController.onEndCreation(); l.aiManager.entities.clear(); gameController.speedManager.setSpeed(0);
        Scenes.speedControls.onSpeedStateChanged(); VelocityManager.getInstance().value = 1;
        setGamePaused(false); gameView.appear(); gameController.createMenuOverlay();
        Scenes.quickVelocityTutorial.destroy(); menuControllerYio.forceDyingElementsToEnd();
    }
    private Planet node(double x,double y,FractionType owner,PlanetType type,int troops) {
        Planet p = gameController.objectsLayer.planetsManager.spawnPlanet(new CircleYio(x*w,y*w,.05*w),owner);
        p.setType(type); p.setUnitsInside(troops); p.setCurrentDefense(0); return p;
    }
    private Unit launch(Planet from,Planet to,int count,float progress) {
        ObjectsLayer l = gameController.objectsLayer; l.unitManager.launchUnit(from,to,count);
        Unit unit = l.unitManager.units.get(l.unitManager.units.size()-1); unit.acceleratableFactor.value=progress; return unit;
    }
    private int x(PointYio p) { OrthographicCamera c=gameController.cameraController.orthoCam; return Math.round((p.x-c.position.x)/c.zoom+w/2f); }
    private int y(PointYio p) { OrthographicCamera c=gameController.cameraController.orthoCam; return h-Math.round((p.y-c.position.y)/c.zoom+h/2f); }
    private void click(PointYio p) { down(p); up(p); }
    private void down(PointYio p) { Gdx.input.getInputProcessor().touchDown(x(p),y(p),0,Input.Buttons.LEFT); }
    private void drag(PointYio p) { Gdx.input.getInputProcessor().touchDragged(x(p),y(p),0); }
    private void up(PointYio p) { Gdx.input.getInputProcessor().touchUp(x(p),y(p),0,Input.Buttons.LEFT); }
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
