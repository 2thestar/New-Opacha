package yio.tro.opacha.menu.scenes.gameplay;

import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.reactions.Reaction;
import yio.tro.opacha.menu.scenes.*;
import java.util.EnumMap;

public final class SceneDiplomacy extends ModalSceneYio {
    public final EnumMap<FractionType, ButtonYio> rows = new EnumMap<>(FractionType.class);
    private String lastState;

    @Override protected void initialize() {
        createCloseButton();
        createDefaultLabel(.88, .31);
        defaultLabel.alignBottom(.15);
        for (final FractionType other : FractionType.values()) {
            if (other == FractionType.green || other == FractionType.neutral) continue;
            ButtonYio row = uiFactory.getButton().setParent(defaultLabel).setSize(.78, .07)
                    .setBackgroundEnabled(true).setBorder(true)
                    .centerHorizontal().alignTop(.025 + .09 * rows.size())
                    .setAnimation(AnimationYio.down).applyText(" ")
                    .setReaction(new Reaction() { @Override protected void reaction() {
                        AllianceManager a = yioGdxGame.gameController.objectsLayer.tactics.alliances;
                        if (a.areAllied(FractionType.green, other)) a.breakPact(FractionType.green, other);
                        else a.offer(FractionType.green, other);
                        refresh();
                    }});
            rows.put(other, row);
        }
    }

    @Override protected void onAppear() { lastState = null; refresh(); }
    @Override public void move() { if (isCurrentlyVisible()) refresh(); }

    private void refresh() {
        AllianceManager a = yioGdxGame.gameController.objectsLayer.tactics.alliances;
        StringBuilder state = new StringBuilder();
        for (FractionType other : rows.keySet()) state.append(other).append(a.alive(other))
                .append(a.areAllied(FractionType.green, other)).append(a.isBroken(FractionType.green, other))
                .append(a.hasOffer(other, FractionType.green)).append(a.hasOffer(FractionType.green, other));
        if (state.toString().equals(lastState)) return;
        lastState = state.toString();
        for (FractionType other : rows.keySet()) {
            boolean alive = a.alive(other), allied = a.areAllied(FractionType.green, other), broken = a.isBroken(FractionType.green, other);
            String key = !alive ? "eliminated" : broken ? "pact_broken" : allied ? "break_pact"
                    : a.hasOffer(other, FractionType.green) ? "accept_pact" : a.hasOffer(FractionType.green, other) ? "pact_pending" : "offer_pact";
            rows.get(other).applyText(languagesManager.getString(other.toString()) + ": " + languagesManager.getString(key),
                    allied ? BackgroundYio.green : BackgroundYio.gray)
                    .setGroundIndex(allied ? yio.tro.opacha.menu.elements.ground.GroundIndex.GREEN : yio.tro.opacha.menu.elements.ground.GroundIndex.BUTTON_WHITE)
                    .setTouchable(alive && !broken && !key.equals("pact_pending"));
        }
    }
}
