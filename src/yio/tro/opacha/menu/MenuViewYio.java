package yio.tro.opacha.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import yio.tro.opacha.*;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.elements.ScrollableAreaYio;
import yio.tro.opacha.menu.menu_renders.MenuRenders;
import yio.tro.opacha.stuff.GraphicsYio;

public class MenuViewYio {

    public YioGdxGame yioGdxGame;
    MenuControllerYio menuControllerYio;
    public SpriteBatch batch;
    public ShapeRenderer shapeRenderer;
    public OrthographicCamera orthoCam;
    public int w, h;
    private final Rectangle scrollViewport = new Rectangle(), scrollScissors = new Rectangle();


    public MenuViewYio(YioGdxGame yioGdxGame) {
        this.yioGdxGame = yioGdxGame;
        menuControllerYio = yioGdxGame.menuControllerYio;
        shapeRenderer = new ShapeRenderer();
        w = Gdx.graphics.getWidth();
        h = Gdx.graphics.getHeight();
        createOrthoCam();
        update();
    }


    private void createOrthoCam() {
        orthoCam = new OrthographicCamera(yioGdxGame.w, yioGdxGame.h);
        orthoCam.position.set(orthoCam.viewportWidth / 2f, orthoCam.viewportHeight / 2f, 0);
        orthoCam.update();
    }


    public void update() {
        batch = yioGdxGame.batch;
        MenuRenders.updateRenderSystems(this);
    }


    public void renderAll(boolean onTopOfGameView) {
        batch.begin();

        renderLayers(onTopOfGameView, true);
        renderLayers(onTopOfGameView, false);

        renderDebug();

        GraphicsYio.setBatchAlpha(batch, 1);
        batch.end();
    }


    private void renderLayers(boolean onTopOfGameView, boolean dyingStatus) {
        for (int layer = 0; layer < 3; layer++) for (InterfaceElement element : menuControllerYio.visibleElements) {
            if (!element.isVisible()) continue;
            if (!element.compareGvStatus(onTopOfGameView)) continue;
            if (element.getDyingStatus() != dyingStatus) continue;
            if (element.isViewPositionNotUpdatedYet()) continue;
            ScrollableAreaYio scroll = scrollParent(element);
            if (scroll != null) {
                scroll.copyViewportTo(scrollViewport);
                ScissorStack.calculateScissors(orthoCam, batch.getTransformMatrix(), scrollViewport, scrollScissors);
                batch.flush();
                if (!ScissorStack.pushScissors(scrollScissors)) continue;
            }
            try {
                if (layer == 0) element.getRenderSystem().renderFirstLayer(element);
                else if (layer == 1) element.getRenderSystem().renderSecondLayer(element);
                else {
                    element.getRenderSystem().renderThirdLayer(element);
                    MenuKeyboard.renderFocus(this, element);
                }
            } finally {
                if (scroll != null) { batch.flush(); ScissorStack.popScissors(); }
            }
        }
    }

    private ScrollableAreaYio scrollParent(InterfaceElement element) {
        for (InterfaceElement parent = element.getParent(); parent != null; parent = parent.getParent())
            if (parent instanceof ScrollableAreaYio) return (ScrollableAreaYio) parent;
        return null;
    }


    private void renderDebug() {

    }
}
