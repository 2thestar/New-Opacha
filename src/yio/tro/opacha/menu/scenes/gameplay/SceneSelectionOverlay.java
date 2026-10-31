package yio.tro.opacha.menu.scenes.gameplay;

import yio.tro.opacha.Fonts;
import yio.tro.opacha.SettingsManager;
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
    public ButtonYio bDefensive, bEconomic, actionsButton, halvedModeButton, stopSendingButton;
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
                if (selectedPlanet == null || !selectedPlanet.isControlledByPlayer()) return;
                Scenes.nodeActions.node = selectedPlanet;
                destroy(); Scenes.nodeActions.create();
            }
        }).setGroundIndex(GroundIndex.YELLOW);
        stopSendingButton = uiFactory.getButton().setSize(.49, .055).alignRight(.035)
                .alignBottom(.11 + getBottomOffset()).setFont(Fonts.smallFont).applyText("ui_stop_sending")
                .setTouchOffset(.004).setGroundIndex(GroundIndex.YELLOW)
                .setAnimation(AnimationYio.down).setIgnoreResumePause(true).setReaction(new Reaction() {
                    @Override protected void reaction() { gameController.objectsLayer.planetsManager.stopSending(selectedPlanet); }
                });
        halvedModeButton = uiFactory.getButton().setSize(.38, .055).centerHorizontal()
                .alignBottom(.11 + getBottomOffset()).setFont(Fonts.smallFont).applyText("ui_send_half")
                .setTouchOffset(.004).setIgnoreResumePause(true)
                .setAnimation(AnimationYio.down).setReaction(new Reaction() {
                    @Override protected void reaction() {
                        if (selectedPlanet == null || !selectedPlanet.isControlledByPlayer()) return;
                        selectedPlanet.setHalvedSelectionMode(!selectedPlanet.halvedSelectionMode);
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

    @Override protected void onAppear() {
        super.onAppear(); updateHalfLabel(); positionOrderButtons();
        bEconomic.alignBottom(.035 + getBottomOffset());
        bDefensive.alignBottom(.035 + getBottomOffset());
        actionsButton.alignBottom(.035 + getBottomOffset());
    }

    private void positionOrderButtons() {
        halvedModeButton.alignBottom(.11 + getBottomOffset());
        stopSendingButton.alignBottom(.11 + getBottomOffset());
        boolean sending = selectedPlanet != null && selectedPlanet.hasAutoTarget();
        boolean halfVisible = yio.tro.opacha.game.RuleFeatures.has(yio.tro.opacha.game.RuleFeatures.HALF)
                && SettingsManager.getInstance().halvedModeButtonEnabled;
        if (sending && halfVisible) {
            halvedModeButton.setSize(.40, .055).alignLeft(.035);
            stopSendingButton.setSize(.49, .055).alignRight(.035);
        } else {
            halvedModeButton.setSize(.38, .055).centerHorizontal();
            stopSendingButton.setSize(.49, .055).centerHorizontal();
        }
    }

    /** Route deletion/capture must also remove a now-meaningless Stop control. */
    public void onAutoTargetChanged(Planet planet) {
        if (planet != selectedPlanet || !isCurrentlyVisible() || stopSendingButton == null) return;
        positionOrderButtons();
        if (planet.hasAutoTarget()) super.appearElement(stopSendingButton);
        else stopSendingButton.destroy();
    }
    private void updateHalfLabel() {
        halvedModeButton.applyText(selectedPlanet.halvedSelectionMode ? "ui_send_all" : "ui_send_half")
                .setGroundIndex(selectedPlanet.halvedSelectionMode ? GroundIndex.YELLOW : GroundIndex.BUTTON_WHITE);
    }
    @Override protected void appearElement(InterfaceElement element) {
        if (element == stopSendingButton && !selectedPlanet.hasAutoTarget()) return;
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
