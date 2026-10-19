package yio.tro.opacha.game;

import yio.tro.opacha.game.export_import.SavableYio;
import yio.tro.opacha.game.loading.LoadingParameters;
import yio.tro.opacha.game.loading.LoadingType;
import yio.tro.opacha.game.gameplay.ai.AiType;
import yio.tro.opacha.game.gameplay.model.FractionType;
import java.util.EnumMap;

public class GameRules {

    public static LoadingParameters initialParameters;
    public static LoadingType initialLoadingType;
    public static int fastForwardSpeed;
    public static boolean aiOnlyMode;
    public static Difficulty difficulty;
    public static int levelIndex;
    public static PlayType playType = PlayType.conquest;
    public static boolean directionalRoutes;
    public static int features = RuleFeatures.ALL;
    public static final EnumMap<FractionType, AiType> aiTypes = new EnumMap<>(FractionType.class);


    public static void bootInit() {
        initialParameters = null;
        initialLoadingType = null;
        fastForwardSpeed = 3;
        defaultValues();
    }


    static void defaultValues() {
        aiOnlyMode = false;
        difficulty = Difficulty.easy;
        levelIndex = -1;
        playType = PlayType.conquest;
        directionalRoutes = false;
        features = RuleFeatures.ALL;
        aiTypes.clear();
    }


    public static String saveToString() {
        return difficulty + " " + aiOnlyMode + " " + playType + " " + directionalRoutes + " " + features;
    }

    public static boolean isFlagMode() { return playType == PlayType.capture_flag || playType == PlayType.home_flags; }
}
