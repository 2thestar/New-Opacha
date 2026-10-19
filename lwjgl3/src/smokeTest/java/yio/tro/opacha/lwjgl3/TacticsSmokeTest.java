package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.loading.*;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;

/** Touch-driven feature tour at desktop and Pixel-like portrait aspect ratios. */
public final class TacticsSmokeTest extends YioGdxGame {
    private final FileHandle output;
    private int frames, lastFrame, step;
    private boolean clicking, complete;
    private boolean rulesOnly;
    private boolean victoryOnly;
    private boolean checkedDone;
    private int resultWait;
    private Planet home, routeTarget, enemy;
    private Unit carrier;
    private Link deliveryRoute;
    private Planet deliveryTarget;
    private float pausedReload;
    private final long deadline = System.nanoTime() + 260_000_000_000L;
    private TacticsSmokeTest(String directory) { output = new FileHandle(directory); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        TacticsSmokeTest game = new TacticsSmokeTest(args[2]);
        game.rulesOnly = args.length > 3 && args[3].equals("rules");
        game.victoryOnly = args.length > 3 && args[3].equals("victory");
        Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.configuration(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        config.setTitle("New Opacha tactics checks"); config.setInitialVisible(false);
        config.setPreferencesConfig(game.output.child("prefs-" + System.currentTimeMillis()).path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, config);
        require(game.complete, "Tactics tour ended early");
        System.out.println((game.rulesOnly ? "TACTICS RULES PASSED: " : "TACTICS UI PASSED: ") + args[0] + " x " + args[1]);
    }
    @Override public void render() {
        super.render();
        if (clicking || gameController == null || splashVisible) return;
        require(System.nanoTime() < deadline, "Tactics timeout at " + step);
        require(!Scenes.catchedException.isCurrentlyVisible(), "Exception screen at step " + step);
        if (++frames - lastFrame < 90) return;
        lastFrame = frames;
        if (rulesOnly) {
            TacticsChecks.run(gameController);
            output.child("mode-matches.txt").writeString(TacticsModeMatches.run(gameController), false);
            complete = true; exitApp(); return;
        }
        if (victoryOnly && step == 0) { load(PlayType.home_flags); step = 37; return; }
        System.out.println("Tactics UI step " + step);
        TacticsManager t = gameController.objectsLayer == null ? null : gameController.objectsLayer.tactics;
        switch (step++) {
            case 0: click(Scenes.mainMenu.playButton); break;
            case 1: click(Scenes.chooseGameMode.skirmishButton); break;
            case 2: screenshot("01-conquest-setup"); click(Scenes.skirmishMenu.playTypeButton); break;
            case 3: screenshot("02-neutral-flag-setup"); click(Scenes.skirmishMenu.playTypeButton); break;
            case 4: screenshot("03-home-flags-setup"); click(Scenes.skirmishMenu.playTypeButton); break;
            case 5: screenshot("04-alliances-setup"); click(Scenes.skirmishMenu.playTypeButton); break;
            case 6: click(Scenes.skirmishMenu.playTypeButton); break;
            case 7: click(Scenes.skirmishMenu.startButton); break;
            case 8:
                require(GameRules.playType == PlayType.capture_flag && t.flagPlanet != null && t.flagPlanet.unitsInside == 50, "Neutral flag setup failed");
                if (Scenes.quickVelocityTutorial.isCurrentlyVisible()) clickAt(w / 2, h / 2);
                screenshot("05-neutral-supernode");
                TacticsChecks.run(gameController); load(PlayType.capture_flag); break;
            case 9:
                fixture(); clickWorld(home.viewPosition.center); break;
            case 10: require(Scenes.selectionOverlay.isCurrentlyVisible(), "Node was not selected"); click(Scenes.selectionOverlay.actionsButton); break;
            case 11:
                if (!checkedDone) { click(Scenes.nodeActions.doneButton); checkedDone = true; step = 9; break; }
                screenshot("06-economy-actions"); click(Scenes.nodeActions.buyBombButton); break;
            case 12: require(home.bombStock == 1, "Buy bomb touch failed"); clickWorld(home.viewPosition.center); break;
            case 13: click(Scenes.selectionOverlay.actionsButton); break;
            case 14: screenshot("07-bomb-inventory"); click(Scenes.nodeActions.routeButton); break;
            case 15:
                require(t.pending == TacticsManager.Action.blueprint, "Blueprint command not selected");
                screenshot("07b-route-targeting");
                clickWorld(routeTarget.viewPosition.center); break;
            case 16:
                require(home.getLink(routeTarget) != null && home.getLink(routeTarget).isBlueprint(), "Blueprint target touch failed");
                screenshot("08-route-blueprint"); clickWorld(home.viewPosition.center); break;
            case 17: clickWorld(routeTarget.viewPosition.center); break;
            case 18:
                carrier = null;
                for (Unit unit : gameController.objectsLayer.unitManager.units) if (unit.isAlive() && unit.fraction == FractionType.green) carrier = unit;
                require(carrier != null && carrier.digger, "Troops did not become diggers");
                screenshot("09-diggers"); home.setUnitsInside(150); clickWorld(home.viewPosition.center); break;
            case 19: click(Scenes.selectionOverlay.actionsButton); break;
            case 20: click(Scenes.nodeActions.placeBombButton); break;
            case 21:
                require(t.pending == TacticsManager.Action.bomb, "Deliver bomb command not selected");
                require(new com.badlogic.gdx.graphics.g2d.GlyphLayout(Scenes.notification.notificationElement.font,
                        Scenes.notification.notificationElement.message).width <= .94*w,"Bomb targeting prompt overflowed the viewport");
                screenshot("09b-bomb-targeting"); deliveryRoute=home.adjoinedLinks.get(0);
                clickWorld(new PointYio((deliveryRoute.one.viewPosition.center.x+deliveryRoute.two.viewPosition.center.x)/2,
                        (deliveryRoute.one.viewPosition.center.y+deliveryRoute.two.viewPosition.center.y)/2)); break;
            case 22:
                require(home.bombStock == 0 && t.couriers.size() == 1 && t.bombs.isEmpty(), "Courier touch dispatch failed");
                screenshot("10a-secret-courier");
                float saved=VelocityManager.getInstance().value; VelocityManager.getInstance().value=1;
                for(int i=0;i<1000;i++) t.couriers.get(0).move(); VelocityManager.getInstance().value=saved;
                require(t.bombs.size()==1,"Courier did not plant its route bomb");
                home.bombStock=4; home.setUnitsInside(150); step=50; break;
            case 23: click(Scenes.selectionOverlay.actionsButton); break;
            case 24: click(Scenes.nodeActions.buildGunButton); break;
            case 25: require(home.type == PlanetType.artillery, "Build artillery touch failed"); clickWorld(home.viewPosition.center); break;
            case 26: screenshot("11-artillery-range"); click(Scenes.selectionOverlay.actionsButton); break;
            case 27: screenshot("12-artillery-actions"); click(Scenes.nodeActions.fireButton); break;
            case 28: require(t.pending == TacticsManager.Action.artillery, "Fire command not selected"); screenshot("12b-artillery-targets"); clickWorld(enemy.viewPosition.center); break;
            case 29:
                require(home.artilleryReload > 0, "Fire target touch failed"); screenshot("13-artillery-shot");
                Texture.invalidateAllTextures(Gdx.app); key(Input.Keys.ESCAPE); break;
            case 30: require(gamePaused, "Pause failed"); pausedReload = home.artilleryReload; screenshot("14-paused"); break;
            case 31:
                require(home.artilleryReload == pausedReload, "Reload advanced while paused");
                click(Scenes.pauseMenu.resumeButton); break;
            case 32: require(!gamePaused, "Resume failed"); screenshot("15-restored-textures"); load(PlayType.alliances); break;
            case 33:
                fixture(); t.alliances.offer(FractionType.red, FractionType.green); click(Scenes.gameOverlay.pactsButton); break;
            case 34: screenshot("16-alliance-offer"); click(Scenes.diplomacy.rows.get(FractionType.red)); break;
            case 35:
                require(t.alliances.areAllied(FractionType.green, FractionType.red), "Accept alliance touch failed");
                screenshot("17-alliance-formed"); click(Scenes.diplomacy.rows.get(FractionType.red)); break;
            case 36:
                require(t.alliances.isBroken(FractionType.green, FractionType.red), "Break alliance touch failed");
                screenshot("18-alliance-broken"); Scenes.diplomacy.destroy(); load(PlayType.home_flags); break;
            case 37:
                resultWait = 0;
                screenshot("19-home-flags");
                gameController.objectsLayer.aiManager.entities.clear();
                gameController.objectsLayer.tactics.homeFlags[FractionType.red.ordinal()].setFraction(FractionType.green);
                break;
            case 38:
                require(gameController.objectsLayer.tactics.flagWinner == FractionType.green, "Home flag UI victory failed");
                if (resultWait++ == 0) { step--; break; }
                if (!Scenes.matchResults.isCurrentlyVisible()) { step--; break; }
                if (gameView.appearFactor.get() > 0) { step--; break; }
                if (!resultIsStable()) { step--; break; }
                require(!Scenes.quickVelocityTutorial.isCurrentlyVisible(), "Gameplay tutorial leaked onto the result screen");
                screenshot("20-flag-result"); load(PlayType.alliances); break;
            case 39:
                resultWait = 0;
                gameController.objectsLayer.aiManager.entities.clear();
                for (FractionType a : FractionType.values()) for (FractionType b : FractionType.values()) {
                    if (a != FractionType.neutral && b != FractionType.neutral && a != b) {
                        t.alliances.offer(a, b); t.alliances.offer(b, a);
                    }
                }
                break;
            case 40:
                require(gameController.objectsLayer.fillManager.matchResults != null && gameController.objectsLayer.fillManager.matchResults.coalition.size() == 4,
                        "Coalition result lost a winner");
                if (resultWait++ == 0) { step--; break; }
                if (!Scenes.matchResults.isCurrentlyVisible()) { step--; break; }
                if (gameView.appearFactor.get() > 0) { step--; break; }
                if (!resultIsStable()) { step--; break; }
                screenshot("21-coalition-result"); click(Scenes.matchResults.continueButton); break;
            case 41:
                require(Scenes.chooseGameMode.isCurrentlyVisible(), "Continue did not return to game selection");
                complete = true; exitApp(); break;
            case 42: click(Scenes.selectionOverlay.actionsButton); break;
            case 43: screenshot("10b-node-trap-actions"); click(Scenes.nodeActions.armBombButton); break;
            case 44:
                require(home.nodeBombs==1 && home.bombStock==3,"Trap this node touch failed");
                screenshot("10c-hidden-node-bomb"); clickWorld(home.viewPosition.center); break;
            case 45: click(Scenes.selectionOverlay.actionsButton); break;
            case 46: click(Scenes.nodeActions.placeBombButton); break;
            case 47:
                deliveryTarget=deliveryRoute.getOppositePlanet(home); deliveryTarget.setFraction(FractionType.green);
                clickWorld(deliveryTarget.viewPosition.center); break;
            case 48:
                require(t.reservedBombs(deliveryTarget)==1 && deliveryTarget.nodeBombs==0,"Node delivery touch failed");
                screenshot("10d-node-delivery");
                for(BombCourier courier:t.couriers) if(courier.active) {
                    float speed=VelocityManager.getInstance().value; VelocityManager.getInstance().value=1;
                    for(int i=0;i<1500;i++) courier.move(); VelocityManager.getInstance().value=speed;
                }
                require(deliveryTarget.nodeBombs==1,"Node delivery did not arm the target");
                step=49; break;
            case 49: screenshot("10e-delivered-node-bomb"); clickWorld(home.viewPosition.center); step=23; break;
            case 50: screenshot("10-placed-bomb"); clickWorld(home.viewPosition.center); step=42; break;
            default: break;
        }
    }
    private void load(PlayType type) {
        LoadingParameters p = new LoadingParameters();
        p.addParameter("map_size", 1); p.addParameter("colors", 4); p.addParameter("ai_only", false);
        p.addParameter("difficulty", 2); p.addParameter("opponents", 0); p.addParameter("seed", 2718);
        p.addParameter("play_type", type.toString()); p.addParameter("directional", true);
        loadingManager.startInstantly(LoadingType.skirmish_create, p);
    }
    private void fixture() {
        gameController.objectsLayer.aiManager.entities.clear(); VelocityManager.getInstance().value = .05f;
        home = null; routeTarget = null; enemy = null;
        for (Planet planet : gameController.objectsLayer.planetsManager.planets) {
            planet.setAutoTarget(null);
            if (planet.fraction == FractionType.green) home = planet;
            if (!planet.isNeutral()) planet.setUnitsInside(30);
        }
        require(home != null, "Fixture has no human node"); home.setType(PlanetType.economic); home.setUnitsInside(240);
        double closest = Double.MAX_VALUE;
        for (Planet planet : gameController.objectsLayer.planetsManager.planets) {
            double d = home.position.center.distanceTo(planet.position.center);
            if (planet != home && !home.isLinkedTo(planet) && d < closest) { closest = d; routeTarget = planet; }
            if (!planet.isNeutral() && planet != home && d <= gameController.objectsLayer.tactics.artilleryRange()) enemy = planet;
        }
        require(routeTarget != null && enemy != null, "Fixture lacks route or artillery targets");
    }
    private void clickWorld(PointYio point) {
        OrthographicCamera c = gameController.cameraController.orthoCam;
        clickAt(Math.round((point.x - c.position.x) / c.zoom + w / 2f),
                h - Math.round((point.y - c.position.y) / c.zoom + h / 2f));
    }
    private boolean resultIsStable() {
        InterfaceElement<?> button = Scenes.matchResults.continueButton;
        RectangleYio p = button.getViewPosition();
        return button.getFactor().get() > .99 && Math.abs(p.x - .19 * w) < 2 && Math.abs(p.y - .305 * h) < 2;
    }
    private void click(InterfaceElement<?> e) {
        require(e != null, "Missing control at step " + (step - 1));
        RectangleYio p = e.getViewPosition(); clickAt(Math.round(p.x + p.width / 2), h - Math.round(p.y + p.height / 2));
    }
    private void clickAt(int x, int y) {
        clicking = true;
        try { Gdx.input.getInputProcessor().touchDown(x, y, 0, Input.Buttons.LEFT); Gdx.input.getInputProcessor().touchUp(x, y, 0, Input.Buttons.LEFT); }
        finally { clicking = false; }
    }
    private void key(int key) { Gdx.input.getInputProcessor().keyDown(key); Gdx.input.getInputProcessor().keyUp(key); }
    private void screenshot(String name) {
        Pixmap p = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(output.child(name + ".png"), p, -1, true); } finally { p.dispose(); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
