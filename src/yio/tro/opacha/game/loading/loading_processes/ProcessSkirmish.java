package yio.tro.opacha.game.loading.loading_processes;

import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.Difficulty;
import yio.tro.opacha.game.GameMode;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.loading.LoadingManager;
import yio.tro.opacha.game.gameplay.ai.AiType;
import yio.tro.opacha.game.gameplay.model.FractionType;
import java.util.*;

public class ProcessSkirmish extends AbstractLoadingProcess {

    public ProcessSkirmish(LoadingManager loadingManager) {
        super(loadingManager);
    }


    @Override
    public void prepare() {
        initGameMode(GameMode.skirmish);
        initLevelSize(getLevelSize(getMapSizeFromParameters()));
    }


    @Override
    public void applyGameRules() {
        GameRules.aiOnlyMode = (boolean) loadingParameters.getParameter("ai_only");
        int difficultyIndex = (int) loadingParameters.getParameter("difficulty");
        GameRules.difficulty = Difficulty.values()[difficultyIndex];
    }


    @Override
    public void loadSavedData(int step) {

    }


    @Override
    public void generateLevel() {
        int seed = (int) loadingParameters.getParameter("seed");
        YioGdxGame.predictableRandom.setSeed(seed);
        int colors = (int) loadingParameters.getParameter("colors");
        getObjectsLayer().mapGenerator.generateLinkedMap(getMapSizeFromParameters(), colors);
        assignAiTypes(seed);
    }

    private void assignAiTypes(int seed) {
        Object parameter = loadingParameters.getParameter("opponents");
        int selection = parameter instanceof Number ? ((Number) parameter).intValue() : 0;
        List<AiType> mixed = new ArrayList<>(Arrays.asList(AiType.values()));
        Collections.shuffle(mixed, new Random(seed ^ 0x4e65774f));
        int index = 0;
        for (FractionType fraction : FractionType.values()) {
            if (fraction == FractionType.neutral || (fraction == FractionType.green && !GameRules.aiOnlyMode)) continue;
            if (!getObjectsLayer().planetsManager.isFractionAlive(fraction)) continue;
            AiType type = selection >= 1 && selection <= AiType.values().length
                    ? AiType.values()[selection - 1] : mixed.get(index++ % mixed.size());
            GameRules.aiTypes.put(fraction, type);
        }
    }


    @Override
    public void onEndCreation() {

    }
}
