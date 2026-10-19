package yio.tro.opacha.menu;

import com.badlogic.gdx.Input;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.elements.slider.SliderYio;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** Keyboard focus follows the top scene; opening an overlay cannot activate a control behind it. */
public final class MenuKeyboard {
    private MenuKeyboard() { }

    public static boolean handle(MenuControllerYio menu, int key) {
        if (key != Input.Keys.TAB && key != Input.Keys.UP && key != Input.Keys.DOWN
                && key != Input.Keys.LEFT && key != Input.Keys.RIGHT && key != Input.Keys.ENTER
                && key != Input.Keys.SPACE) return false;
        if (menu.getCurrentScene() == null) return false;
        if (key == Input.Keys.SPACE && !menu.yioGdxGame.gamePaused && !menu.showKeyboardFocus) return false;
        ArrayList<InterfaceElement> controls = new ArrayList<>();
        for (InterfaceElement element : menu.getCurrentScene().getLocalElementsList()) {
            if (!element.isVisible() || !element.isTouchable() || element.getFactor().get() < .95) continue;
            if (element instanceof ButtonYio) {
                ButtonYio b = (ButtonYio) element;
                if (!b.isRenderable() || b.isInSilentReactionMode()) continue;
            } else if (!(element instanceof CircleButtonYio) && !(element instanceof SliderYio)
                    && !(element instanceof CheckButtonYio) && !(element instanceof VelocitySliderElement)) continue;
            controls.add(element);
        }
        if (controls.isEmpty()) return false;
        Collections.sort(controls, new Comparator<InterfaceElement>() {
            @Override public int compare(InterfaceElement a, InterfaceElement b) {
                int y = Float.compare(b.getPosition().y, a.getPosition().y);
                return y != 0 ? y : Float.compare(a.getPosition().x, b.getPosition().x);
            }
        });
        if (!controls.contains(menu.keyboardFocus)) {
            menu.keyboardFocus = controls.get(0);
            for (InterfaceElement element : controls) if (element instanceof ButtonYio
                    && ((ButtonYio) element).groundIndex == GroundIndex.YELLOW) {
                menu.keyboardFocus = element; break;
            }
        } else if (key == Input.Keys.TAB || key == Input.Keys.UP || key == Input.Keys.DOWN) {
            int direction = key == Input.Keys.UP || (key == Input.Keys.TAB
                    && com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) ? -1 : 1;
            int next = (controls.indexOf(menu.keyboardFocus) + controls.size() + direction) % controls.size();
            menu.keyboardFocus = controls.get(next);
        }
        menu.showKeyboardFocus = true;
        if (key == Input.Keys.LEFT || key == Input.Keys.RIGHT) {
            if (menu.keyboardFocus instanceof SliderYio) {
                SliderYio slider = (SliderYio) menu.keyboardFocus;
                int next = Math.max(0, Math.min(slider.numberOfSegments,
                        slider.getValueIndex() + (key == Input.Keys.LEFT ? -1 : 1)));
                slider.setValueIndex(next);
            } else if (menu.keyboardFocus instanceof VelocitySliderElement) {
                ((VelocitySliderElement) menu.keyboardFocus).stepFromInput(key == Input.Keys.LEFT ? -1 : 1);
            }
            return true;
        }
        if (key == Input.Keys.ENTER || key == Input.Keys.SPACE) {
            menu.keyboardFocus.pressArtificially();
        }
        return true;
    }

    public static void renderFocus(MenuViewYio view, InterfaceElement element) {
        MenuControllerYio menu = view.yioGdxGame.menuControllerYio;
        if (!menu.showKeyboardFocus || menu.keyboardFocus != element) return;
        yio.tro.opacha.stuff.RectangleYio r = element.getViewPosition();
        float gap = view.w * .009f, stroke = Math.max(2, view.w * .005f);
        UiStyle skin = UiStyle.get();
        skin.line(view.batch, r.x - gap, r.y - gap, r.x + r.width + gap, r.y - gap, stroke, UiStyle.TEXT, 1);
        skin.line(view.batch, r.x - gap, r.y + r.height + gap, r.x + r.width + gap, r.y + r.height + gap, stroke, UiStyle.TEXT, 1);
        skin.line(view.batch, r.x - gap, r.y - gap, r.x - gap, r.y + r.height + gap, stroke, UiStyle.TEXT, 1);
        skin.line(view.batch, r.x + r.width + gap, r.y - gap, r.x + r.width + gap, r.y + r.height + gap, stroke, UiStyle.TEXT, 1);
    }
}
