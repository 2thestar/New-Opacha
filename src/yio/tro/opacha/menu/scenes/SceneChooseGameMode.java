package yio.tro.opacha.menu.scenes;

import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.elements.AnimationYio;
import yio.tro.opacha.menu.elements.ButtonYio;

public class SceneChooseGameMode extends SceneYio {


    public ButtonYio skirmishButton;
    public ButtonYio mapsButton;


    @Override
    public void initialize() {
        setBackground(GroundIndex.CYAN);
        uiFactory.getLabelElement().setSize(.8, .08).centerHorizontal().alignBottom(.76)
                .setFont(yio.tro.opacha.Fonts.titleFont).setString(languagesManager.getString("ui_choose_game"));

        skirmishButton = uiFactory.getButton()
                .setSize(0.8, 0.075)
                .setPosition(0.1, 0.61)
                .applyText("skirmish")
                .setGroundIndex(GroundIndex.YELLOW)
                .setReaction(getSkirmishReaction())
                .setAnimation(AnimationYio.none);

        uiFactory.getButton()
                .clone(previousElement)
                .alignBottom(previousElement, 0.02)
                .applyText("campaign").setGroundIndex(GroundIndex.BUTTON_WHITE)
                .setReaction(getCampaignReaction());

        mapsButton = uiFactory.getButton().clone(previousElement).alignBottom(previousElement,.02)
                .applyText("ui_maps").setReaction(new Reaction() { @Override protected void reaction() {
                    Scenes.campaign.freePlay = true; Scenes.campaign.create();
                }});

        uiFactory.getButton()
                .clone(previousElement)
                .alignBottom(previousElement, 0.02)
                .applyText("calendar")
                .setGroundIndex(GroundIndex.BUTTON_WHITE)
                .setReaction(getCalendarReaction());

        uiFactory.getButton()
                .clone(previousElement)
                .alignBottom(previousElement, 0.02)
                .applyText("editor").setGroundIndex(GroundIndex.BUTTON_WHITE)
                .setReaction(getEditorReaction());

        spawnBackButton(getBackReaction());
    }


    private Reaction getEditorReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                Scenes.editorLobby.create();
            }
        };
    }


    private Reaction getBackReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                yioGdxGame.setGamePaused(true);
                Scenes.mainMenu.create();
            }
        };
    }


    private Reaction getCampaignReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                Scenes.campaign.freePlay = false; Scenes.campaign.create();
            }
        };
    }


    private Reaction getCalendarReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                Scenes.calendar.create();
            }
        };
    }


    private Reaction getSkirmishReaction() {
        return new Reaction() {
            @Override
            protected void reaction() {
                Scenes.skirmishMenu.create();
            }
        };
    }


}
