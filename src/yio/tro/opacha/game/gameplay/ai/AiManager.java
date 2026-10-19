package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.game.GameMode;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.gameplay.IGameplayManager;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.FractionType;

import java.util.ArrayList;

public class AiManager implements IGameplayManager{

    ObjectsLayer objectsLayer;
    public ArrayList<AbstractAiEntity> entities;


    public AiManager(ObjectsLayer objectsLayer) {
        this.objectsLayer = objectsLayer;
        entities = new ArrayList<>();
    }


    @Override
    public void defaultValues() {
        entities.clear();
    }


    @Override
    public void onEndCreation() {
        createEntities();
        for (AbstractAiEntity entity : entities) {
            entity.onEndCreation();
        }
    }


    private void createEntities() {
        entities.clear();
        for (FractionType fractionType : FractionType.values()) {
            if (fractionType == FractionType.neutral) continue;
            if (!objectsLayer.planetsManager.isFractionAlive(fractionType)) continue;
            if (fractionType == FractionType.green && !GameRules.aiOnlyMode) continue;
            addEntity(fractionType);
        }
    }


    private void addEntity(FractionType fractionType) {
        AbstractAiEntity aiEntity;
        switch (getType(fractionType)) {
            case savvy: aiEntity = new AiSavvyEntity(objectsLayer); break;
            case silly: aiEntity = new AiSillyEntity(objectsLayer); break;
            default: aiEntity = new AiCustomEntity(objectsLayer); break;
        }
        aiEntity.setFractionType(fractionType);
        aiEntity.setDifficulty(GameRules.difficulty);
        entities.add(aiEntity);
    }

    public AiType getType(FractionType fraction) {
        if (fraction == null || fraction == FractionType.neutral) return null;
        if (fraction == FractionType.green && !GameRules.aiOnlyMode) return null;
        AiType type = GameRules.aiTypes.get(fraction);
        return type == null ? AiType.simple : type;
    }


    @Override
    public void moveActually() {
        if (objectsLayer.gameController.gameMode == GameMode.editor) return;
        for (AbstractAiEntity entity : entities) {
            entity.moveActually();
        }
    }


    @Override
    public void moveVisually() {
        for (AbstractAiEntity entity : entities) {
            entity.moveVisually();
        }
    }
}
