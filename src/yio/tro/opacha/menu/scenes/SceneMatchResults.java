package yio.tro.opacha.menu.scenes;

import yio.tro.opacha.game.gameplay.model.MatchResults;
import yio.tro.opacha.menu.LanguagesManager;
import yio.tro.opacha.menu.elements.ButtonYio;
import yio.tro.opacha.menu.elements.AnimationYio;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.stuff.factor_yio.FactorYio;

import java.util.ArrayList;

public class SceneMatchResults extends ModalSceneYio{

    MatchResults matchResults;
    private ArrayList<String> lines;
    public ButtonYio continueButton;

    @Override protected void beginCreation() {
        menuControllerYio.setCurrentScene(this);
        destroyAllVisibleElements();
        menuControllerYio.checkToRemoveInvisibleElements();
    }


    @Override
    protected void initialize() {
        lines = new ArrayList<>();
        matchResults = null;
        createCloseButton();
        updateBackReaction();
        createDefaultLabel(0.8, 0.3);
        defaultLabel.alignBottom(.28);
        continueButton = uiFactory.getButton().setParent(defaultLabel).setSize(.62, .065)
                .centerHorizontal().alignBottom(.025).applyText("continue_match")
                .setGroundIndex(yio.tro.opacha.menu.elements.ground.GroundIndex.YELLOW)
                .setBackgroundEnabled(true).setBorder(true)
                .setAnimation(AnimationYio.down).setReaction(new Reaction() {
                    @Override protected void reaction() { getBackReaction().performReactActions(menuControllerYio); }
                });
    }


    private void updateDefaultLabelText() {
        lines.clear();
        lines.add(LanguagesManager.getInstance().getString(getTitleKey()));
        lines.add(getWinnerColorLine());
        lines.add(" ");

        defaultLabel.applyText(lines);
    }


    @Override
    protected void onAppear() {
        super.onAppear();
    }


    @Override
    public void move() {
        super.move();
    }


    private String getWinnerColorLine() {
        if (matchResults.coalition.size() > 1) {
            StringBuilder colors = new StringBuilder();
            for (yio.tro.opacha.game.gameplay.model.FractionType owner : matchResults.coalition) {
                if (colors.length() > 0) colors.append(", ");
                colors.append(languagesManager.getString(owner.toString()));
            }
            return languagesManager.getString("winners") + ": " + colors;
        }
        String colorString = LanguagesManager.getInstance().getString(matchResults.winner + "");
        return LanguagesManager.getInstance().getString("winner") + ": " + colorString;
    }


    private String getTitleKey() {
        if (matchResults.humanWon) {
            return "player_won";
        } else {
            return "ai_won";
        }
    }


    private void updateBackReaction() {
        closeButton.setReaction(getBackReaction());
    }


    private Reaction getBackReaction() {
        if (matchResults != null && matchResults.campaignMode) {
            return new Reaction() {
                @Override
                protected void reaction() {
                    yioGdxGame.setGamePaused(true);
                    menuControllerYio.destroyGameView();
                    Scenes.onCampaignLevelCompleted.create();
                    Scenes.onCampaignLevelCompleted.setCurrentLevelIndex(matchResults.levelIndex);
                }
            };
        }

        return new Reaction() {
            @Override
            protected void reaction() {
                yioGdxGame.setGamePaused(true);
                menuControllerYio.destroyGameView();
                Scenes.chooseGameMode.create();
            }
        };
    }


    public void setMatchResults(MatchResults matchResults) {
        this.matchResults = matchResults;
        updateDefaultLabelText();
        updateBackReaction();
    }

}
