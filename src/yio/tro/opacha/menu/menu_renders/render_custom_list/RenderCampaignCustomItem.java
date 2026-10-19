package yio.tro.opacha.menu.menu_renders.render_custom_list;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import yio.tro.opacha.menu.elements.customizable_list.AbstractCustomListItem;
import yio.tro.opacha.menu.elements.customizable_list.CampaignCustomItem;
import yio.tro.opacha.menu.elements.customizable_list.CciInnerItem;
import yio.tro.opacha.menu.elements.customizable_list.CciType;
import yio.tro.opacha.stuff.GraphicsYio;

import java.util.HashMap;

public class RenderCampaignCustomItem extends AbstractRenderCustomListItem{


    private CampaignCustomItem campaignCustomItem;
    HashMap<CciType, TextureRegion> mapTextures;
    private float alpha;


    @Override
    public void loadTextures() {
        mapTextures = new HashMap<>();
        for (CciType cciType : CciType.values()) {
            mapTextures.put(cciType, GraphicsYio.loadTextureRegion("menu/campaign/campaign_" + cciType + ".png", true));
        }
    }


    @Override
    public void renderItem(AbstractCustomListItem item) {
        campaignCustomItem = (CampaignCustomItem) item;
        alpha = campaignCustomItem.customizableListYio.getAlpha();

        GraphicsYio.setBatchAlpha(batch, alpha);

        for (CciInnerItem cciInnerItem : campaignCustomItem.items) {
            renderSingleInnerItem(cciInnerItem);
        }

        GraphicsYio.setBatchAlpha(batch, 1);
    }


    private void renderSingleInnerItem(CciInnerItem cciInnerItem) {
        yio.tro.opacha.menu.UiStyle skin = yio.tro.opacha.menu.UiStyle.get();
        yio.tro.opacha.stuff.RectangleYio r = cciInnerItem.position;
        boolean done = cciInnerItem.type == CciType.completed;
        boolean locked = cciInnerItem.type == CciType.unknown;
        skin.rounded(batch,r.x,r.y,r.width,r.height,yio.tro.opacha.menu.UiStyle.OUTLINE,alpha * (locked ? .35f : 1));
        float inset = Math.max(1, .004f * w);
        skin.rounded(batch,r.x+inset,r.y+inset,r.width-2*inset,r.height-2*inset,
                done ? yio.tro.opacha.menu.UiStyle.GREEN : yio.tro.opacha.menu.UiStyle.BOARD,alpha);
        cciInnerItem.title.font.setColor(done ? yio.tro.opacha.menu.UiStyle.INK : yio.tro.opacha.menu.UiStyle.TEXT);
        GraphicsYio.renderTextOptimized(batch,blackPixel,cciInnerItem.title,alpha * (locked ? .55f : 1));
        cciInnerItem.title.font.setColor(yio.tro.opacha.menu.UiStyle.TEXT);
        if (done) {
            skin.line(batch,r.x+r.width*.67,r.y+r.height*.77,r.x+r.width*.74,r.y+r.height*.70,
                    Math.max(1,w*.003),yio.tro.opacha.menu.UiStyle.INK,alpha);
            skin.line(batch,r.x+r.width*.74,r.y+r.height*.70,r.x+r.width*.87,r.y+r.height*.86,
                    Math.max(1,w*.003),yio.tro.opacha.menu.UiStyle.INK,alpha);
        }
        GraphicsYio.setBatchAlpha(batch,alpha);

        if (cciInnerItem.selectionEngineYio.isSelected()) {
            GraphicsYio.setBatchAlpha(batch, cciInnerItem.selectionEngineYio.getAlpha());
            GraphicsYio.drawByRectangle(batch, blackPixel, cciInnerItem.selectionPosition);
            GraphicsYio.setBatchAlpha(batch, alpha);
        }
    }
}
