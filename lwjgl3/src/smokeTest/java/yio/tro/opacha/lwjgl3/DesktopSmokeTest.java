package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import yio.tro.opacha.PlatformType;
import yio.tro.opacha.SoundManager;
import yio.tro.opacha.SoundThreadYio;
import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.GameController;
import yio.tro.opacha.game.GameMode;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.RectangleYio;

/** Native integration check. Requires a display and OpenGL, like the game itself. */
public final class DesktopSmokeTest extends YioGdxGame {
    private final FileHandle output;
    private int frames;
    private int stage;
    private int lastStageFrame;
    private int gameMoveIndex;
    private boolean clicking;
    private boolean complete;
    private final long deadline = System.nanoTime() + 60_000_000_000L;

    private DesktopSmokeTest(String directory) {
        output = new FileHandle(directory);
    }

    public static void main(String[] args) {
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        DesktopSmokeTest game = new DesktopSmokeTest(args[2]);
        YioGdxGame.platformType = PlatformType.pc;
        Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.configuration(width, height);
        config.setTitle("Opacha build check - " + width + " x " + height);
        config.setInitialVisible(false);
        config.setPreferencesConfig(game.output.child("preferences-" + System.currentTimeMillis()).path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, config);
        require(game.complete, "Game closed before the smoke test completed");
        System.out.println("SMOKE PASSED: " + width + " x " + height + " (screenshots: " + game.output.path() + ")");
    }

    @Override
    public void render() {
        super.render();
        // Some upstream controls render immediately inside their click handler.
        if (clicking) return;
        require(System.nanoTime() < deadline, "Smoke test timed out at stage " + stage);
        frames++;
        if (gameController == null || splashVisible) return;
        require(!Scenes.catchedException.isCurrentlyVisible(), "The game displayed its exception screen");
        if (frames - lastStageFrame < 90) return;
        lastStageFrame = frames;

        switch (stage++) {
            case 0:
                require(Scenes.mainMenu.isCurrentlyVisible(), "Main menu did not load");
                screenshot("01-main-menu");
                click(Scenes.mainMenu.playButton);
                break;
            case 1:
                require(Scenes.chooseGameMode.isCurrentlyVisible(), "Play button did not open game modes");
                screenshot("02-game-modes");
                click(Scenes.chooseGameMode.skirmishButton);
                break;
            case 2:
                require(Scenes.skirmishMenu.isCurrentlyVisible(), "Skirmish button did not open setup");
                screenshot("03-skirmish-setup");
                click(Scenes.skirmishMenu.startButton);
                break;
            case 3:
                require(gameController.gameMode == GameMode.skirmish, "Start button did not create a skirmish");
                require(!gamePaused, "Created match is paused");
                require(gameController.objectsLayer.planetsManager.planets.size() > 1, "Map has no planets");
                gameMoveIndex = GameController.currentMoveIndex;
                if (Scenes.quickVelocityTutorial.isCurrentlyVisible()) {
                    screenshot("04-speed-tutorial");
                    clickAt(w / 2, h / 2);
                }
                break;
            case 4:
                require(GameController.currentMoveIndex > gameMoveIndex, "Game simulation did not advance");
                require(!Scenes.quickVelocityTutorial.isCurrentlyVisible(), "Click did not dismiss the speed tutorial");
                screenshot("05-match");
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                Gdx.input.getInputProcessor().keyUp(Input.Keys.ESCAPE);
                break;
            case 5:
                require(gamePaused, "Escape did not pause the match");
                require(Scenes.pauseMenu.isCurrentlyVisible(), "Pause menu did not open");
                screenshot("06-pause-menu");
                complete = true;
                exitApp();
                break;
            default:
                break;
        }
    }

    private void click(InterfaceElement<?> element) {
        RectangleYio bounds = element.getPosition();
        int x = Math.round(bounds.x + bounds.width / 2);
        int y = h - Math.round(bounds.y + bounds.height / 2);
        clickAt(x, y);
    }

    private void clickAt(int x, int y) {
        clicking = true;
        try {
            Gdx.input.getInputProcessor().touchDown(x, y, 0, Input.Buttons.LEFT);
            Gdx.input.getInputProcessor().touchUp(x, y, 0, Input.Buttons.LEFT);
        } finally {
            clicking = false;
        }
    }

    @Override
    public void dispose() {
        SoundThreadYio soundThread = SoundManager.soundThreadYio;
        super.dispose();
        require(soundThread == null || !soundThread.isAlive(), "Sound thread did not stop when the game closed");
    }

    private void screenshot(String name) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            PixmapIO.writePNG(output.child(name + ".png"), pixels, -1, true);
        } finally {
            pixels.dispose();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
