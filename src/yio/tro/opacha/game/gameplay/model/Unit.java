package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.game.GameController;
import yio.tro.opacha.game.VelocityManager;
import yio.tro.opacha.game.gameplay.AcceleratableYio;
import yio.tro.opacha.stuff.CircleYio;
import yio.tro.opacha.stuff.RenderableTextYio;
import yio.tro.opacha.stuff.factor_yio.FactorYio;
import yio.tro.opacha.stuff.object_pool.ReusableYio;

public class Unit implements ReusableYio, AcceleratableYio{

    UnitManager unitManager;
    public Link link;
    public CircleYio viewPosition;
    public AcceleratableFactor acceleratableFactor;
    public int value;
    boolean readyToUpdateViewPosition;
    public FractionType fraction;
    boolean alive;
    Planet target;
    public RenderableTextYio title;
    public boolean digger;
    private double digAccumulator;
    private static int nextJourney;
    int journeyId;


    public Unit(UnitManager unitManager) {
        this.unitManager = unitManager;
        viewPosition = new CircleYio();
        acceleratableFactor = new AcceleratableFactor();
        title = new RenderableTextYio();
        title.setFont(Fonts.unitFont);
    }


    @Override
    public void reset() {
        link = null;
        viewPosition.reset();
        acceleratableFactor.reset();
        value = 0;
        readyToUpdateViewPosition = true;
        fraction = null;
        setAlive(false);
        target = null;
        digger = false;
        digAccumulator = 0;
    }


    @Override
    public void moveActually() {
        applyWalk();
    }


    private void applyWalk() {
        if (!alive) return;
        float oldPosition = acceleratableFactor.value;
        if (acceleratableFactor.move(VelocityManager.getInstance().value)) {
            if (digger) dig(oldPosition);
            unitManager.objectsLayer.tactics.checkBombs(this, oldPosition, acceleratableFactor.value);
            readyToUpdateViewPosition = true;
        } else {
            onReachedTarget();
        }
    }

    private void dig(float previous) {
        if (!link.isBlueprint()) { digger = false; return; }
        float next = acceleratableFactor.value;
        float front = link.constructionProgress();
        if (next <= front) return;
        digAccumulator += Math.max(0, next - Math.max(previous, front)) * link.constructionCost;
        int requested = next >= 1 ? link.constructionCost - link.constructionSpent : (int) (digAccumulator + .00001);
        int work = Math.min(value, Math.min(link.constructionCost - link.constructionSpent, requested));
        link.constructionSpent += work;
        digAccumulator -= work;
        setValue(value - work);
        acceleratableFactor.value = Math.min(next, link.constructionProgress());
        if (link.isBlueprint()) acceleratableFactor.needsToMove = true;
        if (value == 0) setAlive(false);
        if (!link.isBlueprint()) digger = false;
    }


    private void onReachedTarget() {
        link.checkForBattles();
        if (!isAlive()) return;

        setAlive(false);
        target.onUnitReached(this);
    }


    public boolean isReadyToBeRemoved() {
        return !alive;
    }


    public boolean isCurrentlyVisible() {
        GameController gameController = unitManager.objectsLayer.gameController;
        return gameController.cameraController.isCircleInViewFrame(viewPosition);
    }


    public void launch(Link link, Planet start) {
        if (link == null) {
            System.out.println("Unit.launch(): problem");
            return;
        }

        this.link = link;
        journeyId = ++nextJourney;

        if (start == link.one) {
            acceleratableFactor.appear(link.movementMultiplier * link.speedFrom(start));
        } else {
            acceleratableFactor.destroy(link.movementMultiplier * link.speedFrom(start));
        }

        viewPosition.setRadius(2 * link.thickness);
        target = link.getOppositePlanet(start);
        digger = link.isBlueprint();
        setAlive(true);
    }


    private void updateViewPosition() {
        viewPosition.center.setBy(link.one.viewPosition.center);
        viewPosition.center.relocateRadial(acceleratableFactor.value * link.distance, link.angle);
    }


    @Override
    public void moveVisually() {
        checkToUpdateViewPosition();
    }


    private void checkToUpdateViewPosition() {
        if (!readyToUpdateViewPosition) return;
        readyToUpdateViewPosition = false;
        updateViewPosition();
        updateTitlePosition();
    }


    private void updateTitlePosition() {
        title.position.setBy(viewPosition.center);
        title.position.x -= title.width / 2;
        title.position.y += 1.8f * viewPosition.radius;
    }


    public void setAlive(boolean alive) {
        this.alive = alive;

        checkToNotifyLink();
    }


    private void checkToNotifyLink() {
        if (link == null) return;

        if (alive) {
            link.onUnitAdded(this);
        } else {
            link.onUnitDied(this);
        }
    }


    public boolean isAlive() {
        return alive;
    }


    public void setValue(int value) {
        this.value = value;
        title.setString("" + value);
        title.updateMetrics();
    }

    public void takeDamage(int damage) {
        setValue(Math.max(0, value - damage));
        if (value == 0) setAlive(false);
    }


    public void setFraction(FractionType fraction) {
        this.fraction = fraction;
    }
}
