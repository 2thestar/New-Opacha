package yio.tro.opacha.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import yio.tro.opacha.game.gameplay.model.FractionType;
import yio.tro.opacha.stuff.GraphicsYio;

/** Shared, managed geometry for the paper-and-ink interface. No per-frame allocations. */
public final class UiStyle {
    public static final Color PAPER = Color.valueOf("FCF0ED");
    public static final Color BOARD = Color.valueOf("FCFBFA");
    public static final Color INK = Color.valueOf("302A28");
    public static final Color TEXT = Color.valueOf("302A28");
    public static final Color OUTLINE = Color.valueOf("302A28");
    public static final Color NEUTRAL = Color.valueOf("FCFBFA");
    public static final Color WHITE = Color.WHITE;
    public static final Color MUTED = Color.valueOf("716762");
    public static final Color RULE = Color.valueOf("D8CECA");
    public static final Color GOLD = Color.valueOf("FED23F");
    public static final Color GREEN = Color.valueOf("6CD6A0");
    public static final Color RED = Color.valueOf("FF9D91");
    public static final Color CYAN = Color.valueOf("83CDE3");
    private static UiStyle instance;
    private static boolean night;
    private final Pixmap pixels;
    private final Texture texture;
    private final NinePatch shape;
    public final TextureRegion circle, pixel;
    private final float[] triangleVertices = new float[20];

    private UiStyle() {
        // Antialiased rounded rectangle, with a fully opaque centre for the pixel region.
        // Lines combine a rectangle and two caps. Keep all three on one texture,
        // otherwise each segment flushes SpriteBatch twice.
        pixels = new Pixmap(256, 128, Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            double dx = Math.max(Math.abs(x - 31.5) - 16, 0);
            double dy = Math.max(Math.abs(y - 31.5) - 16, 0);
            pixels.drawPixel(x, y, rgba(Math.min(1, Math.max(0, 16 - Math.hypot(dx, dy)))));
        }
        for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
            pixels.drawPixel(68 + x, y, rgba(Math.min(1, Math.max(0, 63.5 - Math.hypot(x - 63.5, y - 63.5)))));
        }
        texture = managed(pixels);
        shape = new NinePatch(new TextureRegion(texture, 0, 0, 64, 64), 16, 16, 16, 16);
        circle = new TextureRegion(texture, 68, 0, 128, 128);
        pixel = new TextureRegion(texture, 31, 31, 1, 1);
    }

    private static int rgba(double alpha) { return 0xffffff00 | (int) (255 * alpha); }

    private static Texture managed(Pixmap pixels) {
        Texture result = new Texture(new PixmapTextureData(pixels, null, false, false, true));
        result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return result;
    }

    public static UiStyle get() {
        if (instance == null) instance = new UiStyle();
        return instance;
    }

    public static boolean isNight() { return night; }

    /** Existing controls retain these Color objects, so theme changes apply without recreating screens. */
    public static void applyTheme(boolean useNight) {
        night = useNight;
        PAPER.set(Color.valueOf(night ? "1D232B" : "FCF0ED"));
        BOARD.set(Color.valueOf(night ? "29323D" : "FCFBFA"));
        TEXT.set(Color.valueOf(night ? "F4EFE8" : "302A28"));
        OUTLINE.set(Color.valueOf(night ? "93A1AF" : "302A28"));
        MUTED.set(Color.valueOf(night ? "A4B1BE" : "716762"));
        RULE.set(Color.valueOf(night ? "566271" : "D8CECA"));
        NEUTRAL.set(Color.valueOf(night ? "E0E5E8" : "FCFBFA"));
        yio.tro.opacha.Fonts.applyTheme();
    }

    public static Color buttonText(int index, boolean hasBackground) {
        return hasBackground && index >= 0 && index <= 4 && index != 1 ? INK : TEXT;
    }

    public void rounded(SpriteBatch batch, float x, float y, float width, float height, Color color, float alpha) {
        batch.setColor(color.r, color.g, color.b, alpha);
        float scale = Math.min(GraphicsYio.width / 390f, Math.min(width, height) / 36f);
        shape.draw(batch, x, y, 0, 0, width / scale, height / scale, scale, scale, 0);
        batch.setColor(Color.WHITE);
    }

    public void disc(SpriteBatch batch, float x, float y, float radius, Color color, float alpha) {
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(circle, x - radius, y - radius, radius * 2, radius * 2);
        batch.setColor(Color.WHITE);
    }

    public void line(SpriteBatch batch, double x1, double y1, double x2, double y2, double width, Color color, float alpha) {
        batch.setColor(color.r, color.g, color.b, alpha);
        GraphicsYio.drawLine(batch, pixel, x1, y1, x2, y2, width);
        disc(batch, (float) x1, (float) y1, (float) width / 2, color, alpha);
        disc(batch, (float) x2, (float) y2, (float) width / 2, color, alpha);
        batch.setColor(Color.WHITE);
    }

    /** Board marks share the node outline's stroke, including paths, arrows and shields. */
    public static float gameStroke() { return .0055f * GraphicsYio.width; }

    public void triangle(SpriteBatch batch, double x, double y, double angle, double radius, Color color, float alpha) {
        double dx = Math.cos(angle), dy = Math.sin(angle);
        float bits = Color.toFloatBits(color.r, color.g, color.b, alpha);
        for (int i = 0; i < 4; i++) {
            double forward = i == 0 ? radius : -.6 * radius;
            double side = i == 1 ? .78 * radius : i == 0 ? 0 : -.78 * radius;
            int v = i * 5;
            triangleVertices[v] = (float) (x + dx * forward - dy * side);
            triangleVertices[v+1] = (float) (y + dy * forward + dx * side);
            triangleVertices[v+2] = bits;
            triangleVertices[v+3] = pixel.getU(); triangleVertices[v+4] = pixel.getV();
        }
        batch.draw(pixel.getTexture(), triangleVertices, 0, triangleVertices.length);
    }

    public void arrow(SpriteBatch batch, double x, double y, double angle, double size, Color color, float alpha) {
        double dx = Math.cos(angle), dy = Math.sin(angle);
        line(batch,x-dx*size-dy*size*.65,y-dy*size+dx*size*.65,x,y,gameStroke(),color,alpha);
        line(batch,x-dx*size+dy*size*.65,y-dy*size-dx*size*.65,x,y,gameStroke(),color,alpha);
    }

    public static Color faction(FractionType fraction) {
        switch (fraction) {
            case green: return GREEN;
            case red: return RED;
            case yellow: return GOLD;
            case cyan: return CYAN;
            default: return NEUTRAL;
        }
    }

    public static Color surface(int index) {
        switch (index) {
            case 2: return GREEN;
            case 3: return GOLD;
            case 4: return RED;
            case 0: return CYAN;
            default: return BOARD;
        }
    }

    public static void dispose() {
        if (instance == null) return;
        instance.texture.dispose();
        instance.pixels.dispose();
        instance = null;
    }
}
