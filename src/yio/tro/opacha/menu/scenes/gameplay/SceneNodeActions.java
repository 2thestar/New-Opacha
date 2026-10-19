package yio.tro.opacha.menu.scenes.gameplay;

import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.scenes.*;
import java.util.ArrayList;

public final class SceneNodeActions extends ModalSceneYio {
    public Planet node;
    public ButtonYio routeButton, buyBombButton, buildGunButton, fireButton, placeBombButton, doneButton;
    private final ArrayList<ButtonYio> options = new ArrayList<>();

    @Override protected void onDestroy() {
        if (node == null) return;
        node.deselect();
        node.forceDeselection();
    }

    @Override protected void initialize() {
        createCloseButton();
        createDefaultLabel(.86, .4);
        routeButton = option("build_route", 0);
        buyBombButton = option("buy_bomb", 1);
        buildGunButton = option("build_artillery", 2);
        fireButton = option("fire_artillery", 3);
        placeBombButton = option("place_bomb", 4);
        doneButton = uiFactory.getButton().setParent(defaultLabel).setSize(.76, .055)
                .centerHorizontal().alignBottom(.02).setAnimation(AnimationYio.down).applyText("ui_done")
                .setReaction(new Reaction() { @Override protected void reaction() { destroy(); }});
    }

    private ButtonYio option(String key, final int action) {
        ButtonYio button = uiFactory.getButton().setParent(defaultLabel).setSize(.76, .065)
                .setBackgroundEnabled(true).setBorder(true)
                .centerHorizontal().setAnimation(AnimationYio.down).applyText(key)
                .setReaction(new Reaction() { @Override protected void reaction() { choose(action); }});
        return button;
    }

    @Override protected void onAppear() {
        options.clear();
        if (yio.tro.opacha.game.RuleFeatures.has(16)) options.add(routeButton);
        if (node.isEconomic() && yio.tro.opacha.game.RuleFeatures.has(32)) options.add(buyBombButton);
        if (yio.tro.opacha.game.RuleFeatures.has(64)) {
            if (node.type != PlanetType.artillery) options.add(buildGunButton);
            else options.add(fireButton);
        }
        int stock = yioGdxGame.gameController.objectsLayer.tactics.inventory[FractionType.green.ordinal()];
        if (stock > 0 && yio.tro.opacha.game.RuleFeatures.has(32)) options.add(placeBombButton);
        placeBombButton.applyText(languagesManager.getString("place_bomb") + " (" + stock + ")");
        defaultLabel.setSize(.86, .11 + .08 * options.size()).alignBottom(.12).centerHorizontal();
        for (int i = 0; i < options.size(); i++) options.get(i).alignTop(.02 + .08 * i);
    }

    @Override protected void appearElement(InterfaceElement element) {
        if ((element == routeButton || element == buyBombButton || element == buildGunButton
                || element == fireButton || element == placeBombButton) && !options.contains(element)) return;
        super.appearElement(element);
    }

    private void choose(int action) {
        if (node == null || !node.isControlledByPlayer()) { destroy(); return; }
        TacticsManager t = yioGdxGame.gameController.objectsLayer.tactics;
        boolean success = true;
        if (action == 0) t.begin(TacticsManager.Action.blueprint, node);
        if (action == 1) success = t.buyBomb(node);
        if (action == 2) success = t.buildArtillery(node);
        if (action == 3) {
            if (node.unitsInside < 20 || node.artilleryReload > .0001f) success = false;
            else t.begin(TacticsManager.Action.artillery, node);
        }
        if (action == 4) t.begin(TacticsManager.Action.bomb, node);
        if (success) { destroy(); if (action == 1 || action == 2) yioGdxGame.gameController.objectsLayer.planetsManager.deselect(); }
        else Scenes.notification.show(languagesManager.getString(node.artilleryReload > .0001f && action == 3 ? "reloading" : "need_twenty"));
    }
}
