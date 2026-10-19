package yio.tro.opacha.menu.scenes.gameplay;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.SettingsManager;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.Planet;
import yio.tro.opacha.game.gameplay.model.PlanetsManager;
import yio.tro.opacha.game.gameplay.model.PlanetType;
import yio.tro.opacha.menu.elements.AnimationYio;
import yio.tro.opacha.menu.elements.ButtonYio;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.elements.ground.GroundIndex;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.scenes.ModalSceneYio;
import yio.tro.opacha.menu.scenes.Scenes;

public class SceneSelectionOverlay extends ModalSceneYio {
    public ButtonYio bDefensive, bEconomic, actionsButton, halvedModeButton;
    Planet selectedPlanet;

    @Override protected void initialize() {
        bEconomic = action(.035, "ui_grow", new Reaction() {
            @Override protected void reaction() {
                PlanetsManager manager = gameController.objectsLayer.planetsManager;
                if (!manager.canBeUpgradedToEconomic(selectedPlanet)) { Scenes.notification.show("ui_need_ten"); return; }
                manager.onEconomicUpgradeButtonClicked();
            }
        }).setKey("economic_upgrade");
        bDefensive = action(.355, "ui_defend", new Reaction() {
            @Override protected void reaction() {
                PlanetsManager manager = gameController.objectsLayer.planetsManager;
                if (!manager.canBeUpgradedToDefensive(selectedPlanet)) { Scenes.notification.show("ui_need_ten"); return; }
                manager.onDefensiveUpgradeButtonClicked();
            }
        });
        actionsButton = action(.675, "node_actions", new Reaction() {
            @Override protected void reaction() {
                Scenes.nodeActions.node = selectedPlanet;
                destroy(); Scenes.nodeActions.create();
            }
        }).setGroundIndex(GroundIndex.YELLOW);
        halvedModeButton = uiFactory.getButton().setSize(.38, .052).centerHorizontal()
                .alignBottom(.11 + getBottomOffset()).setFont(Fonts.smallFont).applyText("ui_send_half")
                .setAnimation(AnimationYio.down).setReaction(new Reaction() {
                    @Override protected void reaction() {
                        ObjectsLayer layer = gameController.objectsLayer;
                        layer.planetsManager.onLongTappedPlanet(selectedPlanet);
                        updateHalfLabel();
                    }
                });
    }

    private ButtonYio action(double left, String key, Reaction reaction) {
        return uiFactory.getButton().setSize(.29, .058).alignLeft(left)
                .alignBottom(.035 + getBottomOffset()).setFont(Fonts.smallFont).applyText(key)
                .setTouchOffset(.004)
                .setAnimation(AnimationYio.down).setIgnoreResumePause(true).setReaction(reaction);
    }

    @Override protected void onAppear() { super.onAppear(); updateHalfLabel(); }
    private void updateHalfLabel() {
        halvedModeButton.applyText(selectedPlanet.halvedSelectionMode ? "ui_send_all" : "ui_send_half")
                .setGroundIndex(selectedPlanet.halvedSelectionMode ? GroundIndex.YELLOW : GroundIndex.BUTTON_WHITE);
    }
    @Override protected void appearElement(InterfaceElement element) {
        if (element == bEconomic && !yio.tro.opacha.game.RuleFeatures.has(yio.tro.opacha.game.RuleFeatures.GROW)) return;
        if (element == bDefensive && !yio.tro.opacha.game.RuleFeatures.has(yio.tro.opacha.game.RuleFeatures.DEFEND)) return;
        if (element == halvedModeButton && !yio.tro.opacha.game.RuleFeatures.has(yio.tro.opacha.game.RuleFeatures.HALF)) return;
        if (element == actionsButton && (yio.tro.opacha.game.GameRules.features & 112) == 0) return;
        if (selectedPlanet.type == PlanetType.economic && element == bEconomic) return;
        if (selectedPlanet.type == PlanetType.defensive && element == bDefensive) return;
        if (element == halvedModeButton && !SettingsManager.getInstance().halvedModeButtonEnabled) return;
        super.appearElement(element);
    }
    private double getBottomOffset() { return SettingsManager.getInstance().thinBezels ? .025 : 0; }
    public void setSelectedPlanet(Planet planet) { selectedPlanet = planet; }
}
