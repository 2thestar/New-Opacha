package yio.tro.opacha.menu.menu_renders;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import yio.tro.opacha.menu.elements.ground.GeParticle;
import yio.tro.opacha.menu.elements.ground.GroundElement;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.stuff.GraphicsYio;

public class RenderGround extends RenderInterfaceElement{

    public static final int QUANTITY = 7;
    TextureRegion groundTextures[];
    private GroundElement groundElement;
    private TextureRegion particleTexture1, particleTexture2;


    @Override
    public void loadTextures() {
        groundTextures = new TextureRegion[QUANTITY];

        for (int i = 0; i < groundTextures.length; i++) {
            groundTextures[i] = GraphicsYio.loadTextureRegion("menu/ground/ground" + i + ".png", false);
        }

        particleTexture1 = GraphicsYio.loadTextureRegion("menu/ground/particle1.png", false);
        particleTexture2 = GraphicsYio.loadTextureRegion("menu/ground/particle2.png", false);
    }


    @Override
    public void renderFirstLayer(InterfaceElement element) {

    }


    @Override
    public void renderSecondLayer(InterfaceElement element) {
        groundElement = (GroundElement) element;

        updateAlpha();
        renderGround();
        yio.tro.opacha.menu.ChapterBackground.draw(batch, groundElement.getViewPosition().x,
                groundElement.getViewPosition().y, w, h,
                yio.tro.opacha.game.campaign.CampaignManager.getInstance().getVisualChapter(), groundElement.getFactor().get());
        renderMainMenuMark();

        GraphicsYio.setBatchAlpha(batch, 1);
    }


    private void renderMainMenuMark() {
        if (groundElement != yio.tro.opacha.menu.scenes.Scenes.mainMenu.getGround()) return;
        yio.tro.opacha.menu.UiStyle skin = yio.tro.opacha.menu.UiStyle.get();
        float alpha = groundElement.getFactor().get();
        float x1 = w * .29f, x2 = w * .51f, x3 = w * .72f;
        float y1 = h * .545f, y2 = h * .50f, y3 = h * .56f, r = w * .048f;
        skin.line(batch, x1, y1, x2, y2, w * .006, yio.tro.opacha.menu.UiStyle.OUTLINE, alpha);
        skin.line(batch, x2, y2, x3, y3, w * .006, yio.tro.opacha.menu.UiStyle.OUTLINE, alpha);
        markNode(skin, x1, y1, r, yio.tro.opacha.menu.UiStyle.GREEN, alpha);
        markNode(skin, x2, y2, r, yio.tro.opacha.menu.UiStyle.GOLD, alpha);
        markNode(skin, x3, y3, r, yio.tro.opacha.menu.UiStyle.CYAN, alpha);
    }

    private void markNode(yio.tro.opacha.menu.UiStyle skin, float x, float y, float r,
                          com.badlogic.gdx.graphics.Color fill, float alpha) {
        skin.disc(batch, x + w * .006f, y - w * .009f, r, yio.tro.opacha.menu.UiStyle.INK, .15f * alpha);
        skin.disc(batch, x, y, r, yio.tro.opacha.menu.UiStyle.INK, alpha);
        skin.disc(batch, x, y + w * .003f, r - w * .005f, fill, alpha);
    }


    private TextureRegion getParticleTexture() {
        if (groundElement.invertedParticlesMode) {
            return particleTexture2;
        }

        return particleTexture1;
    }


    private void renderGround() {
        com.badlogic.gdx.graphics.Color color = yio.tro.opacha.menu.UiStyle.PAPER;
        float alpha = batch.getColor().a;
        batch.setColor(color.r, color.g, color.b, alpha);
        GraphicsYio.drawByRectangle(batch, yio.tro.opacha.menu.UiStyle.get().pixel, groundElement.getViewPosition());
        batch.setColor(1, 1, 1, 1);
    }


    private void updateAlpha() {
        if (groundElement.getFactor().isInAppearState() && groundElement.getFactor().get() < 1) {
            GraphicsYio.setBatchAlpha(batch, Math.sqrt(groundElement.getFactor().get()));
            return;
        }

        GraphicsYio.setBatchAlpha(batch, groundElement.getFactor().get());
    }


    @Override
    public void renderThirdLayer(InterfaceElement element) {

    }
}
