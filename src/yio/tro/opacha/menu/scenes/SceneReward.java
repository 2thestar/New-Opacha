package yio.tro.opacha.menu.scenes;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;

public final class SceneReward extends SceneYio {
    @Override protected void initialize() {
        setBackground(GroundIndex.YELLOW);
        uiFactory.getButton().setSize(.88,.16).centerHorizontal().alignBottom(.61)
                .setFont(Fonts.titleFont).setTouchable(false).setBorder(false).setBackgroundEnabled(false)
                .applyManyLines("ui_campaign_complete");
        uiFactory.getButton().setSize(.76,.075).centerHorizontal().alignBottom(.40).applyText("ui_maps")
                .setGroundIndex(GroundIndex.YELLOW).setAnimation(AnimationYio.none).setReaction(new Reaction() {
                    @Override protected void reaction() { Scenes.campaign.freePlay = true; Scenes.campaign.create(); }
                });
        uiFactory.getButton().setSize(.76,.065).centerHorizontal().alignBottom(.29).applyText("campaign")
                .setAnimation(AnimationYio.none).setReaction(new Reaction() {
                    @Override protected void reaction() { Scenes.campaign.freePlay = false; Scenes.campaign.create(); }
                });
        spawnBackButton(new Reaction() { @Override protected void reaction() { Scenes.chooseGameMode.create(); }});
    }
}
