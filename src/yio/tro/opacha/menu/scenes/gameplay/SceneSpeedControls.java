package yio.tro.opacha.menu.scenes.gameplay;

import yio.tro.opacha.menu.elements.ButtonYio;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.scenes.ModalSceneYio;

public class SceneSpeedControls extends ModalSceneYio {
    public ButtonYio playPauseButton, fastForwardButton;

    @Override protected void initialize() {
        playPauseButton = uiFactory.getButton().setSize(.24, .052).alignLeft(.03).alignTop(.025)
                .setTouchOffset(.008)
                .setIgnoreResumePause(true).applyText("ui_pause").setReaction(new Reaction() {
                    @Override protected void reaction() { gameController.speedManager.onPlayPauseButtonPressed(); }
                });
        fastForwardButton = uiFactory.getButton().setSize(.21, .052).alignLeft(.29).alignTop(.025)
                .setTouchOffset(.008)
                .setIgnoreResumePause(true).applyText("ui_faster").setReaction(new Reaction() {
                    @Override protected void reaction() { gameController.speedManager.onFastForwardButtonPressed(); }
                });
    }
    @Override protected void onAppear() { super.onAppear(); onSpeedStateChanged(); }
    public void onSpeedStateChanged() {
        if (playPauseButton == null) return;
        int speed = yioGdxGame.gameController.speedManager.getSpeed();
        playPauseButton.applyText(speed == 0 ? "ui_resume" : "ui_pause")
                .setGroundIndex(speed == 0 ? GroundIndex.YELLOW : GroundIndex.BUTTON_WHITE);
        fastForwardButton.applyText(speed > 1 ? "ui_normal_speed" : "ui_faster")
                .setGroundIndex(speed > 1 ? GroundIndex.YELLOW : GroundIndex.BUTTON_WHITE);
    }
}
