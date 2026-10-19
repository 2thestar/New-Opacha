package yio.tro.opacha.menu.menu_renders;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import yio.tro.opacha.Fonts;
import yio.tro.opacha.menu.elements.CheckButtonYio;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.stuff.GraphicsYio;
import yio.tro.opacha.stuff.RenderableTextYio;

public class RenderCheckButton extends RenderInterfaceElement {

    TextureRegion activeTexture;
    private BitmapFont font;
    private CheckButtonYio checkButton;
    private float f;


    @Override
    public void loadTextures() {
        activeTexture = GraphicsYio.loadTextureRegion("menu/check_button/chk_active.png", true);
    }


    @Override
    public void renderFirstLayer(InterfaceElement element) {

    }


    @Override
    public void renderSecondLayer(InterfaceElement element) {
        checkButton = (CheckButtonYio) element;
        font = Fonts.smallFont;
        f = checkButton.getAlpha();

        if (f < 0.4) return;

        renderSelection();
        renderActiveSquare();
        renderText();

        GraphicsYio.setBatchAlpha(batch, 1);
    }


    private void renderText() {
        if (f < RenderableTextYio.OPTIMIZATION_CUT_OUT) {
            GraphicsYio.setBatchAlpha(batch, 0.15 * f);
            GraphicsYio.drawByRectangle(batch, blackPixel, checkButton.renderableText.bounds);
            GraphicsYio.setBatchAlpha(batch, 1);
            return;
        }

        if (f < 1) {
            GraphicsYio.setFontAlpha(font, f);
            GraphicsYio.renderText(batch, checkButton.renderableText);
            GraphicsYio.setFontAlpha(font, 1);
            return;
        }

        GraphicsYio.renderText(batch, checkButton.renderableText);
    }


    private void renderActiveSquare() {
        yio.tro.opacha.stuff.RectangleYio r = checkButton.getActiveSquare();
        yio.tro.opacha.menu.UiStyle skin = yio.tro.opacha.menu.UiStyle.get();
        float border = Math.max(1.5f, w * .004f);
        skin.rounded(batch, r.x, r.y, r.width, r.height, yio.tro.opacha.menu.UiStyle.OUTLINE, f);
        skin.rounded(batch, r.x + border, r.y + border, r.width - 2 * border, r.height - 2 * border,
                checkButton.isChecked() ? yio.tro.opacha.menu.UiStyle.GOLD : yio.tro.opacha.menu.UiStyle.BOARD, f);
        if (checkButton.isChecked()) {
            skin.line(batch, r.x + .23f * r.width, r.y + .50f * r.height,
                    r.x + .43f * r.width, r.y + .29f * r.height, border, yio.tro.opacha.menu.UiStyle.INK, f);
            skin.line(batch, r.x + .43f * r.width, r.y + .29f * r.height,
                    r.x + .77f * r.width, r.y + .73f * r.height, border, yio.tro.opacha.menu.UiStyle.INK, f);
        }
    }


    private void renderSelection() {
        if (checkButton.selectionFactor.get() > 0) {
            GraphicsYio.setBatchAlpha(batch, 0.2 * checkButton.selectionFactor.get());
            GraphicsYio.drawByRectangle(batch, blackPixel, checkButton.getViewPosition());
            GraphicsYio.setBatchAlpha(batch, 1);
        }
    }


    @Override
    public void renderThirdLayer(InterfaceElement element) {

    }
}
