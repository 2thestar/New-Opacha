package yio.tro.opacha.game.gameplay;

import yio.tro.opacha.SettingsManager;
import yio.tro.opacha.game.GameController;
import yio.tro.opacha.game.GameMode;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.export_import.ExportManager;
import yio.tro.opacha.game.gameplay.ai.AiManager;
import yio.tro.opacha.game.gameplay.fill.FillManager;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.gameplay.particles.ParticlesManager;
import yio.tro.opacha.game.gameplay.stars.StarsManager;
import yio.tro.opacha.stuff.PointYio;
import yio.tro.opacha.stuff.RepeatYio;
import yio.tro.opacha.stuff.TouchableYio;

public class ObjectsLayer implements TouchableYio, AcceleratableYio {

    public GameController gameController;
    public PointYio tempPoint;
    public ParticlesManager particlesManager;
    public StarsManager starsManager;
    public PlanetsManager planetsManager;
    public MapGenerator mapGenerator;
    public LinksManager linksManager;
    public UnitManager unitManager;
    public AiManager aiManager;
    RepeatYio<ObjectsLayer> repeatCheckToEnd;
    boolean readyToEndMatch;
    public ImpossibruDetector impossibruDetector;
    public FillManager fillManager;
    public SpectateManager spectateManager;
    public EditorWorker editorWorker;
    public ExportManager exportManager;
    public TacticsManager tactics;


    public ObjectsLayer(GameController gameController) {
        this.gameController = gameController;

        tempPoint = new PointYio();
        particlesManager = new ParticlesManager(this);
        starsManager = new StarsManager(this);
        planetsManager = new PlanetsManager(this);
        linksManager = new LinksManager(this);
        mapGenerator = new MapGenerator(this);
        unitManager = new UnitManager(this);
        tactics = new TacticsManager(this);
        aiManager = new AiManager(this);
        impossibruDetector = new ImpossibruDetector(this);
        fillManager = new FillManager(this);
        spectateManager = new SpectateManager(this);
        editorWorker = new EditorWorker(this);
        exportManager = new ExportManager(this);

        defaultValues();
        initRepeats();
    }


    private void initRepeats() {
        repeatCheckToEnd = new RepeatYio<ObjectsLayer>(this, 30) {
            @Override
            public void performAction() {
                parent.checkToEndMatch();
            }
        };
    }


    private void defaultValues() {
        readyToEndMatch = true;
        planetsManager.defaultValues();
        linksManager.defaultValues();
        starsManager.defaultValues();
        unitManager.defaultValues();
        aiManager.defaultValues();
        fillManager.defaultValues();
        spectateManager.defaultValues();
        editorWorker.defaultValues();
    }


    @Override
    public void moveActually() {
        if (!readyToEndMatch && !spectateManager.active) return;
        if (GameRules.isFlagMode() && tactics.flagWinner != null) return;
        particlesManager.moveActually();
        starsManager.moveActually();
        planetsManager.moveActually();
        linksManager.moveActually();
        unitManager.moveActually();
        tactics.moveActually();
        aiManager.moveActually();
        fillManager.moveActually();
        spectateManager.moveActually();
        editorWorker.moveActually();
    }


    @Override
    public void moveVisually() {
        particlesManager.moveVisually();
        starsManager.moveVisually();
        planetsManager.moveVisually();
        linksManager.moveVisually();
        unitManager.moveVisually();
        aiManager.moveVisually();
        repeatCheckToEnd.move();
        fillManager.moveVisually();
        spectateManager.moveVisually();
        editorWorker.moveVisually();
    }


    public void onEndCreation() {
        starsManager.onEndCreation();
        planetsManager.onEndCreation();
        linksManager.onEndCreation();
        unitManager.onEndCreation();
        tactics.onEndCreation();
        aiManager.onEndCreation();
        fillManager.onEndCreation();
        spectateManager.onEndCreation();
        editorWorker.onEndCreation();
    }


    private void checkToEndMatch() {
        if (!readyToEndMatch) return;
        if (gameController.gameMode == GameMode.editor) return;
        FractionType specialWinner = tactics.winner();
        if (GameRules.isFlagMode() && specialWinner == null) return;
        if (specialWinner == null) {
            if (planetsManager.areThereAtLeastTwoAliveFractions()) return;
            if (unitManager.areThereAtLeastTwoAliveFractions()) return;
            FractionType moving = unitManager.getAliveFraction(null);
            if (moving != null && planetsManager.getAliveFraction(null) != moving) return;
        }
        if (spectateManager.active) return;
        readyToEndMatch = false;

        MatchResults matchResults = new MatchResults();
        matchResults.setWinner(specialWinner == null ? planetsManager.getAliveFraction(null) : specialWinner);
        if (GameRules.playType == yio.tro.opacha.game.PlayType.alliances && tactics.alliances.peacefulSurvivors()) {
            for (FractionType owner : FractionType.values()) if (owner != FractionType.neutral && tactics.alliances.alive(owner)) matchResults.coalition.add(owner);
        }
        boolean humanWon = matchResults.winner == FractionType.green && !GameRules.aiOnlyMode;
        if (GameRules.playType == yio.tro.opacha.game.PlayType.alliances && !GameRules.aiOnlyMode
                && tactics.alliances.alive(FractionType.green) && tactics.alliances.peacefulSurvivors()) humanWon = true;
        matchResults.setHumanWon(humanWon);
        matchResults.setCampaignMode(gameController.gameMode == GameMode.campaign);
        matchResults.setCalendarMode(gameController.gameMode == GameMode.calendar);

        if (matchResults.campaignMode) {
            int levelIndex = (int) GameRules.initialParameters.getParameter("index");
            matchResults.setLevelIndex(levelIndex);
        }

        if (gameController.gameMode == GameMode.tutorial) {
            matchResults.setCampaignMode(true);
            matchResults.setLevelIndex(0);
        }

        if (matchResults.calendarMode) {
            int year = (int) GameRules.initialParameters.getParameter("year");
            int month = (int) GameRules.initialParameters.getParameter("month");
            int day = (int) GameRules.initialParameters.getParameter("day");
            matchResults.setDate(year, month, day);
        }

        if (planetsManager.lastCapturedPlanet == null) {
            for (Planet planet : planetsManager.planets) if (planet.fraction == matchResults.winner) {
                planetsManager.lastCapturedPlanet = planet;
                break;
            }
        }
        if (SettingsManager.getInstance().spectateAfterVictory && specialWinner == null) {
            spectateManager.launch(planetsManager.lastCapturedPlanet, matchResults);
            return;
        }

        fillManager.launch(planetsManager.lastCapturedPlanet);
        cleanupForResult();
        fillManager.setMatchResults(matchResults);
    }

    private void cleanupForResult() {
        tactics.cancel();
        yio.tro.opacha.menu.scenes.Scenes.nodeActions.destroy();
        yio.tro.opacha.menu.scenes.Scenes.diplomacy.destroy();
    }


    public void move() {
        for (int speed = gameController.speedManager.getSpeed(); speed > 0; speed--) {
            moveActually();
        }

        moveVisually();
    }


    public void onClick() {
        System.out.println("ObjectsLayer.onClick");
    }


    @Override
    public boolean onTouchDown(PointYio touchPoint) {
        return false;
    }


    @Override
    public boolean onTouchDrag(PointYio touchPoint) {
        return false;
    }


    @Override
    public boolean onTouchUp(PointYio touchPoint) {
        return false;
    }




    public void onDestroy() {

    }
}
