package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ai.*;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.loading.*;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.elements.slider.SliderYio;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.RectangleYio;
import java.lang.reflect.Field;
import java.util.HashSet;

/** UI + native simulation checks, with isolated saves and screenshots for visual review. */
public final class AiSmokeTest extends YioGdxGame {
    private final FileHandle output;
    private int frames, lastStageFrame, step;
    private boolean clicking, complete;
    private boolean matchesOnly;
    private boolean rulesOnly;
    private final long deadline = System.nanoTime() + 110_000_000_000L;

    private AiSmokeTest(String directory) { output = new FileHandle(directory); }

    public static void main(String[] args) {
        int width = Integer.parseInt(args[0]), height = Integer.parseInt(args[1]);
        AiSmokeTest game = new AiSmokeTest(args[2]);
        game.matchesOnly = args.length > 3 && args[3].equals("matches");
        game.rulesOnly = args.length > 3 && args[3].equals("rules");
        platformType = PlatformType.pc;
        Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.configuration(width, height);
        config.setTitle("New Opacha AI check");
        config.setInitialVisible(false);
        config.setPreferencesConfig(game.output.child("preferences-" + System.currentTimeMillis()).path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, config);
        require(game.complete, "AI smoke test closed early");
        System.out.println("AI SMOKE PASSED: " + width + " x " + height);
    }

    @Override public void render() {
        super.render();
        if (clicking || gameController == null || splashVisible) return;
        require(System.nanoTime() < deadline, "AI smoke timeout at step " + step);
        require(!Scenes.catchedException.isCurrentlyVisible(), "Game exception screen");
        if (rulesOnly) {
            if (++frames < 2) return;
            AiRegressionChecks.run(gameController);
            AiBehaviorChecks.run(gameController);
            complete = true;
            exitApp();
            return;
        }
        if (matchesOnly) {
            if (++frames < 90) return;
            output.child("ai-matches.txt").writeString(AiMatchChecks.run(gameController), false);
            complete = true;
            exitApp();
            return;
        }
        if (++frames - lastStageFrame < 90) return;
        lastStageFrame = frames;
        switch (step++) {
            case 0: click(Scenes.mainMenu.playButton); break;
            case 1: click(Scenes.chooseGameMode.skirmishButton); break;
            case 2:
                require(Scenes.skirmishMenu.isCurrentlyVisible(), "Setup did not open");
                screenshot("00-initial-setup");
                select("colorsSlider", 2);
                select("difficultySlider", 2);
                select("opponentsSlider", 0);
                break;
            case 3: screenshot("01-setup-mixed"); select("opponentsSlider", 1); break;
            case 4: screenshot("02-setup-simple"); select("opponentsSlider", 2); break;
            case 5: screenshot("03-setup-savvy"); select("opponentsSlider", 3); break;
            case 6: screenshot("04-setup-silly"); select("opponentsSlider", 0); break;
            case 7: click(Scenes.skirmishMenu.startButton); break;
            case 8:
                require(gameController.objectsLayer.aiManager.entities.size() == 3, "Mixed setup did not create three opponents");
                require(new HashSet<>(GameRules.aiTypes.values()).size() == 3, "Mixed opponents lack a type");
                require(Gdx.app.getPreferences("vitamin.soccer_parameters").getInteger("opponents", -1) == 0,
                        "Opponent selection was not saved");
                if (Scenes.quickVelocityTutorial.isCurrentlyVisible()) clickAt(w / 2, h / 2);
                LoadingParameters original = GameRules.initialParameters;
                AiBehaviorChecks.run(gameController);
                if (w == 480) output.child("ai-matches.txt").writeString(AiMatchChecks.run(gameController), false);
                loadingManager.startInstantly(LoadingType.skirmish_create, original);
                break;
            case 9:
                screenshot("05-mixed-match");
                // Exercise the texture restoration path used after Android context loss.
                Texture.invalidateAllTextures(Gdx.app);
                break;
            case 10:
                screenshot("06-mixed-playing");
                for (Planet planet : gameController.objectsLayer.planetsManager.planets) {
                    if (planet.fraction != FractionType.red) continue;
                    planet.setFraction(FractionType.yellow);
                    break;
                }
                break;
            case 11:
                screenshot("07-owner-change");
                LoadingParameters parameters = new LoadingParameters();
                parameters.addParameter("map_size", 0);
                parameters.addParameter("colors", 4);
                parameters.addParameter("ai_only", true);
                parameters.addParameter("difficulty", 2);
                parameters.addParameter("opponents", 0);
                parameters.addParameter("seed", 2718);
                loadingManager.startInstantly(LoadingType.skirmish_create, parameters);
                require(gameController.objectsLayer.aiManager.entities.size() == 4, "AI-only green has no controller");
                break;
            case 12:
                screenshot("08-ai-only");
                Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE);
                Gdx.input.getInputProcessor().keyUp(Input.Keys.ESCAPE);
                break;
            case 13:
                require(gamePaused && Scenes.pauseMenu.isCurrentlyVisible(), "Pause is broken");
                screenshot("09-pause");
                complete = true;
                exitApp();
                break;
            default: break;
        }
    }

    private void select(String fieldName, int value) {
        try {
            Field field = Scenes.skirmishMenu.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            SliderYio slider = (SliderYio) field.get(Scenes.skirmishMenu);
            RectangleYio p = slider.getViewPosition();
            clickAt(Math.round(p.x + p.width * value / slider.numberOfSegments), h - Math.round(p.y));
            System.out.println("Slider " + fieldName + " bounds=" + p + " expected=" + value + " actual=" + slider.getValueIndex());
            require(slider.getValueIndex() == value, "Slider touch missed: " + fieldName);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private void click(InterfaceElement<?> element) {
        RectangleYio p = element.getPosition();
        clickAt(Math.round(p.x + p.width / 2), h - Math.round(p.y + p.height / 2));
    }

    private void clickAt(int x, int y) {
        clicking = true;
        try {
            Gdx.input.getInputProcessor().touchDown(x, y, 0, Input.Buttons.LEFT);
            Gdx.input.getInputProcessor().touchUp(x, y, 0, Input.Buttons.LEFT);
        } finally { clicking = false; }
    }

    private void screenshot(String name) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(output.child(name + ".png"), pixels, -1, true); }
        finally { pixels.dispose(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
