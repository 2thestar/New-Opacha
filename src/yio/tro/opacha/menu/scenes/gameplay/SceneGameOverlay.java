package yio.tro.opacha.menu.scenes.gameplay;

import yio.tro.opacha.game.touch_modes.TouchMode;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.PlayType;
import yio.tro.opacha.game.gameplay.model.TacticsManager;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.elements.gameplay.ViewTouchModeElement;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.elements.ButtonYio;
import yio.tro.opacha.menu.scenes.ModalSceneYio;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.GraphicsYio;

public class SceneGameOverlay extends ModalSceneYio {

    public ButtonYio pauseMenuButton;
    public ViewTouchModeElement viewTouchModeElement;
    public ButtonYio pactsButton, cancelButton;


    public SceneGameOverlay() {
        viewTouchModeElement = null;
    }


    @Override
    protected void beginCreation() {
        menuControllerYio.setCurrentScene(this);
        destroyAllVisibleElements();
        menuControllerYio.checkToRemoveInvisibleElements();
    }


    @Override
    public void initialize() {
        pauseMenuButton = uiFactory.getButton()
                .setSize(.22, .052)
                .alignRight(0.03)
                .alignTop(.025)
                .setTouchOffset(.008)
                .applyText("ui_menu")
                .setReaction(getPauseReaction())
                .setKey("pause_menu")
                .tagAsBackButton();

        viewTouchModeElement = uiFactory.getViewTouchModeElement()
                .setSize(1, 0.06)
                .centerHorizontal()
                .alignTop(.18);
        pactsButton = uiFactory.getButton().setSize(.21, .052).alignLeft(.52).alignTop(.025)
                .setTouchOffset(.008)
                .setGroundIndex(yio.tro.opacha.menu.elements.ground.GroundIndex.BUTTON_WHITE)
                .applyText("pacts").setReaction(new Reaction() { @Override protected void reaction() {
                    Scenes.diplomacy.create();
                }});
        cancelButton = uiFactory.getButton().setSize(.3, .055).centerHorizontal().alignBottom(.06)
                .setGroundIndex(yio.tro.opacha.menu.elements.ground.GroundIndex.BUTTON_WHITE)
                .applyText("cancel").setReaction(new Reaction() { @Override protected void reaction() {
                    yioGdxGame.gameController.objectsLayer.tactics.cancel();
                }});
    }

    @Override protected void appearElement(InterfaceElement element) {
        if (element == pactsButton && (GameRules.playType != PlayType.alliances || GameRules.aiOnlyMode)) return;
        if (element == cancelButton && yioGdxGame.gameController.objectsLayer.tactics.pending == TacticsManager.Action.none) return;
        super.appearElement(element);
    }

    public void setCommandMode(boolean active) {
        if (cancelButton == null) return;
        if (active) super.appearElement(cancelButton);
        else cancelButton.destroy();
    }


    private Reaction getPauseReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                yioGdxGame.setGamePaused(true);
                gameController.onEscapedToPauseMenu();
                menuControllerYio.destroyGameView();

                gameController.createPauseMenu();
            }
        };
    }


    public void onTouchModeSet(TouchMode touchMode) {
        if (viewTouchModeElement != null) {
            viewTouchModeElement.onTouchModeSet(touchMode);
        }
    }


    @Override
    public void onLevelCreationEnd() {
        super.onLevelCreationEnd();
    }
}
