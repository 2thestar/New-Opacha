package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.GameMode;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.slider.SliderYio;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;

/** Checks the new entry points, keyboard controls and selected-node touch targets in the shipped JARs. */
public final class UiSmokeTest extends YioGdxGame {
    private final FileHandle output;
    private int frames, lastFrame, step;
    private boolean clicking, complete;
    private Planet home;
    private final long deadline = System.nanoTime() + 100_000_000_000L;
    private UiSmokeTest(String path) { output = new FileHandle(path); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        UiSmokeTest game = new UiSmokeTest(args[2]);
        Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.configuration(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        config.setTitle("Opacha interface check"); config.setInitialVisible(false);
        config.setPreferencesConfig(game.output.child("prefs-" + System.currentTimeMillis()).path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, config);
        require(game.complete, "Interface check ended early");
        System.out.println("INTERFACE PASSED: " + args[0] + " x " + args[1]);
    }
    @Override public void render() {
        super.render();
        if (clicking || gameController == null || splashVisible) return;
        require(System.nanoTime() < deadline, "Interface timeout at " + step);
        require(!Scenes.catchedException.isCurrentlyVisible(), "Exception screen at " + step);
        if (++frames - lastFrame < 80) return;
        lastFrame = frames;
        switch (step++) {
            case 0:
                key(Input.Keys.TAB);
                require(menuControllerYio.keyboardFocus == Scenes.mainMenu.playButton, "Initial focus is not Play");
                step = 22; break;
            case 1:
                require(Scenes.settingsMenu.isCurrentlyVisible(), "Settings entry did not open");
                screenshot("02-settings");
                boolean sound = Scenes.settingsMenu.chkSound.isChecked();
                menuControllerYio.keyboardFocus = Scenes.settingsMenu.chkSound;
                key(Input.Keys.ENTER);
                require(Scenes.settingsMenu.chkSound.isChecked() != sound, "Keyboard did not toggle sound");
                key(Input.Keys.ESCAPE); break;
            case 2:
                require(Scenes.mainMenu.isCurrentlyVisible(), "Settings Back did not return home");
                click(Scenes.mainMenu.tutorialButton); break;
            case 3:
                require(gameController.gameMode == GameMode.campaign && Scenes.campaignBrief.isCurrentlyVisible(), "How to play did not load the first campaign lesson");
                screenshot("03-tutorial"); click(Scenes.campaignBrief.startButton); break;
            case 4:
                key(Input.Keys.ESCAPE); step=24; break;
            case 24:
                require(gamePaused && Scenes.pauseMenu.isCurrentlyVisible(), "Tutorial Back did not open Pause");
                click(Scenes.pauseMenu.mainMenuButton); step = 20; break;
            case 5:
                require(Scenes.skirmishMenu.isCurrentlyVisible(), "Keyboard did not open skirmish");
                SliderYio slider = null;
                for (InterfaceElement e : Scenes.skirmishMenu.getLocalElementsList()) if (e instanceof SliderYio) { slider = (SliderYio) e; break; }
                require(slider != null, "Setup has no keyboard-adjustable slider");
                int value = slider.getValueIndex(); menuControllerYio.keyboardFocus = slider;
                key(Input.Keys.RIGHT);
                require(slider.getValueIndex() == value + 1, "Arrow key did not change map size");
                key(Input.Keys.LEFT);
                require(slider.getValueIndex() == value, "Arrow key did not restore map size");
                step = 23; break;
            case 6:
                require(gameController.gameMode == GameMode.skirmish && !gamePaused, "Match did not start");
                if (Scenes.quickVelocityTutorial.isCurrentlyVisible()) clickAt(w / 2, h / 2);
                break;
            case 7: click(Scenes.speedControls.playPauseButton); break;
            case 8:
                require(gameController.speedManager.getSpeed() == 0 && !gamePaused, "Tactical Pause did not stop the simulation");
                screenshot("05-tactical-pause"); click(Scenes.speedControls.playPauseButton); break;
            case 9:
                require(gameController.speedManager.getSpeed() == 1, "Tactical Resume did not resume");
                gameController.objectsLayer.aiManager.entities.clear();
                home = null;
                for (Planet p : gameController.objectsLayer.planetsManager.planets) if (p.fraction == FractionType.green) home = p;
                require(home != null, "No human node"); home.setUnitsInside(40);
                clickWorld(home.viewPosition.center); break;
            case 10:
                require(Scenes.selectionOverlay.isCurrentlyVisible(), "Node selection did not open actions");
                require(!overlap(Scenes.selectionOverlay.bEconomic, Scenes.selectionOverlay.bDefensive)
                        && !overlap(Scenes.selectionOverlay.bDefensive, Scenes.selectionOverlay.actionsButton), "Node action targets overlap");
                screenshot("06-node-selected"); click(Scenes.selectionOverlay.halvedModeButton); break;
            case 11:
                require(home.halvedSelectionMode, "Send half did not enable half selection");
                screenshot("07-send-half"); click(Scenes.selectionOverlay.bEconomic); break;
            case 12:
                require(home.isEconomic(), "Grow did not upgrade the node");
                Texture.invalidateAllTextures(Gdx.app); break;
            case 13:
                screenshot("08-textures-restored"); click(Scenes.gameOverlay.pauseMenuButton); break;
            case 14:
                require(gamePaused && Scenes.pauseMenu.isCurrentlyVisible(), "Menu did not pause");
                screenshot("09-menu-paused"); key(Input.Keys.ENTER); break;
            case 15:
                require(!gamePaused, "Keyboard Continue did not resume");
                screenshot("10-resumed"); complete = true; exitApp(); break;
            case 20: click(Scenes.mainMenu.playButton); step = 21; break;
            case 21: key(Input.Keys.ENTER); step = 5; break;
            // Capture on a later frame so the focus outline has actually been drawn.
            case 22: screenshot("01-keyboard-focus"); click(Scenes.mainMenu.settingsButton); step = 1; break;
            case 23: screenshot("04-setup-keyboard"); click(Scenes.skirmishMenu.startButton); step = 6; break;
            default: break;
        }
    }
    private boolean overlap(InterfaceElement a, InterfaceElement b) {
        RectangleYio x = a.getViewPosition(), y = b.getViewPosition();
        return x.x < y.x + y.width && x.x + x.width > y.x && x.y < y.y + y.height && x.y + x.height > y.y;
    }
    private void clickWorld(PointYio p) {
        OrthographicCamera c = gameController.cameraController.orthoCam;
        clickAt(Math.round((p.x - c.position.x) / c.zoom + w / 2f), h - Math.round((p.y - c.position.y) / c.zoom + h / 2f));
    }
    private void click(InterfaceElement e) {
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
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
