package yio.tro.opacha.menu.elements;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.Yio;
import yio.tro.opacha.game.VelocityManager;
import yio.tro.opacha.menu.MenuControllerYio;
import yio.tro.opacha.menu.UiHaptics;
import yio.tro.opacha.menu.menu_renders.MenuRenders;
import yio.tro.opacha.menu.menu_renders.RenderInterfaceElement;
import yio.tro.opacha.stuff.*;

public class VelocitySliderElement extends InterfaceElement<VelocitySliderElement> {

    public RectangleYio centralLine;
    float values[];
    public int index;
    public CircleYio framePosition;
    public CircleYio visualFrame;
    public RenderableTextYio title;
    boolean touched;
    public SelectionEngineYio selectionEngineYio;
    boolean forceVisualFrame;
    private float grabOffset;
    private int activePointer = -1;


    public VelocitySliderElement(MenuControllerYio menuControllerYio, int id) {
        super(menuControllerYio, id);
        centralLine = new RectangleYio();
        framePosition = new CircleYio();
        title = new RenderableTextYio();
        title.setFont(Fonts.smallFont);
        visualFrame = new CircleYio();
        touched = false;
        selectionEngineYio = new SelectionEngineYio();
        index = -1;
        initValues();
    }


    private void initValues() {
        values = new float[]{
                0.1f,
                0.2f,
                0.3f,
                0.4f,
                0.5f,
                0.75f,
                1f,
                1.25f,
                1.5f,
                2f,
                2.5f,
                3f,
                4f
        };
    }


    public void loadValues() {
        syncIndexWithVelocity();
        forceVisualFrame = true;
    }


    private void syncIndexWithVelocity() {
        float actualValue = VelocityManager.getInstance().value;
        int index = convertVelocityToIndex(actualValue);
        if (index == -1) {
            index = 6;
        }
        setIndex(index);
    }


    private int convertVelocityToIndex(float velocity) {
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(velocity - values[i]) < 0.01f) {
                return i;
            }
        }
        return -1;
    }


    public void setIndex(int index) {
        if (index < 0) {
            index = 0;
        }
        if (index > values.length - 1) {
            index = values.length - 1;
        }
        this.index = index;
        updateTitle();
    }


    private void updateTitle() {
        float value = values[index];
        title.setString((value == (int) value ? Integer.toString((int) value) : Float.toString(value)) + "x");
        title.updateMetrics();
    }


    @Override
    protected VelocitySliderElement getThis() {
        return this;
    }


    @Override
    public void move() {
        updateViewPosition();
    }


    @Override
    protected void onApplyParent() {
        super.onApplyParent();
        updateCentralLinePosition();
        updateFramePosition();
        updateVisualFrame();
        updateTitlePosition();
        moveSelection();
    }


    private void updateVisualFrame() {
        if (forceVisualFrame || !menuControllerYio.yioGdxGame.gameView.coversAllScreen()) {
            forceVisualFrame = false;
            visualFrame.setBy(framePosition);
            return;
        }
        visualFrame.center.y = framePosition.center.y;
        visualFrame.radius = framePosition.radius;
        if (touched) return;
        visualFrame.center.x += 0.2 * (framePosition.center.x - visualFrame.center.x);
    }


    private void moveSelection() {
        if (touched) return;
        selectionEngineYio.move();
    }


    private void updateTitlePosition() {
        title.position.x = visualFrame.center.x - title.width / 2;
        title.position.y = visualFrame.center.y + title.height / 2;
        title.updateBounds();
    }


    private void updateFramePosition() {
        framePosition.center.y = centralLine.y + centralLine.height / 2;
        framePosition.radius = 0.10f * GraphicsYio.width;
        framePosition.center.x = centralLine.x + index * centralLine.width / (values.length - 1);
    }


    private void updateCentralLinePosition() {
        float inset = .11f * GraphicsYio.width;
        centralLine.x = viewPosition.x + inset;
        centralLine.height = 2 * GraphicsYio.borderThickness;
        centralLine.y = viewPosition.y + viewPosition.height / 2 - centralLine.height / 2;
        centralLine.width = viewPosition.width - 2 * inset;
    }


    @Override
    public void onDestroy() {
        touched = false;
        activePointer = -1;
        VelocityManager.getInstance().saveValues();
    }


    public void decreaseSpeed() {
        if (index == 0) return;
        setIndex(index - 1);
        applyValues();
    }


    @Override
    public void onAppear() {
        touched = false;
        activePointer = -1;
        loadValues();
        forceVisualFrame = true;
    }


    @Override
    public boolean checkToPerformAction() {
        return false;
    }


    @Override
    public boolean touchDown() {
        touched = isTouched();
        if (!touched) return false;
        selectionEngineYio.select();
        grabOffset = Math.abs(currentTouch.x - visualFrame.center.x) <= visualFrame.radius
                ? currentTouch.x - visualFrame.center.x : 0;
        applyCurrentTouch();
        return true;
    }


    private boolean isTouched() {
        return isTouchInsideRectangle(currentTouch.x, currentTouch.y, viewPosition, 0, .005f * GraphicsYio.height);
    }


    void applyCurrentTouch() {
        int previousIndex = index;

        float sx = centralLine.x;
        float length = centralLine.width;
        float v = (currentTouch.x - grabOffset - sx) / length;
        if (v < 0) {
            v = 0;
        }
        if (v > 1) {
            v = 1;
        }
        setIndex(Math.round(v * (values.length - 1)));

        if (previousIndex != index) {
            applyValues();
            UiHaptics.speedStep();
        }

        visualFrame.center.x = sx + v * length;
    }


    public void applyValues() {
        VelocityManager.getInstance().setValue(values[index]);
    }

    public int getStepCount() { return values.length; }

    public boolean isDragging() { return touched; }

    public boolean cancelTouch(int pointer) {
        if (pointer != activePointer) return false;
        touched = false;
        activePointer = -1;
        forceVisualFrame = true;
        VelocityManager.getInstance().saveValues();
        return true;
    }

    public void stepFromInput(int direction) {
        int previousIndex = index;
        setIndex(index + direction);
        if (index == previousIndex) return;
        applyValues();
        UiHaptics.speedStep();
    }

    /** One finger owns the drag until release; another touch cannot move or release it. */
    @Override public boolean touchDownElement(int x, int y, int pointer, int button) {
        if (activePointer != -1) return true;
        if (!super.touchDownElement(x, y, pointer, button)) return false;
        activePointer = pointer;
        return true;
    }

    @Override public boolean touchDragElement(int x, int y, int pointer) {
        return pointer == activePointer && super.touchDragElement(x, y, pointer);
    }

    @Override public boolean touchUpElement(int x, int y, int pointer, int button) {
        if (pointer != activePointer) return false;
        boolean handled = super.touchUpElement(x, y, pointer, button);
        activePointer = -1;
        return handled;
    }


    @Override
    public boolean touchDrag() {
        if (!touched) return false;
        applyCurrentTouch();
        return true;
    }


    @Override
    public boolean touchUp() {
        if (!touched) return false;
        applyCurrentTouch();
        touched = false;
        return true;
    }


    @Override
    public RenderInterfaceElement getRenderSystem() {
        return MenuRenders.renderVelocitySlider;
    }
}
