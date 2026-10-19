package yio.tro.opacha.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import yio.tro.opacha.SettingsManager;
import yio.tro.opacha.stuff.GraphicsYio;

/** Static edge shapes. A chapter selects the motif; no clock, randomness or gameplay state is consumed. */
public final class ChapterBackground {
    private ChapterBackground() { }

    public static void draw(SpriteBatch batch, float x, float y, float width, float height, int chapter, float alpha) {
        if (!SettingsManager.getInstance().abstractBackgrounds) return;
        UiStyle skin = UiStyle.get();
        int motif = Math.floorMod(chapter, 6);
        float drift = (chapter % 7) * .019f;
        Color tint = motif % 3 == 0 ? UiStyle.CYAN : motif % 3 == 1 ? UiStyle.GREEN : UiStyle.GOLD;
        float opacity = (UiStyle.isNight() ? .055f : .045f) * alpha;
        skin.disc(batch, x + width * (.04f + drift), y + height * .08f, width * .48f, tint, opacity);
        skin.disc(batch, x + width * (1.08f - drift), y + height * .88f, width * .47f, tint, opacity);
        for (int band = 0; band < 3; band++) {
            double previousX = 0, previousY = 0;
            for (int i = 0; i <= 32; i++) {
                double t = i / 32.0;
                double px, py, offset = band * .035 + drift;
                switch (motif) {
                    case 0: px = t; py = .08 + offset + .045 * Math.sin(t * 6.3); break;
                    case 1:
                        px = .90 + (.34 + offset) * Math.cos(t * Math.PI * 1.2);
                        py = .88 + (.20 + offset) * Math.sin(t * Math.PI * 1.2); break;
                    case 2: px = .04 + offset + .04 * Math.sin(t * 9); py = t; break;
                    case 3: px = t; py = .85 + offset + .04 * Math.cos(t * 8); break;
                    case 4:
                        px = .02 + (.36 + offset) * Math.cos(t * Math.PI * 1.4);
                        py = .18 + (.22 + offset) * Math.sin(t * Math.PI * 1.4); break;
                    default: px = .93 - offset + .035 * Math.cos(t * 7); py = t; break;
                }
                double sx = x + px * width, sy = y + py * height;
                if (i > 0) {
                    batch.setColor(tint.r, tint.g, tint.b, alpha * (UiStyle.isNight() ? .13f : .10f));
                    GraphicsYio.drawLine(batch, skin.pixel, previousX, previousY, sx, sy, width * .0035);
                }
                previousX = sx; previousY = sy;
            }
        }
        batch.setColor(Color.WHITE);
    }
}
