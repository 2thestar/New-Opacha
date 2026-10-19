package yio.tro.opacha.game.campaign;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.*;
import java.util.*;

/** Fixed, inspectable and editor-compatible level data. */
public final class CampaignLevels {
    public static final int COUNT = 48;
    private static List<Level> levels;
    private static List<String> chapters;
    private CampaignLevels() { }
    public static final class Level {
        public final int index, chapter, features;
        public final String title, mode, layout, concept, brief, hint, code;
        Level(JsonValue value) {
            index = value.getInt("index"); chapter = value.getInt("chapter"); features = value.getInt("features");
            title = value.getString("title"); mode = value.getString("mode"); layout = value.getString("layout");
            concept = value.getString("concept"); brief = value.getString("brief"); hint = value.getString("hint"); code = value.getString("code");
        }
    }
    private static void load() {
        if (levels != null) return;
        JsonValue root = new JsonReader().parse(Gdx.files.internal("campaign/levels.json"));
        List<Level> loaded = new ArrayList<>(); List<String> names = new ArrayList<>();
        for (JsonValue value : root.get("chapters")) names.add(value.asString());
        for (JsonValue value : root.get("levels")) {
            Level level = new Level(value);
            if (level.index != loaded.size()) throw new IllegalStateException("Campaign indices must be contiguous");
            loaded.add(level);
        }
        if (loaded.size() != COUNT || names.size() != 4) throw new IllegalStateException("Campaign needs 12 maps in each of four modes");
        levels = java.util.Collections.unmodifiableList(loaded); chapters = java.util.Collections.unmodifiableList(names);
    }
    public static Level get(int index) {
        load();
        if (index < 0 || index >= COUNT) throw new IllegalArgumentException("Invalid campaign level: " + index);
        return levels.get(index);
    }
    public static String chapter(int index) { load(); return chapters.get(index); }
}
