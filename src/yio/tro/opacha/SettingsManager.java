package yio.tro.opacha;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import yio.tro.opacha.menu.scenes.Scenes;

public class SettingsManager {

    private static SettingsManager instance;
    public boolean soundEnabled;
    public boolean fullScreenMode;
    public boolean requestRestartApp;
    public boolean halvedModeButtonEnabled;
    public boolean adaptiveDifficulty;
    public boolean spectateAfterVictory;
    public boolean lockButtonEnabled;
    public boolean velocitySliderEnabled;
    public boolean colorblindMode;
    public boolean thinBezels;
    public int graphicsQuality;
    public boolean nightMode;
    public boolean abstractBackgrounds;
    public boolean hapticsEnabled = true;


    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }

        return instance;
    }


    public static void initialize() {
        instance = null;
    }


    public void saveValues() {
        Preferences prefs = getPreferences();

        prefs.putBoolean("sound", soundEnabled);
        prefs.putBoolean("haptics", hapticsEnabled);
        prefs.putBoolean("night_mode", nightMode);
        prefs.putBoolean("abstract_backgrounds", abstractBackgrounds);
        prefs.putBoolean("full_screen", fullScreenMode);
        prefs.putInteger("graphics_quality", graphicsQuality);
        prefs.putBoolean("halved_mode_button", halvedModeButtonEnabled);
        prefs.putBoolean("adaptive_difficulty", adaptiveDifficulty);
        prefs.putBoolean("spectate_after_victory", spectateAfterVictory);
        prefs.putBoolean("lock_button", lockButtonEnabled);
        prefs.putBoolean("velocity_slider", velocitySliderEnabled);
        if (thinBezels != prefs.getBoolean("thin_bezels", false)) {
            Scenes.selectionOverlay.tagUnInitialized();
        }
        prefs.putBoolean("thin_bezels", thinBezels);
        if (colorblindMode != prefs.getBoolean("colorblind", false)) {
            requestRestartApp = true;
        }
        prefs.putBoolean("colorblind", colorblindMode);

        prefs.flush();
    }


    public void loadValues() {
        Preferences prefs = getPreferences();

        soundEnabled = prefs.getBoolean("sound", true);
        hapticsEnabled = prefs.getBoolean("haptics", true);
        nightMode = prefs.getBoolean("night_mode", false);
        abstractBackgrounds = prefs.getBoolean("abstract_backgrounds", false);
        yio.tro.opacha.menu.UiStyle.applyTheme(nightMode);
        fullScreenMode = prefs.getBoolean("full_screen", true);
        graphicsQuality = prefs.getInteger("graphics_quality", 1);
        halvedModeButtonEnabled = prefs.getBoolean("halved_mode_button", true);
        adaptiveDifficulty = prefs.getBoolean("adaptive_difficulty", true);
        spectateAfterVictory = prefs.getBoolean("spectate_after_victory", false);
        lockButtonEnabled = prefs.getBoolean("lock_button", false);
        velocitySliderEnabled = prefs.getBoolean("velocity_slider", true);
        colorblindMode = prefs.getBoolean("colorblind", false);
        thinBezels = prefs.getBoolean("thin_bezels", false);

        onValuesChanged();
    }


    private Preferences getPreferences() {
        return Gdx.app.getPreferences("vitamin.settings");
    }


    public void onValuesChanged() {

    }

}
