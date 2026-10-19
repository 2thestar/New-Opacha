package yio.tro.opacha.menu.scenes;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.game.campaign.CampaignLevels;
import yio.tro.opacha.game.loading.*;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;

public final class SceneMapDetails extends SceneYio {
    public int levelIndex;
    public ButtonYio playButton, editButton;
    private ButtonYio title, hint, mode;
    @Override protected void initialize() {
        setBackground(GroundIndex.MAIN_MENU);
        spawnBackButton(new Reaction() { @Override protected void reaction() { Scenes.campaign.create(); }});
        title = uiFactory.getButton().setSize(.88,.12).centerHorizontal().alignBottom(.68)
                .setFont(Fonts.titleFont).setBackgroundEnabled(false).setBorder(false).setTouchable(false);
        mode = uiFactory.getButton().setSize(.88,.06).centerHorizontal().alignBottom(.60)
                .setFont(Fonts.smallFont).setBackgroundEnabled(false).setBorder(false).setTouchable(false);
        hint = uiFactory.getButton().setSize(.84,.15).centerHorizontal().alignBottom(.40)
                .setFont(Fonts.smallFont).setBackgroundEnabled(false).setBorder(false).setTouchable(false);
        playButton = uiFactory.getButton().setSize(.76,.075).centerHorizontal().alignBottom(.27).applyText("ui_play")
                .setGroundIndex(GroundIndex.YELLOW).setAnimation(AnimationYio.none).setReaction(load(false));
        editButton = uiFactory.getButton().setSize(.76,.065).centerHorizontal().alignBottom(.17).applyText("ui_edit_map")
                .setAnimation(AnimationYio.none).setReaction(load(true));
    }
    private Reaction load(final boolean editor) {
        return new Reaction() { @Override protected void reaction() {
            LoadingParameters parameters = new LoadingParameters();
            parameters.addParameter("level_code", CampaignLevels.get(levelIndex).code);
            yioGdxGame.loadingManager.startInstantly(editor ? LoadingType.editor_load : LoadingType.editor_play, parameters);
        }};
    }
    @Override protected void onAppear() {
        CampaignLevels.Level level = CampaignLevels.get(levelIndex);
        title.applyManyLines(level.title); mode.applyText("play_" + level.mode); hint.applyManyLines(level.hint);
    }
}
