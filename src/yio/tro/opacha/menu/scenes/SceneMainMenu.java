package yio.tro.opacha.menu.scenes;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.PlatformType;
import yio.tro.opacha.SoundManager;
import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.loading.LoadingParameters;
import yio.tro.opacha.game.loading.LoadingType;
import yio.tro.opacha.menu.elements.AnimationYio;
import yio.tro.opacha.menu.elements.ButtonYio;
import yio.tro.opacha.menu.elements.ground.GpSrNone;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;

public class SceneMainMenu extends SceneYio {
    public ButtonYio supportiveButton, settingsButton, playButton, tutorialButton;
    private int secretCount;

    @Override public void initialize() {
        setBackground(GroundIndex.MAIN_MENU);
        getGround().setSpawnRule(new GpSrNone());
        uiFactory.getButton().setSize(.88, .10).centerHorizontal().alignBottom(.65)
                .setFont(Fonts.titleFont).applyText("New Opacha")
                .setBorder(false).setBackgroundEnabled(false).setAnimation(AnimationYio.none)
                .setSilentReactionMode(true).setReaction(new Reaction() {
                    @Override protected void reaction() {
                        secretCount += 120;
                        if (secretCount > 300) { Scenes.secretScreen.create(); SoundManager.playSound(SoundManager.button); }
                    }
                });
        playButton = uiFactory.getButton().setSize(.74, .075).centerHorizontal().alignBottom(.34)
                .applyText("ui_play").setGroundIndex(GroundIndex.YELLOW).setAnimation(AnimationYio.none)
                .setKey("play").setReaction(new Reaction() {
                    @Override protected void reaction() { Scenes.chooseGameMode.create(); }
                });
        tutorialButton = uiFactory.getButton().setSize(.74, .060).centerHorizontal().alignBottom(.25)
                .applyText("ui_how_to_play").setAnimation(AnimationYio.none).setReaction(new Reaction() {
                    @Override protected void reaction() {
                        yioGdxGame.loadingManager.startLoading(LoadingType.tutorial, new LoadingParameters());
                    }
                });
        settingsButton = uiFactory.getButton().setSize(.74, .060).centerHorizontal().alignBottom(.17)
                .applyText("settings").setAnimation(AnimationYio.none).setReaction(new Reaction() {
                    @Override protected void reaction() { Scenes.settingsMenu.create(); }
                });
        supportiveButton = uiFactory.getButton().setSize(.35, .052).centerHorizontal().alignBottom(.065)
                .setFont(Fonts.smallFont).applyText(YioGdxGame.platformType == PlatformType.ios ? "about" : "ui_quit")
                .setBorder(false).setBackgroundEnabled(false).setAnimation(AnimationYio.none).tagAsBackButton()
                .setReaction(new Reaction() {
                    @Override protected void reaction() {
                        if (YioGdxGame.platformType == PlatformType.ios) Scenes.aboutMenu.create();
                        else yioGdxGame.exitApp();
                    }
                });
    }
    @Override protected void onAppear() { super.onAppear(); secretCount = 0; }
    @Override public void move() { super.move(); if (secretCount > 0) secretCount--; }
}
