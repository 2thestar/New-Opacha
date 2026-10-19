package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.campaign.CampaignManager;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.loading.*;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;

/** Real UI, preference round trips, chapter boundaries and GL restoration in both themes. */
public final class ThemeSmokeTest extends YioGdxGame {
    private final FileHandle output;
    private int frame, lastFrame, step;
    private boolean clicking, complete;
    private Planet home;
    private final long deadline = System.nanoTime() + 160_000_000_000L;
    private ThemeSmokeTest(String path) { output = new FileHandle(path); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        ThemeSmokeTest game = new ThemeSmokeTest(args[2]);
        Lwjgl3ApplicationConfiguration c = Lwjgl3Launcher.configuration(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        c.setInitialVisible(false); c.setTitle("Opacha day and night checks");
        c.setPreferencesConfig(game.output.child("prefs-" + System.currentTimeMillis()).path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, c);
        require(game.complete, "Theme checks stopped early");
        System.out.println("THEMES PASSED: " + args[0] + " x " + args[1]);
    }
    @Override public void render() {
        super.render();
        if (clicking || gameController == null || splashVisible) return;
        require(System.nanoTime() < deadline, "Theme timeout at " + step);
        require(!Scenes.catchedException.isCurrentlyVisible(), "Exception screen at theme step " + step);
        if (++frame - lastFrame < 60) return;
        lastFrame = frame;
        SettingsManager settings = SettingsManager.getInstance();
        switch (step++) {
            case 0: click(Scenes.mainMenu.settingsButton); break;
            case 1: click(Scenes.settingsMenu.appearanceButton); break;
            case 2:
                require(Scenes.appearance.isCurrentlyVisible() && !UiStyle.isNight() && !settings.abstractBackgrounds,
                        "Default appearance is not plain Day");
                screenshot("01-day-plain"); click(Scenes.appearance.backgroundsButton); break;
            case 3:
                require(settings.abstractBackgrounds, "Background toggle did not apply");
                screenshot("02-day-abstract"); click(Scenes.appearance.nightButton); break;
            case 4:
                require(settings.nightMode && UiStyle.isNight(), "Night button did not apply theme");
                screenshot("03-night-abstract"); click(Scenes.appearance.backgroundsButton); break;
            case 5:
                require(!settings.abstractBackgrounds, "Background toggle did not turn off");
                screenshot("04-night-plain"); click(Scenes.appearance.backgroundsButton); break;
            case 6:
                settings.nightMode = false; settings.abstractBackgrounds = false; settings.loadValues();
                require(settings.nightMode && settings.abstractBackgrounds && UiStyle.isNight(), "Appearance preferences did not reload");
                Texture.invalidateAllTextures(Gdx.app); break;
            case 7: screenshot("05-restored-night"); key(Input.Keys.ESCAPE); break;
            case 8: require(Scenes.settingsMenu.isCurrentlyVisible(), "Appearance Back did not return to Settings"); screenshot("06-night-settings"); key(Input.Keys.ESCAPE); break;
            case 9: screenshot("07-night-home"); click(Scenes.mainMenu.playButton); break;
            case 10: screenshot("08-night-modes"); click(Scenes.chooseGameMode.skirmishButton); break;
            case 11: screenshot("09-night-setup"); click(Scenes.skirmishMenu.startButton); break;
            case 12:
                if (Scenes.quickVelocityTutorial.isCurrentlyVisible()) clickAt(w / 2, h / 2);
                break;
            case 13:
                screenshot("10-night-match");
                gameController.objectsLayer.aiManager.entities.clear();
                home = null;
                for (Planet p : gameController.objectsLayer.planetsManager.planets) if (p.fraction == FractionType.green) home = p;
                require(home != null, "Night match has no player node");
                home.setType(PlanetType.artillery); home.setUnitsInside(80); home.artilleryReload = 4;
                clickWorld(home.viewPosition.center); break;
            case 14: screenshot("11-night-artillery"); click(Scenes.selectionOverlay.actionsButton); break;
            case 15: screenshot("12-night-actions"); key(Input.Keys.ESCAPE); break;
            case 16: click(Scenes.gameOverlay.pauseMenuButton); break;
            case 17: screenshot("13-night-paused"); load(29); break;
            case 18:
                require(gameController.gameMode == GameMode.campaign && CampaignManager.chapterOf(GameRules.levelIndex) == 0,
                        "Campaign level 29 has the wrong background chapter");
                screenshot("14-chapter-1-night"); settings.nightMode = false; UiStyle.applyTheme(false); settings.saveValues(); break;
            case 19: screenshot("15-chapter-1-day"); load(30); break;
            case 20:
                require(CampaignManager.chapterOf(GameRules.levelIndex) == 1, "Level 30 did not change chapter");
                screenshot("16-chapter-2-day"); load(60); break;
            case 21: screenshot("17-chapter-3-day"); load(90); break;
            case 22: screenshot("18-chapter-4-day"); load(120); break;
            case 23: screenshot("19-chapter-5-day"); load(150); break;
            case 24: screenshot("20-chapter-6-day"); settings.nightMode = true; UiStyle.applyTheme(true); settings.saveValues(); break;
            case 25:
                screenshot("21-chapter-6-night");
                setGamePaused(true); gameView.destroy(); Scenes.campaign.create(); break;
            case 26:
                screenshot("22-night-campaign-list");
                require(CampaignManager.chapterOf(59) == 1 && CampaignManager.chapterOf(60) == 2,
                        "Chapter boundary changed incorrectly");
                LoadingParameters fixture = new LoadingParameters();
                fixture.addParameter("map_size", 1); fixture.addParameter("colors", 4);
                fixture.addParameter("ai_only", false); fixture.addParameter("difficulty", 2);
                fixture.addParameter("opponents", 0); fixture.addParameter("seed", 2718);
                fixture.addParameter("directional", true);
                loadingManager.startInstantly(LoadingType.skirmish_create, fixture); break;
            case 27:
                gameController.objectsLayer.aiManager.entities.clear();
                home = null;
                for (Planet p : gameController.objectsLayer.planetsManager.planets) if (p.fraction == FractionType.green) home = p;
                require(home != null, "Tactical theme fixture has no human node");
                home.setType(PlanetType.artillery); home.setUnitsInside(80);
                TacticsManager tactics = gameController.objectsLayer.tactics;
                Planet enemy = null, routeEnd = null;
                for (Planet p : gameController.objectsLayer.planetsManager.planets) {
                    if (tactics.canFire(home, p)) enemy = p;
                    if (p != home && !home.isLinkedTo(p)) routeEnd = p;
                }
                require(enemy != null && routeEnd != null, "Tactical theme fixture lacks targets");
                require(tactics.createBlueprint(home, routeEnd, FractionType.green) != null, "Theme blueprint was not created");
                tactics.bombs.add(new RouteBomb(home.adjoinedLinks.get(0), FractionType.green, .5f));
                require(tactics.fire(home, enemy), "Theme artillery did not fire");
                gameController.speedManager.onPlayPauseButtonPressed();
                clickWorld(home.viewPosition.center); break;
            case 28:
                screenshot("23-night-tactics");
                MatchResults result = new MatchResults(); result.setWinner(FractionType.green); result.setHumanWon(true);
                result.coalition.add(FractionType.green);
                setGamePaused(true); Scenes.matchResults.create(); Scenes.matchResults.setMatchResults(result); break;
            case 29: screenshot("24-night-result"); click(Scenes.matchResults.continueButton); break;
            case 30:
                require(Scenes.chooseGameMode.isCurrentlyVisible(), "Night result Continue did not return to game modes");
                complete = true; exitApp(); break;
            default: break;
        }
    }
    private void load(int level) {
        LoadingParameters p = new LoadingParameters(); p.addParameter("index", level);
        loadingManager.startInstantly(LoadingType.campaign_create, p);
    }
    private void clickWorld(PointYio p) {
        OrthographicCamera c = gameController.cameraController.orthoCam;
        clickAt(Math.round((p.x - c.position.x) / c.zoom + w / 2f), h - Math.round((p.y - c.position.y) / c.zoom + h / 2f));
    }
    private void click(InterfaceElement e) {
        RectangleYio r = e.getViewPosition(); clickAt(Math.round(r.x + r.width / 2), h - Math.round(r.y + r.height / 2));
    }
    private void clickAt(int x, int y) {
        clicking = true;
        try { Gdx.input.getInputProcessor().touchDown(x, y, 0, Input.Buttons.LEFT); Gdx.input.getInputProcessor().touchUp(x, y, 0, Input.Buttons.LEFT); }
        finally { clicking = false; }
    }
    private void key(int k) { Gdx.input.getInputProcessor().keyDown(k); Gdx.input.getInputProcessor().keyUp(k); }
    private void screenshot(String name) {
        Pixmap p = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(output.child(name + ".png"), p, -1, true); } finally { p.dispose(); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
