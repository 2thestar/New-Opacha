package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.campaign.CampaignLevels;
import yio.tro.opacha.game.loading.*;
import yio.tro.opacha.game.gameplay.model.HumanStylePlayer;
import yio.tro.opacha.menu.scenes.Scenes;

/** Fast manual testing with isolated saves and the real game, input and renderer. */
public class PlaytestLab extends YioGdxGame {
    protected final FileHandle output;
    protected int level;
    private boolean loaded, botEnabled;
    private HumanStylePlayer bot;
    private int screenshotIndex;

    public PlaytestLab(String path, int level) { output = new FileHandle(path); this.level = level; }

    public static void main(String[] args) {
        platformType = PlatformType.pc;
        int level = Integer.getInteger("labLevel", 1) - 1;
        if (level < 0 || level >= CampaignLevels.COUNT) throw new IllegalArgumentException("Map must be 1..48");
        PlaytestLab game = new PlaytestLab(args[0], level);
        Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.configuration(
                Integer.getInteger("labWidth", 390), Integer.getInteger("labHeight", 844));
        config.setTitle("Opacha playtest"); config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs").path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, config);
    }

    @Override public void render() {
        super.render();
        if (gameController == null || splashVisible) return;
        if (!loaded) { loaded = true; output.mkdirs(); startLevel(level); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) startLevel(level);
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) startLevel((level + 47) % 48);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) startLevel((level + 1) % 48);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) capture("capture-" + (++screenshotIndex) + "-map-" + (level + 1));
        if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) {
            botEnabled = !botEnabled;
            System.out.println(botEnabled ? "Scripted player ON; this attempt is assisted" : "Scripted player OFF");
        }
        if (botEnabled && !gamePaused && gameController.speedManager.getSpeed() > 0
                && !Scenes.campaignBrief.isCurrentlyVisible()) bot.move(gameController.objectsLayer);
    }

    protected void startLevel(int index) {
        level = index; botEnabled = false;
        setGamePaused(true);
        // Closing an old briefing must restore its speed before creating the next one.
        Scenes.campaignBrief.destroy();
        gameView.destroy();
        random.setSeed(Integer.getInteger("labSeedStart", 1) * 109L);
        SettingsManager.getInstance().adaptiveDifficulty = false;
        SettingsManager.getInstance().soundEnabled = false;
        VelocityManager.getInstance().value = 1;
        LoadingParameters parameters = new LoadingParameters(); parameters.addParameter("index", index);
        loadingManager.startInstantly(LoadingType.campaign_create, parameters);
        bot = new HumanStylePlayer(HumanStylePlayer.Style.valueOf(System.getProperty("labStyle", "planner")),
                yio.tro.opacha.game.gameplay.model.FractionType.green, Integer.getInteger("labSeedStart", 1) * 197L);
        if (!Scenes.campaignBrief.isCurrentlyVisible()) {
            gameController.speedManager.setSpeed(0); Scenes.speedControls.onSpeedStateChanged();
        }
        System.out.println("MAP " + (index + 1) + ": " + CampaignLevels.get(index).title
                + " | F5 restart | [ ] maps | F6 screenshot | F8 bot | Space pause");
    }

    protected void capture(String name) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(output.child(name + ".png"), pixels, -1, true); }
        finally { pixels.dispose(); }
        System.out.println("Screenshot: " + output.child(name + ".png").path());
    }
}
