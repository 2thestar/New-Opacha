package yio.tro.opacha.menu.menu_renders;

import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.elements.VelocitySliderElement;
import yio.tro.opacha.stuff.GraphicsYio;
import yio.tro.opacha.stuff.RectangleYio;

public class RenderVelocitySlider extends RenderInterfaceElement {
    @Override public void loadTextures() { }
    @Override public void renderFirstLayer(InterfaceElement element) { }
    @Override public void renderThirdLayer(InterfaceElement element) { }

    @Override public void renderSecondLayer(InterfaceElement element) {
        if (getGameView().appearFactor.get() < .1) return;
        VelocitySliderElement slider = (VelocitySliderElement) element;
        UiStyle skin = UiStyle.get();
        RectangleYio card = slider.getViewPosition(), track = slider.centralLine;
        float alpha = slider.getAlpha(), y = slider.visualFrame.center.y;
        float x = slider.visualFrame.center.x;
        skin.rounded(batch, card.x, card.y, card.width, card.height, UiStyle.PAPER, alpha);
        skin.line(batch, track.x, y, track.x + track.width, y, w * .016, UiStyle.OUTLINE, alpha);
        skin.line(batch, track.x, y, x, y, w * .008, UiStyle.GOLD, alpha);
        for (int i = 0; i < slider.getStepCount(); i++) {
            float stopX = track.x + i * track.width / (slider.getStepCount() - 1);
            skin.disc(batch, stopX, y, w * (i == 6 ? .011f : .007f), UiStyle.OUTLINE, alpha);
            skin.disc(batch, stopX, y, w * .0035f, i <= slider.index ? UiStyle.GOLD : UiStyle.PAPER, alpha);
        }
        float halfWidth = slider.visualFrame.radius + (slider.isDragging() ? w * .004f : 0);
        float height = card.height * (slider.isDragging() ? .87f : .81f);
        float inset = Math.max(1.5f, w * .004f);
        skin.rounded(batch, x - halfWidth, y - height / 2 - h * .003f,
                halfWidth * 2, height, UiStyle.INK, alpha * .16f);
        skin.rounded(batch, x - halfWidth, y - height / 2, halfWidth * 2, height, UiStyle.INK, alpha);
        skin.rounded(batch, x - halfWidth + inset, y - height / 2 + inset,
                halfWidth * 2 - inset * 2, height - inset * 2, UiStyle.GOLD, alpha);
        slider.title.font.setColor(UiStyle.INK);
        GraphicsYio.setFontAlpha(slider.title.font, alpha);
        GraphicsYio.renderText(batch, slider.title);
        slider.title.font.setColor(UiStyle.TEXT);
        GraphicsYio.setBatchAlpha(batch, 1);
    }
}
