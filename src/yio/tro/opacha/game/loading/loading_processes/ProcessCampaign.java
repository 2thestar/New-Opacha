package yio.tro.opacha.game.loading.loading_processes;

import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.campaign.CampaignLevels;
import yio.tro.opacha.game.loading.LoadingManager;
import yio.tro.opacha.menu.scenes.Scenes;
import java.util.Random;

public final class ProcessCampaign extends AbstractLoadingProcess {
    private CampaignLevels.Level level;
    public ProcessCampaign(LoadingManager manager) { super(manager); }
    @Override public void prepare() {
        Scenes.campaign.freePlay = false;
        level = CampaignLevels.get((int) loadingParameters.getParameter("index"));
        initGameMode(GameMode.campaign); initLevelSize(LevelSize.tiny);
        YioGdxGame.predictableRandom = new Random(4001L + level.index);
    }
    @Override public void applyGameRules() { GameRules.levelIndex = level.index; }
    @Override public void loadSavedData(int step) { }
    @Override public void generateLevel() {
        getGameController().importManager.perform(level.code);
        GameRules.levelIndex = level.index;
    }
    @Override public void onEndCreation() {
        Scenes.quickVelocityTutorial.destroy();
        if (!level.brief.isEmpty()) { Scenes.campaignBrief.useHint = false; Scenes.campaignBrief.create(); }
    }
}
