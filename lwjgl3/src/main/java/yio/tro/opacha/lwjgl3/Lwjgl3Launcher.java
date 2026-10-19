package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import yio.tro.opacha.PlatformType;
import yio.tro.opacha.YioGdxGame;

/** Desktop entry point for the original portrait game. */
public final class Lwjgl3Launcher {
    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        YioGdxGame.platformType = PlatformType.pc;
        new Lwjgl3Application(new YioGdxGame(), configuration(480, 800));
    }

    static Lwjgl3ApplicationConfiguration configuration(int width, int height) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("New Opacha");
        config.setWindowedMode(width, height);
        // The upstream game calculates its layout and framebuffers once at startup.
        config.setResizable(false);
        config.setForegroundFPS(60);
        config.useVsync(true);
        config.setPreferencesConfig(".opacha-upgraded", Files.FileType.External);
        return config;
    }
}
