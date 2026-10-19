package yio.tro.opacha.game.campaign;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import yio.tro.opacha.menu.elements.customizable_list.CciType;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.TimeMeasureYio;

import java.util.ArrayList;

public class CampaignManager {


    private static CampaignManager instance;
    public static final int LAST_LEVEL_INDEX = CampaignLevels.COUNT - 1;
    public static final int LEVELS_PER_CHAPTER = 12;
    private static final String PREFS = "yio.tro.opacha.campaign.v2";
    ArrayList<Integer> completedLevels;
    private int visualChapter;


    public CampaignManager() {
        completedLevels = new ArrayList<>();
    }


    public static void initialize() {
        instance = null;
        getInstance(); // load
    }


    public static CampaignManager getInstance() {
        if (instance == null) {
            instance = new CampaignManager();
            instance.loadValues();
        }

        return instance;
    }


    public void loadValues() {
        Preferences preferences = getPreferences();
        String source = preferences.getString("progress");
        String[] split = source.split(" ");
        completedLevels.clear();
        for (String token : split) {
            if (token.length() < 1) continue;
            try {
                int index = Integer.parseInt(token);
                if (index >= 0 && index <= LAST_LEVEL_INDEX && !completedLevels.contains(index)) completedLevels.add(index);
            } catch (NumberFormatException ignored) { }
        }
        visualChapter = chapterOf(getIndexOfHighestUnlockedLevel());
    }


    public void onLevelCompleted(int index) {
        if (index < 0 || index > LAST_LEVEL_INDEX || isLevelCompleted(index)) return;
        completedLevels.add(index);
        visualChapter = chapterOf(getIndexOfHighestUnlockedLevel());
        Scenes.campaign.onLevelMarkedAsCompleted();
        saveValues();
    }


    public int getNextLevelIndex(int index) {
        for (int offset = 1; offset <= CampaignLevels.COUNT; offset++) {
            int next = (index + offset) % CampaignLevels.COUNT;
            if (!isLevelCompleted(next)) return next;
        }
        return index;
    }


    public boolean isLevelCompleted(int index) {
        if (index == -1) return true;

        for (int completedLevel : completedLevels) {
            if (completedLevel == index) return true;
        }
        return false;
    }


    public boolean areAllLevelsCompleted() {
        return completedLevels.size() > LAST_LEVEL_INDEX;
    }


    public void saveValues() {
        Preferences preferences = getPreferences();
        StringBuilder builder = new StringBuilder();
        for (Integer completedLevel : completedLevels) {
            builder.append(completedLevel).append(" ");
        }
        preferences.putString("progress", builder.toString());
        preferences.flush();
    }


    private Preferences getPreferences() {
        return Gdx.app.getPreferences(PREFS);
    }


    public int getIndexOfHighestUnlockedLevel() {
        int result = 0;
        for (int index = 0; index <= LAST_LEVEL_INDEX; index++) {
            if (getLevelType(index) != CciType.unlocked) continue;
            result = index;
        }
        return result;
    }


    public int getNumberOfCompletedLevels() {
        return completedLevels.size();
    }

    public static int chapterOf(int levelIndex) { return Math.max(0, levelIndex) / LEVELS_PER_CHAPTER; }
    public int getVisualChapter() { return visualChapter; }


    public CciType getLevelType(int levelIndex) {
        if (isLevelCompleted(levelIndex)) {
            return CciType.completed;
        }
        if (isLevelCompleted(levelIndex - 1)) {
            return CciType.unlocked;
        }
        return CciType.unknown;
    }

}
