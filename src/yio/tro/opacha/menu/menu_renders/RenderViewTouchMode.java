package yio.tro.opacha.menu.menu_renders;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.elements.gameplay.ViewTouchModeElement;
import yio.tro.opacha.stuff.GraphicsYio;
import yio.tro.opacha.stuff.RenderableTextYio;

public class RenderViewTouchMode extends RenderInterfaceElement{


    private ViewTouchModeElement viewTouchModeElement;
    private BitmapFont font;
    private TextureRegion whitePixel;
    private RenderableTextYio title;


    @Override
    public void loadTextures() {
        whitePixel = GraphicsYio.loadTextureRegion("pixels/white.png", false);
    }


    @Override
    public void renderFirstLayer(InterfaceElement element) {

    }


    @Override
    public void renderSecondLayer(InterfaceElement element) {
        viewTouchModeElement = (ViewTouchModeElement) element;
        title = viewTouchModeElement.title;
        font = title.font;

        if (!viewTouchModeElement.hasText) return;

        yio.tro.opacha.stuff.RectangleYio r = viewTouchModeElement.backgroundPosition;
        yio.tro.opacha.menu.UiStyle.get().rounded(batch, r.x, r.y, r.width, r.height,
                yio.tro.opacha.menu.UiStyle.BOARD, .94f);
        GraphicsYio.setBatchAlpha(batch, 1);

        GraphicsYio.setFontAlpha(font, viewTouchModeElement.getTextAlpha());
        GraphicsYio.renderText(batch, title);
        GraphicsYio.setFontAlpha(font, 1);
    }


    @Override
    public void renderThirdLayer(InterfaceElement element) {

    }
}
