package yio.tro.opacha.game.view.game_renders;

import yio.tro.opacha.game.gameplay.model.Link;
import yio.tro.opacha.menu.UiStyle;

public class RenderLinks extends GameRender {
    @Override protected void loadTextures(){}
    public void renderShadows(){}
    @Override public void render(){
        for(Link link:getObjectsLayer().linksManager.links){
            if(link.isBlueprint()||!link.isCurrentlyVisible())continue;
            double dx=Math.cos(link.angle),dy=Math.sin(link.angle),x=link.one.viewPosition.center.x,y=link.one.viewPosition.center.y;
            double start=boundary(link.one)+w*.005,end=link.distance-boundary(link.two)-w*.005;
            if(end<=start)continue;
            double stroke=w*.0018;
            if(link.directional){
                double gap=w*.0256,size=w*.006;
                for(double d=start+size;d<end-size;d+=gap){
                    double px=x+dx*d,py=y+dy*d;
                    UiStyle.get().line(batchMovable,px-dx*size-dy*size*.86,py-dy*size+dx*size*.86,px,py,stroke,UiStyle.OUTLINE,.90f);
                    UiStyle.get().line(batchMovable,px-dx*size+dy*size*.86,py-dy*size-dx*size*.86,px,py,stroke,UiStyle.OUTLINE,.90f);
                }
            }else UiStyle.get().line(batchMovable,x+dx*start,y+dy*start,x+dx*end,y+dy*end,stroke,UiStyle.OUTLINE,.85f);
        }
    }
    private double boundary(yio.tro.opacha.game.gameplay.model.Planet node){return node.viewPosition.radius*(node.isDefensive()?1.20:1.13);}
    @Override protected void disposeTextures(){}
}
