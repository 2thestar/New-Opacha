package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.loading.*;
import yio.tro.opacha.menu.*;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.RectangleYio;

/** Real tap/drag, exact stops, second-finger isolation, feedback bounds and saved speed. */
public final class SpeedSliderSmokeTest extends YioGdxGame {
    private final FileHandle output;
    private int frames, lastFrame, step, ticks;
    private boolean dispatching, complete;
    private VelocitySliderElement slider;
    private float dragX, dragY;
    private final long deadline = System.nanoTime() + 85_000_000_000L;
    private SpeedSliderSmokeTest(String path) { output = new FileHandle(path); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        SpeedSliderSmokeTest game = new SpeedSliderSmokeTest(args[2]);
        Lwjgl3ApplicationConfiguration c = Lwjgl3Launcher.configuration(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        c.setInitialVisible(false); c.setTitle("Opacha speed control checks");
        c.setPreferencesConfig(game.output.child("prefs-" + System.currentTimeMillis()).path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, c);
        require(game.complete, "Speed checks stopped early");
        System.out.println("SPEED SLIDER PASSED: " + args[0] + " x " + args[1]);
    }
    @Override public void render() {
        super.render();
        if (dispatching || gameController == null || splashVisible) return;
        require(System.nanoTime() < deadline, "Speed check timeout at " + step);
        require(!Scenes.catchedException.isCurrentlyVisible(), "Exception screen at speed step " + step);
        if (++frames - lastFrame < 55) return;
        lastFrame = frames;
        switch (step++) {
            case 0:
                LoadingParameters p = new LoadingParameters();
                p.addParameter("map_size", 1); p.addParameter("colors", 4);
                p.addParameter("ai_only", false); p.addParameter("difficulty", 2);
                p.addParameter("opponents", 0); p.addParameter("seed", 2718);
                p.addParameter("play_type", PlayType.alliances.toString());
                loadingManager.startInstantly(LoadingType.skirmish_create, p); break;
            case 1:
                if (Scenes.quickVelocityTutorial.isCurrentlyVisible()) tap(w / 2f, h / 2f);
                gameController.objectsLayer.aiManager.entities.clear();
                slider = Scenes.velocityControls.velocitySliderElement;
                UiHaptics.setSpeedTick(() -> ticks++); break;
            case 2:
                require(slider.getViewPosition().width >= .9f * w && slider.getViewPosition().height >= .06f * h,
                        "Speed control lacks a full-width, large touch area");
                require(!overlap(slider, Scenes.gameOverlay.pactsButton)
                        && !overlap(slider, Scenes.speedControls.playPauseButton)
                        && !overlap(slider, Scenes.gameOverlay.viewTouchModeElement), "Speed control overlaps another HUD target");
                screenshot("01-day-speed"); tap(slider.centralLine.x, centerY());
                value(.1f); require(ticks == 1, "A changed speed did not request one tick"); break;
            case 3:
                screenshot("02-minimum"); tap(slider.centralLine.x + slider.centralLine.width, centerY());
                value(4); require(ticks == 2, "Maximum speed did not request one tick");
                tap(slider.centralLine.x + slider.centralLine.width, centerY());
                require(ticks == 2, "Unchanged speed produced haptics"); break;
            case 4:
                screenshot("03-maximum");
                dragX = slider.visualFrame.center.x - .06f * w; dragY = centerY();
                send(0, dragX, dragY, 0); value(4);
                send(0, slider.centralLine.x, dragY, 1);
                send(1, slider.centralLine.x, dragY, 1);
                send(2, slider.centralLine.x, dragY, 1);
                require(slider.isDragging(), "Second finger released the active slider"); value(4);
                dragX = slider.centralLine.x + slider.centralLine.width / 2 - .06f * w;
                send(1, dragX, dragY, 0); value(1); break;
            case 5:
                screenshot("04-dragging"); send(2, dragX, dragY, 0); value(1);
                UiStyle.applyTheme(true); SettingsManager.getInstance().nightMode = true;
                SettingsManager.getInstance().abstractBackgrounds = true; break;
            case 6:
                require(Math.abs(slider.visualFrame.center.x - slider.framePosition.center.x) < 1, "Released handle disagrees with its speed stop");
                screenshot("05-night-speed");
                send(0, slider.centralLine.x, centerY(), 0);
                float[] expected = {.1f,.2f,.3f,.4f,.5f,.75f,1,1.25f,1.5f,2,2.5f,3,4};
                int beforeRapidDrag = ticks;
                for (int i = 0; i < expected.length; i++) {
                    send(1, slider.centralLine.x + i * slider.centralLine.width / 12, centerY(), 0); value(expected[i]);
                }
                require(ticks - beforeRapidDrag <= 1, "Rapid drag produced unbounded haptics");
                dragX = slider.centralLine.x + 7 * slider.centralLine.width / 12; dragY = centerY();
                send(1, dragX, dragY, 0); value(1.25f); break;
            case 7:
                screenshot("06-night-dragging"); send(2, dragX, dragY, 0);
                menuControllerYio.setCurrentScene(Scenes.velocityControls);
                menuControllerYio.keyboardFocus = slider; menuControllerYio.showKeyboardFocus = true;
                key(Input.Keys.RIGHT); value(1.5f); key(Input.Keys.LEFT); value(1.25f); break;
            case 8:
                screenshot("07-keyboard-speed");
                send(0, slider.visualFrame.center.x, centerY(), 0);
                send(3, slider.centralLine.x, centerY(), 0);
                require(!slider.isDragging(), "Cancelled touch retained ownership"); value(1.25f);
                tap(slider.visualFrame.center.x, centerY()); value(1.25f);
                Scenes.velocityControls.destroy();
                require(Math.abs(Gdx.app.getPreferences(VelocityManager.PREFS).getFloat("value") - 1.25f) < .001f,
                        "Speed did not save after dismissing its control");
                Scenes.velocityControls.create(); Texture.invalidateAllTextures(Gdx.app); break;
            case 9:
                value(1.25f); require(!slider.isDragging(), "Slider retained a stale drag after reopening");
                screenshot("08-restored-speed");
                RectangleYio pact = Scenes.gameOverlay.pactsButton.getViewPosition(); tap(pact.x + pact.width / 2, pact.y + pact.height / 2); break;
            case 10:
                require(Scenes.diplomacy.isCurrentlyVisible(), "Pacts control is not accessible beside the speed HUD");
                screenshot("09-night-pacts"); UiHaptics.setSpeedTick(null); complete = true; exitApp(); break;
            default: break;
        }
    }
    private float centerY() { RectangleYio r = slider.getViewPosition(); return r.y + r.height / 2; }
    private void value(float expected) { require(Math.abs(VelocityManager.getInstance().value - expected) < .001f, "Wrong speed: expected " + expected + ", got " + VelocityManager.getInstance().value); }
    private void tap(float x, float y) { send(0,x,y,0); send(2,x,y,0); }
    private void send(int type, float x, float y, int pointer) {
        dispatching = true;
        try {
            InputProcessor input = Gdx.input.getInputProcessor();
            if (type == 0) input.touchDown(Math.round(x), h - Math.round(y), pointer, Input.Buttons.LEFT);
            if (type == 1) input.touchDragged(Math.round(x), h - Math.round(y), pointer);
            if (type == 2) input.touchUp(Math.round(x), h - Math.round(y), pointer, Input.Buttons.LEFT);
            if (type == 3) input.touchCancelled(Math.round(x), h - Math.round(y), pointer, Input.Buttons.LEFT);
        } finally { dispatching = false; }
    }
    private void key(int key) { Gdx.input.getInputProcessor().keyDown(key); Gdx.input.getInputProcessor().keyUp(key); }
    private boolean overlap(InterfaceElement a, InterfaceElement b) {
        RectangleYio x = a.getViewPosition(), y = b.getViewPosition();
        return x.x < y.x + y.width && x.x + x.width > y.x && x.y < y.y + y.height && x.y + x.height > y.y;
    }
    private void screenshot(String name) {
        Pixmap p = Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(output.child(name + ".png"), p, -1, true); } finally { p.dispose(); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
