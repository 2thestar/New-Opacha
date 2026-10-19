package yio.tro.opacha.game.view.game_renders;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import yio.tro.opacha.game.gameplay.ai.AiType;
import java.util.EnumMap;

/** Small outlined symbols. Generated once, filtered at the current world zoom. */
final class AiMarkerTextures {
    private final EnumMap<AiType, TextureRegion> textures = new EnumMap<>(AiType.class);
    private final EnumMap<AiType, Pixmap> pixels = new EnumMap<>(AiType.class);

    AiMarkerTextures() {
        for (AiType type : AiType.values()) textures.put(type, create(type));
    }

    TextureRegion get(AiType type) {
        return textures.get(type);
    }

    private TextureRegion create(AiType type) {
        Pixmap p = new Pixmap(96, 96, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        int outline = 0x282936ff, fill = 0xf7f1deff;
        p.setColor(outline);
        switch (type) {
            case simple:
                p.fillRectangle(23, 10, 8, 80);
                p.fillTriangle(27, 9, 86, 31, 27, 55);
                p.setColor(fill);
                p.fillRectangle(25, 13, 4, 75);
                p.fillTriangle(32, 16, 78, 31, 32, 48);
                break;
            case savvy:
                crown(p, 0);
                p.setColor(fill);
                crown(p, 5);
                break;
            case silly:
                p.fillRectangle(44, 50, 8, 40);
                p.fillCircle(48, 30, 24);
                p.setColor(fill);
                p.fillRectangle(46, 50, 4, 38);
                p.fillCircle(48, 30, 18);
                break;
        }
        // Keep the small CPU images so Android can restore them after GL context loss.
        pixels.put(type, p);
        Texture texture = new Texture(new PixmapTextureData(p, null, false, false, true));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return new TextureRegion(texture);
    }

    private void crown(Pixmap p, int inset) {
        int bottom = 82 - inset;
        p.fillTriangle(8 + inset, 23 + inset, 21 + inset, bottom, 48, 48 + inset);
        p.fillTriangle(48, 9 + 2 * inset, 21 + inset, bottom, 75 - inset, bottom);
        p.fillTriangle(88 - inset, 23 + inset, 48, 48 + inset, 75 - inset, bottom);
        p.fillRectangle(22 + inset, 71, 52 - 2 * inset, 11 - inset);
    }

    void dispose() {
        for (TextureRegion region : textures.values()) region.getTexture().dispose();
        textures.clear();
        for (Pixmap pixmap : pixels.values()) pixmap.dispose();
        pixels.clear();
    }
}
