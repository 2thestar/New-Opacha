package yio.tro.opacha.menu.scenes.options;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.SettingsManager;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.scenes.SceneYio;
import yio.tro.opacha.menu.scenes.Scenes;

public final class SceneAppearance extends SceneYio {
    public ButtonYio dayButton, nightButton;
    public CheckButtonYio backgroundsButton;

    @Override protected void initialize() {
        setBackground(GroundIndex.MAIN_MENU);
        spawnBackButton(new Reaction() {
            @Override protected void reaction() { Scenes.settingsMenu.create(); }
        });
        uiFactory.getLabelElement().setSize(.86, .09).centerHorizontal().alignBottom(.76)
                .setFont(Fonts.titleFont).setString(languagesManager.getString("ui_appearance"));
        dayButton = themeButton(.10, "ui_day", false);
        nightButton = themeButton(.55, "ui_night", true);
        backgroundsButton = uiFactory.getCheckButton().setSize(.80, .07).centerHorizontal()
                .alignBottom(.43).setName("ui_abstract_backgrounds").setReaction(new Reaction() {
                    @Override protected void reaction() {
                        SettingsManager settings = SettingsManager.getInstance();
                        settings.abstractBackgrounds = backgroundsButton.isChecked();
                        settings.saveValues();
                    }
                });
    }

    private ButtonYio themeButton(double left, String key, final boolean night) {
        return uiFactory.getButton().setSize(.35, .075).alignLeft(left).alignBottom(.59)
                .applyText(key).setAnimation(AnimationYio.none).setReaction(new Reaction() {
                    @Override protected void reaction() {
                        SettingsManager settings = SettingsManager.getInstance();
                        settings.nightMode = night;
                        UiStyle.applyTheme(night);
                        settings.saveValues();
                        refresh();
                    }
                });
    }

    @Override protected void onAppear() { super.onAppear(); refresh(); }
    private void refresh() {
        SettingsManager settings = SettingsManager.getInstance();
        dayButton.setGroundIndex(settings.nightMode ? GroundIndex.BUTTON_WHITE : GroundIndex.YELLOW);
        nightButton.setGroundIndex(settings.nightMode ? GroundIndex.YELLOW : GroundIndex.BUTTON_WHITE);
        backgroundsButton.setChecked(settings.abstractBackgrounds);
    }
}
