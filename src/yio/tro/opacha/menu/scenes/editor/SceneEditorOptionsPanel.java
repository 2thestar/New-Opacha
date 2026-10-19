package yio.tro.opacha.menu.scenes.editor;

import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.EditorWorker;
import yio.tro.opacha.menu.LanguagesManager;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.slider.*;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.scenes.*;

public final class SceneEditorOptionsPanel extends ModalSceneYio {
    public ButtonYio modeButton, routeButton, toolsButton, exportButton, randomButton;
    private SliderYio difficulty;
    private CheckButtonYio aiOnly;
    private LightBottomPanelElement panel;
    private long generateArmedUntil;
    @Override protected void initialize() {
        createCloseButton();
        panel = uiFactory.getLightBottomPanelElement().setSize(1,.76).setTitle("settings").setAnimation(AnimationYio.down);
        difficulty = uiFactory.getSlider().setParent(panel).setSize(.72).alignTop(.08).centerHorizontal()
                .setName("difficulty").setValues(0,3).setSegmentsVisible(true).setAnimation(AnimationYio.none)
                .setBehavior(new SliderBehavior() {
                    @Override public String getValueString(LanguagesManager language, SliderYio slider) {
                        return language.getString(Difficulty.values()[slider.getValueIndex()].toString());
                    }
                });
        aiOnly = uiFactory.getCheckButton().setParent(panel).setSize(.80,.06).centerHorizontal().alignBottom(.50).setName("ai_only");
        modeButton = row(.405, new Reaction() { @Override protected void reaction() {
            EditorWorker worker = gameController.objectsLayer.editorWorker;
            worker.setPlayType(PlayType.values()[(GameRules.playType.ordinal()+1)%PlayType.values().length]); refresh();
        }});
        routeButton = row(.325, new Reaction() { @Override protected void reaction() {
            EditorWorker worker = gameController.objectsLayer.editorWorker;
            worker.nextRouteStyle = (worker.nextRouteStyle + 1) % 4; refresh();
        }});
        toolsButton = row(.245, new Reaction() { @Override protected void reaction() { GameRules.features = RuleFeatures.ALL; refresh(); }});
        exportButton = uiFactory.getButton().setParent(panel).setSize(.37,.06).alignLeft(.10).alignBottom(.095)
                .applyText("export").setAnimation(AnimationYio.none).setReaction(new Reaction() { @Override protected void reaction() {
                    apply(); gameController.objectsLayer.exportManager.performToClipboard(); Scenes.notification.show("exported"); destroy();
                }});
        randomButton = uiFactory.getButton().setParent(panel).setSize(.37,.06).alignLeft(.53).alignBottom(.095)
                .applyText("ui_generate").setAnimation(AnimationYio.none).setReaction(new Reaction() { @Override protected void reaction() {
                    if (System.currentTimeMillis() > generateArmedUntil) {
                        generateArmedUntil = System.currentTimeMillis() + 4000; Scenes.notification.show("tap_again_to_confirm"); return;
                    }
                    apply(); gameController.objectsLayer.editorWorker.onRandomGenerationRequested(); destroy();
                }});
    }
    private ButtonYio row(double bottom, Reaction reaction) {
        return uiFactory.getButton().setParent(panel).setSize(.82,.06).centerHorizontal().alignBottom(bottom)
                .setAnimation(AnimationYio.none).setReaction(reaction);
    }
    @Override protected void onAppear() {
        yioGdxGame.gameController.resetTouchMode(); generateArmedUntil = 0;
        difficulty.setValueIndex(GameRules.difficulty.ordinal()); aiOnly.setChecked(GameRules.aiOnlyMode); refresh();
    }
    private void refresh() {
        modeButton.applyText("play_" + GameRules.playType);
        routeButton.applyText("editor_route_" + yioGdxGame.gameController.objectsLayer.editorWorker.nextRouteStyle);
        toolsButton.applyText(GameRules.features == RuleFeatures.ALL ? "editor_all_tools" : "editor_unlock_tools");
    }
    private void apply() { GameRules.difficulty = Difficulty.values()[difficulty.getValueIndex()]; GameRules.aiOnlyMode = aiOnly.isChecked(); }
    @Override protected void onDestroy() { apply(); }
}
