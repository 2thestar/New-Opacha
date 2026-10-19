package yio.tro.opacha.game.export_import;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Clipboard;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.ai.AiType;
import yio.tro.opacha.game.gameplay.model.FractionType;

public class ExportManager {

    ObjectsLayer objectsLayer;
    private StringBuilder builder;


    public ExportManager(ObjectsLayer objectsLayer) {
        this.objectsLayer = objectsLayer;
    }


    public String performToClipboard() {
        String result = perform();

        Clipboard clipboard = Gdx.app.getClipboard();
        clipboard.setContents(result);

        return result;
    }


    public String perform() {
        builder = new StringBuilder();

        addTitle();
        saveSlotName();
        saveGameRules();
        saveAiTypes();
        saveGeneralInfo();
        savePlanets();
        saveLinks();

        builder.append("#");
        return builder.toString();
    }


    private void saveLinks() {
        startSection("links");
        builder.append(objectsLayer.linksManager.saveToString());
    }


    private void savePlanets() {
        startSection("planets");
        builder.append(objectsLayer.planetsManager.saveToString());
    }


    private void saveGeneralInfo() {
        startSection("general");
        builder.append(objectsLayer.gameController.initialLevelSize);
    }


    private void saveSlotName() {
        startSection("map_name");
        String name = "none";
        builder.append(name);
    }


    private void saveGameRules() {
        startSection("game_rules");
        builder.append(GameRules.saveToString());
    }

    private void saveAiTypes() {
        startSection("ai_types");
        for (FractionType fraction : FractionType.values()) {
            AiType type = objectsLayer.aiManager.getType(fraction);
            if (type != null) builder.append(fraction).append(" ").append(type).append(",");
        }
    }


    private void addTitle() {
        builder.append("opacha_level_code");
    }


    private void startSection(String name) {
        builder.append("#").append(name).append(":");
    }

}
