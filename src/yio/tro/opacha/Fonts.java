package yio.tro.opacha;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import yio.tro.opacha.menu.CustomLanguageLoader;
import yio.tro.opacha.menu.LanguagesManager;

public class Fonts {

    public static final String FONT_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789][_!$%#@|\\/?-+=()*&.;:,{}\"´`'<^>∞";
    // йцукенгшщзхъёфывапролджэячсмитьбюЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮіІїЇєЄ

    public static int FONT_SIZE;
    public static BitmapFont buttonFont;
    public static BitmapFont smallFont;
    public static BitmapFont miniFont;
    public static BitmapFont gameFont;
    public static BitmapFont unitFont;
    public static BitmapFont titleFont;


    public static void initFonts() {
        // language should be loaded before fonts are created
        // because of lang_chars
        CustomLanguageLoader.loadLanguage();

        // Keep the original font for scripts Quicksand does not cover.
        String languageCharacters = LanguagesManager.getInstance().getString("lang_characters");
        FileHandle fontFile = Gdx.files.internal(languageCharacters.trim().isEmpty()
                ? "fonts/Quicksand-SemiBold.ttf" : "font.ttf");
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        FONT_SIZE = (int) Math.min(0.029 * Gdx.graphics.getHeight(), 0.059 * Gdx.graphics.getWidth());
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;

        parameter.size = (int) (1.0f * FONT_SIZE);
        parameter.characters = getAllCharacters();
        parameter.flip = false;
        buttonFont = generator.generateFont(parameter);
        buttonFont.setColor(yio.tro.opacha.menu.UiStyle.INK);

        parameter.size = (int) (0.86f * FONT_SIZE);
        parameter.flip = false;
        smallFont = generator.generateFont(parameter);
        smallFont.setColor(yio.tro.opacha.menu.UiStyle.INK);

        parameter.size = (int) (2.25f * FONT_SIZE);
        parameter.flip = false;
        gameFont = generator.generateFont(parameter);
        gameFont.setColor(yio.tro.opacha.menu.UiStyle.INK);
        gameFont.getData().setScale(0.3f);

        parameter.size = (int) (1.4f * FONT_SIZE);
        parameter.flip = false;
        unitFont = generator.generateFont(parameter);
        unitFont.setColor(yio.tro.opacha.menu.UiStyle.INK);
        unitFont.getData().setScale(0.3f);

        parameter.size = (int) (0.76 * FONT_SIZE);
        parameter.flip = false;
        miniFont = generator.generateFont(parameter);
        miniFont.setColor(yio.tro.opacha.menu.UiStyle.INK);

        generator.dispose();
        generator = new FreeTypeFontGenerator(Gdx.files.internal(languageCharacters.trim().isEmpty()
                ? "fonts/Fraunces-Bold.ttf" : "font.ttf"));
        parameter.size = (int) (FONT_SIZE * 1.9f);
        parameter.characters = getAllCharacters();
        titleFont = generator.generateFont(parameter);
        titleFont.setColor(yio.tro.opacha.menu.UiStyle.INK);
        generator.dispose();
        applyTheme();
    }

    public static void applyTheme() {
        if (buttonFont == null) return;
        buttonFont.setColor(yio.tro.opacha.menu.UiStyle.TEXT);
        smallFont.setColor(yio.tro.opacha.menu.UiStyle.TEXT);
        miniFont.setColor(yio.tro.opacha.menu.UiStyle.TEXT);
        titleFont.setColor(yio.tro.opacha.menu.UiStyle.TEXT);
        gameFont.setColor(yio.tro.opacha.menu.UiStyle.INK);
        unitFont.setColor(yio.tro.opacha.menu.UiStyle.INK);
    }


    public static String getAllCharacters() {
        String langChars = LanguagesManager.getInstance().getString("lang_characters");
        return langChars + FONT_CHARACTERS;
    }
}
