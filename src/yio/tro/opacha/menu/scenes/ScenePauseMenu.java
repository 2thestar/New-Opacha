package yio.tro.opacha.menu.scenes;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.menu.elements.LabelElement;
import yio.tro.opacha.menu.elements.ground.GpSrDown;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.elements.AnimationYio;
import yio.tro.opacha.menu.elements.ButtonYio;

public class ScenePauseMenu extends SceneYio {


    public ButtonYio resumeButton;
    public ButtonYio restartButton;
    public ButtonYio mainMenuButton;
    public LabelElement labelElement;
    public ButtonYio hintButton;


    @Override
    public void initialize() {
        setBackground(GroundIndex.CYAN);
        getGround().setSpawnRule(new GpSrDown());
        uiFactory.getLabelElement().setSize(.8, .09).centerHorizontal().alignBottom(.72)
                .setFont(Fonts.titleFont).setString(languagesManager.getString("ui_paused"));

        resumeButton = uiFactory.getButton()
                .setSize(0.8, 0.08)
                .setPosition(0.1, 0.55)
                .applyText("resume")
                .setGroundIndex(GroundIndex.YELLOW)
                .setReaction(getResumeReaction())
                .setAnimation(AnimationYio.none);

        restartButton = uiFactory.getButton()
                .setSize(0.8, 0.08)
                .centerHorizontal()
                .alignBottom(previousElement, 0.02)
                .applyText("restart")
                .setReaction(getRestartReaction())
                .setAnimation(AnimationYio.none);

        mainMenuButton = uiFactory.getButton()
                .setSize(0.8, 0.08)
                .centerHorizontal()
                .alignBottom(previousElement, 0.02)
                .applyText("main_menu")
                .setReaction(getMainMenuReaction())
                .setAnimation(AnimationYio.none)
                .tagAsBackButton();

        labelElement = uiFactory.getLabelElement()
                .setSize(0.86, 0.045)
                .alignTop(0.01)
                .centerHorizontal()
                .setFont(Fonts.smallFont)
                .setString("-");
        hintButton = uiFactory.getButton().setSize(.50,.055).centerHorizontal().alignBottom(.20)
                .applyText("ui_hint").setAnimation(AnimationYio.none).setReaction(new Reaction() {
                    @Override protected void reaction() {
                        yioGdxGame.gameView.appear(); gameController.createMenuOverlay(); yioGdxGame.setGamePaused(false);
                        Scenes.campaignBrief.useHint = true; Scenes.campaignBrief.create();
                    }
                });
    }

    @Override protected void appearElement(yio.tro.opacha.menu.elements.InterfaceElement element) {
        if (element == hintButton && yioGdxGame.gameController.gameMode != yio.tro.opacha.game.GameMode.campaign) return;
        super.appearElement(element);
    }


    @Override
    protected void onAppear() {
        super.onAppear();
        updateLabel();
    }


    private void updateLabel() {
        if (GameRules.levelIndex == -1) {
            labelElement.setString("");
            return;
        }

        if (yioGdxGame.gameController.gameMode == yio.tro.opacha.game.GameMode.campaign)
            labelElement.setString((GameRules.levelIndex + 1) + ". " + yio.tro.opacha.game.campaign.CampaignLevels.get(GameRules.levelIndex).title);
        else labelElement.setString(languagesManager.getString("level") + ": " + GameRules.levelIndex);
    }


    private Reaction getMainMenuReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                yioGdxGame.setGamePaused(true);
                Scenes.mainMenu.create();
            }
        };
    }


    private Reaction getRestartReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                Scenes.confirmRestart.create();
            }
        };
    }


    private Reaction getResumeReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                yioGdxGame.gameView.appear();
                gameController.createMenuOverlay();
                yioGdxGame.setGamePaused(false);
            }
        };
    }
}
