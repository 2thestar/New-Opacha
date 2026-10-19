package yio.tro.opacha.menu.scenes.gameplay;

import yio.tro.opacha.menu.elements.VelocitySliderElement;
import yio.tro.opacha.menu.scenes.ModalSceneYio;
import yio.tro.opacha.stuff.GraphicsYio;

public class SceneVelocityControls extends ModalSceneYio{


    public VelocitySliderElement velocitySliderElement;


    @Override
    protected void initialize() {
        velocitySliderElement = uiFactory.getVelocitySliderElement()
                .setParent(followGameViewElement)
                .setSize(0.94, 0.066)
                .alignTop(.09)
                .alignLeft(0.03);
    }
}
