package yio.tro.opacha.game.view.game_renders;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import java.util.*;

/** Retained pixels allow Android to restore every generated symbol after context loss. */
final class TacticsTextures {
    private final Map<String, TextureRegion> textures = new HashMap<>();
    private final ArrayList<Pixmap> pixels = new ArrayList<>();
    TacticsTextures() {
        for (String name : new String[]{"pixel", "bomb", "flag", "gun", "digger"}) {
            Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            p.setColor(0, 0, 0, 0); p.fill();
            p.setColor(Color.WHITE);
            if (name.equals("pixel")) p.fill();
            if (name.equals("flag")) {
                p.fillRectangle(9, 3, 5, 58);
                p.fillRectangle(14, 5, 43, 29);
                p.setColor(0x343345ff);
                for (int y = 0; y < 3; y++) for (int x = 0; x < 4; x++) if ((x + y) % 2 == 0) p.fillRectangle(16 + 10 * x, 6 + 9 * y, 9, 8);
            }
            if (name.equals("bomb")) {
                p.fillCircle(29, 37, 23);
                p.fillRectangle(27, 7, 6, 12); p.fillRectangle(32, 4, 12, 5);
                p.setColor(0x282936ff); p.fillCircle(29, 37, 18);
                p.setColor(0xf6d087ff); p.fillCircle(47, 6, 4);
            }
            if (name.equals("gun")) {
                p.fillCircle(16, 49, 9); p.fillCircle(44, 49, 9);
                p.fillRectangle(10, 31, 43, 10);
                p.fillTriangle(22, 31, 49, 5, 58, 15);
                p.fillTriangle(22, 31, 58, 15, 35, 39);
            }
            if (name.equals("digger")) {
                for (int i = -2; i <= 2; i++) p.drawLine(14 + i, 55, 49 + i, 12);
                for (int i = -3; i <= 3; i++) p.drawLine(14, 10 + i, 58, 35 + i);
            }
            pixels.add(p);
            Texture texture = new Texture(new PixmapTextureData(p, null, false, false, true));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.put(name, new TextureRegion(texture));
        }
    }
    TextureRegion get(String name) { return textures.get(name); }
    void dispose() {
        for (TextureRegion t : textures.values()) t.getTexture().dispose();
        for (Pixmap p : pixels) p.dispose();
    }
}
