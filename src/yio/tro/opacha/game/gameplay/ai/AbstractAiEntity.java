package yio.tro.opacha.game.gameplay.ai;

import yio.tro.opacha.AdaptiveDifficultyManager;
import yio.tro.opacha.SettingsManager;
import yio.tro.opacha.RefreshRateDetector;
import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.game.Difficulty;
import yio.tro.opacha.game.GameMode;
import yio.tro.opacha.game.VelocityManager;
import yio.tro.opacha.game.gameplay.IGameplayManager;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.FractionType;
import yio.tro.opacha.game.gameplay.model.PathFinder;
import yio.tro.opacha.game.gameplay.model.Planet;
import yio.tro.opacha.game.gameplay.model.PlanetsManager;

import java.util.ArrayList;

public abstract class AbstractAiEntity implements IGameplayManager{

    public static final int APS = 2;
    ObjectsLayer objectsLayer;
    FractionType fractionType;
    private double decisionCountdown, commandCountdown;
    private int decisionInterval;
    public Difficulty difficulty;
    int currentAllowedActionCount;
    public float dynamicPower;


    public AbstractAiEntity(ObjectsLayer objectsLayer) {
        this.objectsLayer = objectsLayer;
        difficulty = Difficulty.easy;
        updateDynamicPower();
        currentAllowedActionCount = APS;
        commandCountdown = 60;
        decisionInterval = 400;
    }


    private void updateDynamicPower() {
        if (objectsLayer.gameController.gameMode != GameMode.campaign || !SettingsManager.getInstance().adaptiveDifficulty) {
            dynamicPower = 1;
            return;
        }

        dynamicPower = AdaptiveDifficultyManager.getInstance().getPower();
    }


    protected void increaseActionCount() {
        if (currentAllowedActionCount < APS) {
            currentAllowedActionCount = APS;
        }
    }


    public void setFractionType(FractionType fractionType) {
        this.fractionType = fractionType;
    }


    protected abstract void perform();


    @Override
    public void defaultValues() {
        fractionType = null;
    }


    @Override
    public void onEndCreation() {

    }


    @Override
    public void moveActually() {
        double step = RefreshRateDetector.getInstance().multiplier * VelocityManager.getInstance().value;
        commandCountdown -= step;
        if (commandCountdown <= 1e-6) {
            commandCountdown += 60;
            increaseActionCount();
        }
        decisionCountdown -= step;
        if (decisionCountdown <= 1e-6) {
            decisionCountdown += decisionInterval;
            perform();
        }
    }


    public boolean isActionAllowed() {
        return currentAllowedActionCount > 0;
    }


    public void onActionPerformed() {
        currentAllowedActionCount--;
    }


    @Override
    public void moveVisually() {

    }


    protected ArrayList<Planet> getPlanetsList() {
        return getPlanetsManager().planets;
    }


    protected PlanetsManager getPlanetsManager() {
        return objectsLayer.planetsManager;
    }


    protected boolean isBeyondCommand(Planet planet) {
        return planet.fraction != fractionType;
    }


    protected void launchUnits(Planet start, Planet target) {
        if (start.unitsInside < 1) return;
        objectsLayer.planetsManager.sendUnits(start, target);
    }


    protected PathFinder getPathFinder() {
        return objectsLayer.planetsManager.pathFinder;
    }


    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        updatePerformFrequency();
    }


    private void updatePerformFrequency() {
        int frequency;
        switch (difficulty) {
            default:
            case hard:
                frequency = 20;
                break;
            case normal:
                frequency = 180;
                break;
            case easy:
                frequency = 400;
                break;
        }
        frequency *= 2 - dynamicPower;

        decisionInterval = frequency;
        decisionCountdown = YioGdxGame.random.nextInt(Math.max(1, frequency));
    }
}
