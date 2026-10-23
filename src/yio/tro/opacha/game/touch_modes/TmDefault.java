package yio.tro.opacha.game.touch_modes;

import yio.tro.opacha.Yio;
import yio.tro.opacha.game.GameController;
import yio.tro.opacha.game.GameRules;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.Planet;
import yio.tro.opacha.game.gameplay.model.PlanetsManager;
import yio.tro.opacha.game.view.game_renders.GameRender;
import yio.tro.opacha.game.view.game_renders.GameRendersList;
import yio.tro.opacha.menu.two_finger_touch.TwoFingerListener;
import yio.tro.opacha.menu.two_finger_touch.TwoFingerTouchManager;
import yio.tro.opacha.stuff.LongTapDetector;
import yio.tro.opacha.stuff.factor_yio.FactorYio;

public class TmDefault extends TouchMode implements TwoFingerListener{

    LongTapDetector longTapDetector;
    public FactorYio iconAnimFactor;
    boolean cameraBlocked;
    Planet touchDownPlanet;
    long touchDownTime;
    TwoFingerTouchManager twoFingerTouchManager;
    boolean twoFingerDetected;


    public TmDefault(GameController gameController) {
        super(gameController);

        iconAnimFactor = new FactorYio();
        twoFingerTouchManager = new TwoFingerTouchManager();

        initLongTapDetector();
    }


    private void initLongTapDetector() {
        longTapDetector = new LongTapDetector() {
            @Override
            public void onLongTapDetected() {
                TmDefault.this.onLongTapDetected();
            }
        };
    }


    @Override
    public void onModeBegin() {
        iconAnimFactor.reset();
        cameraBlocked = false;
        touchDownPlanet = null;
        twoFingerDetected = false;
    }


    @Override
    public void onModeEnd() {

    }


    @Override
    public void move() {
        longTapDetector.move();
        iconAnimFactor.move();
        twoFingerTouchManager.move();
    }


    @Override
    public boolean isCameraMovementEnabled() {
        return !cameraBlocked && gameController.objectsLayer.spectateManager.cameraMovementAllowed;
    }


    @Override
    public void onTouchDown() {
        if (GameRules.aiOnlyMode) return;
        if (gameController.objectsLayer.spectateManager.active) return;

        twoFingerDetected = false;
        longTapDetector.onTouchDown(gameController.currentTouch);
        twoFingerTouchManager.onTouchDown(gameController.currentTouch);

        cameraBlocked = false;
        touchDownPlanet = null;
        checkToBlockCamera();
        checkToHighlightPlanet();
    }


    private void checkToHighlightPlanet() {
        Planet touchedPlanet = getPlanetsManager().findTouchedPlanet(gameController.currentTouchConverted);
        if (touchedPlanet == null) return;

        touchedPlanet.setHoldHighlight(true);
        touchedPlanet.highlightEngine.select();
    }


    private void checkToBlockCamera() {
        Planet touchedPlanet = getPlanetsManager().findTouchedPlanet(gameController.currentTouchConverted);
        if (touchedPlanet == null) return;
        if (!touchedPlanet.isControlledByPlayer()) return;

        cameraBlocked = true;
        touchDownPlanet = touchedPlanet;
        touchDownTime = System.currentTimeMillis();
    }


    private PlanetsManager getPlanetsManager() {
        return gameController.objectsLayer.planetsManager;
    }


    @Override
    public void onTouchDrag() {
        if (GameRules.aiOnlyMode) return;
        longTapDetector.onTouchDrag(gameController.currentTouch);
        twoFingerTouchManager.onTouchDrag(gameController.currentTouch);
    }


    @Override
    public void onTouchUp() {
        if (GameRules.aiOnlyMode) return;
        longTapDetector.onTouchUp(gameController.currentTouch);
        twoFingerTouchManager.onTouchUp(gameController.currentTouch);
        checkToUnblockCamera();
        getPlanetsManager().releaseHighlights();
    }


    private void checkToUnblockCamera() {
        if (gameController.objectsLayer.tactics.pending != yio.tro.opacha.game.gameplay.model.TacticsManager.Action.none) {
            cameraBlocked = false;
            return;
        }
        Planet targetPlanet = getDragTarget();
        cameraBlocked = false;
        if (targetPlanet == null) return;
        touchDownPlanet.setAutoTarget(targetPlanet);
        touchDownPlanet.prepareToSendAutoTargetQuickly();
        yio.tro.opacha.menu.UiHaptics.orderIssued();
    }

    /** Preview and release share one decision, including ownership and route legality. */
    public Planet getDragTarget() {
        if (!cameraBlocked || twoFingerDetected || touchDownPlanet == null
                || !touchDownPlanet.isControlledByPlayer()
                || gameController.objectsLayer.tactics.pending != yio.tro.opacha.game.gameplay.model.TacticsManager.Action.none) return null;
        if (touchDownPlanet.viewPosition.center.distanceTo(gameController.currentTouchConverted)
                < 2 * touchDownPlanet.position.radius) return null;
        double angle = touchDownPlanet.viewPosition.center.angleTo(gameController.currentTouchConverted);
        Planet target = getPlanetsManager().findAdjoinedPlanetClosestToAngle(touchDownPlanet, angle);
        if (target == null || !touchDownPlanet.getLink(target).canUse(touchDownPlanet, touchDownPlanet.fraction)) return null;
        return Yio.distanceBetweenAngles(touchDownPlanet.viewPosition.center.angleTo(target.viewPosition.center), angle)
                <= Math.PI / 4 ? target : null;
    }

    public Planet getDragSource() { return getDragTarget() == null ? null : touchDownPlanet; }


    @Override
    public boolean onClick() {
        if (GameRules.aiOnlyMode) return false;
        if (gameController.objectsLayer.spectateManager.active) return false;
        ObjectsLayer objectsLayer = gameController.objectsLayer;
        objectsLayer.planetsManager.onClick(gameController.currentTouchConverted);
        return true;
    }


    private void onLongTapDetected() {
        if (gameController.objectsLayer.tactics.pending != yio.tro.opacha.game.gameplay.model.TacticsManager.Action.none) return;
        if (GameRules.aiOnlyMode) return;

        Planet touchedPlanet = getPlanetsManager().findTouchedPlanet(gameController.currentTouchConverted);
        if (touchedPlanet == null) return;
        if (!touchedPlanet.isControlledByPlayer()) return;

        cameraBlocked = false; // no auto target after long tap
        ObjectsLayer objectsLayer = gameController.objectsLayer;
        objectsLayer.planetsManager.onLongTappedPlanet(touchedPlanet);
    }


    @Override
    public GameRender getRender() {
        return GameRendersList.getInstance().renderTmDefault;
    }


    @Override
    public String getNameKey() {
        return null;
    }


    @Override
    public void onReachedTwoFingerState() {
        twoFingerDetected = true;
        cameraBlocked = true;
    }


    @Override
    public void onExitedTwoFingerState() {

    }


    @Override
    public void onSecondFingerDragged() {

    }


    @Override
    public void onTwoFingerRotated(double deltaAngle) {

    }
}
