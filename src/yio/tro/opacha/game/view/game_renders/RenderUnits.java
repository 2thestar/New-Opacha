package yio.tro.opacha.game.view.game_renders;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import yio.tro.opacha.game.LevelSize;
import yio.tro.opacha.game.gameplay.model.FractionType;
import yio.tro.opacha.game.gameplay.model.Unit;
import yio.tro.opacha.game.gameplay.model.UnitManager;
import yio.tro.opacha.stuff.GraphicsYio;
import yio.tro.opacha.stuff.Storage3xTexture;

import java.util.HashMap;

public class RenderUnits extends GameRender{

    HashMap<FractionType, Storage3xTexture> mapTextures;
    private UnitManager unitManager;


    @Override
    protected void loadTextures() {
        mapTextures = new HashMap<>();
        for (FractionType fractionType : FractionType.values()) {
            if (fractionType == FractionType.neutral) continue;
            mapTextures.put(fractionType, load3xTexture("unit_" + fractionType));
        }
    }


    @Override
    public void render() {
        unitManager = getObjectsLayer().unitManager;

        for (Unit unit : unitManager.units) {
            if (!unit.isCurrentlyVisible()) continue;
            if (!unit.isAlive()) continue;
            renderSingleUnit(unit);
        }
    }


    private void renderSingleUnit(Unit unit) {
        if (yio.tro.opacha.SettingsManager.getInstance().colorblindMode) {
            GraphicsYio.drawByCircle(batchMovable, getUnitTexture(unit), unit.viewPosition);
        } else {
            yio.tro.opacha.menu.UiStyle skin = yio.tro.opacha.menu.UiStyle.get();
            float x = unit.viewPosition.center.x, y = unit.viewPosition.center.y, r = unit.viewPosition.radius;
            skin.disc(batchMovable, x, y, r, yio.tro.opacha.menu.UiStyle.INK, 1);
            skin.disc(batchMovable, x, y, r * .78f, yio.tro.opacha.menu.UiStyle.faction(unit.fraction), 1);
        }

        if (gameController.initialLevelSize != LevelSize.normal) {
            GraphicsYio.renderText(batchMovable, unit.title);
        }
    }


    private TextureRegion getUnitTexture(Unit unit) {
        Storage3xTexture storage3xTexture = mapTextures.get(unit.fraction);
        if (gameController.initialLevelSize == LevelSize.normal) {
            return storage3xTexture.getLowest();
        }

        return storage3xTexture.getTexture(getCurrentZoomQuality());
    }


    @Override
    protected void disposeTextures() {

    }
}
