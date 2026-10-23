package yio.tro.opacha.game.view.game_renders.tm_renders;

import yio.tro.opacha.game.touch_modes.TmDefault;
import yio.tro.opacha.game.touch_modes.TouchMode;
import yio.tro.opacha.game.view.game_renders.GameRender;
import yio.tro.opacha.stuff.CircleYio;
import yio.tro.opacha.stuff.GraphicsYio;
import yio.tro.opacha.stuff.Storage3xTexture;
import yio.tro.opacha.game.gameplay.model.Planet;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.LanguagesManager;
import yio.tro.opacha.Fonts;
import yio.tro.opacha.stuff.RenderableTextYio;
import com.badlogic.gdx.graphics.Color;

public class RenderTmDefault extends GameRender{


    private TmDefault tm;
    CircleYio circleYio;
    private final RenderableTextYio label = new RenderableTextYio();
    private final Color previousColor = new Color();


    public RenderTmDefault() {
        circleYio = new CircleYio();
    }


    @Override
    protected void loadTextures() {
        label.setFont(Fonts.miniFont);
    }


    @Override
    public void render() {
        tm = TouchMode.tmDefault;

        Planet target = tm.getDragTarget(), source = tm.getDragSource();
        if (source == null || target == null) return;
        float w = GraphicsYio.width;
        double angle = source.viewPosition.center.angleTo(target.viewPosition.center);
        double dx = Math.cos(angle), dy = Math.sin(angle);
        double sx = source.viewPosition.center.x + dx * source.viewPosition.radius * 1.3;
        double sy = source.viewPosition.center.y + dy * source.viewPosition.radius * 1.3;
        double tx = target.viewPosition.center.x - dx * target.viewPosition.radius * 1.4;
        double ty = target.viewPosition.center.y - dy * target.viewPosition.radius * 1.4;
        double width = .004 * w * gameController.cameraController.orthoCam.zoom;
        UiStyle style = UiStyle.get();
        style.line(batchMovable, sx, sy, tx, ty, width * 2.5, UiStyle.BOARD, .95f);
        style.line(batchMovable, sx, sy, tx, ty, width, UiStyle.TEXT, 1);
        double size = .018 * w;
        for (int i = 0; i < 2; i++) {
            double x = tx - dx * size * i, y = ty - dy * size * i;
            style.line(batchMovable, x - dx * size - dy * size * .55, y - dy * size + dx * size * .55,
                    x, y, width, UiStyle.TEXT, 1);
            style.line(batchMovable, x - dx * size + dy * size * .55, y - dy * size - dx * size * .55,
                    x, y, width, UiStyle.TEXT, 1);
        }
        float scaleX = label.font.getData().scaleX, scaleY = label.font.getData().scaleY;
        label.font.getData().setScale(gameController.cameraController.orthoCam.zoom);
        label.setString(LanguagesManager.getInstance().getString(source.halvedSelectionMode ? "ui_repeat_half" : "ui_repeat")); label.updateMetrics();
        label.position.set((sx + tx) / 2 - label.width / 2, (sy + ty) / 2 + label.height + .012 * w);
        style.rounded(batchMovable, label.position.x - .012f * w, label.position.y - label.height - .006f * w,
                label.width + .024f * w, label.height + .012f * w, UiStyle.BOARD, .97f);
        previousColor.set(label.font.getColor());
        label.font.setColor(UiStyle.TEXT); GraphicsYio.renderText(batchMovable, label); label.font.setColor(previousColor);
        label.font.getData().setScale(scaleX, scaleY);
    }


    @Override
    protected void disposeTextures() {

    }
}
