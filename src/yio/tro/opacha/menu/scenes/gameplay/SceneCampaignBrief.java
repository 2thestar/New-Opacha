package yio.tro.opacha.menu.scenes.gameplay;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.campaign.CampaignLevels;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.scenes.*;

/** One paused lesson when a concept first appears; later hints are requested from Pause. */
public final class SceneCampaignBrief extends ModalSceneYio {
    public ButtonYio startButton, body, title;
    public boolean useHint;
    private int previousSpeed;
    @Override protected void initialize() {
        uiFactory.getButton().setSize(1,1).setBorder(false).setBackgroundEnabled(false)
                .setAnimation(AnimationYio.none).setSilentReactionMode(true).setReaction(new Reaction() {
                    @Override protected void reaction() { }
                });
        createDefaultLabel(.88, .48); defaultLabel.alignBottom(.18).setTouchable(false);
        title = uiFactory.getButton().setParent(defaultLabel).setSize(.80,.06).centerHorizontal().alignTop(.025)
                .setFont(Fonts.buttonFont).setBorder(false).setBackgroundEnabled(false).setTouchable(false).setAnimation(AnimationYio.none);
        body = uiFactory.getButton().setParent(defaultLabel).setSize(.78,.28).centerHorizontal().alignTop(.10)
                .setFont(Fonts.smallFont).setBorder(false).setBackgroundEnabled(false).setTouchable(false).setAnimation(AnimationYio.none);
        startButton = uiFactory.getButton().setParent(defaultLabel).setSize(.60,.06).centerHorizontal().alignBottom(.025)
                .applyText("ui_play").setGroundIndex(GroundIndex.YELLOW).setBackgroundEnabled(true).setBorder(true)
                .setAnimation(AnimationYio.none).tagAsBackButton()
                .setReaction(new Reaction() { @Override protected void reaction() { destroy(); }});
    }
    @Override protected void onAppear() {
        CampaignLevels.Level level = CampaignLevels.get(GameRules.levelIndex);
        title.applyManyLines((level.index + 1) + ". " + (useHint || level.concept.isEmpty() ? level.title : level.concept));
        body.applyManyLines(useHint ? level.hint : level.brief);
        startButton.applyText(useHint ? "ui_resume" : "ui_play");
        previousSpeed = yioGdxGame.gameController.speedManager.getSpeed();
        yioGdxGame.gameController.speedManager.setSpeed(0); Scenes.speedControls.onSpeedStateChanged();
    }
    @Override protected void onDestroy() {
        yioGdxGame.gameController.speedManager.setSpeed(previousSpeed); Scenes.speedControls.onSpeedStateChanged();
    }
}
