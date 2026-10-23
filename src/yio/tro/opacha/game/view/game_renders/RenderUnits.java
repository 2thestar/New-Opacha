package yio.tro.opacha.game.view.game_renders;

import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.UiStyle;

public class RenderUnits extends GameRender {
    static final class Pose {Planet start,target;double angle,dx,dy,rx,ry,d,x,y,outgoing,incoming,intake;}
    private final Pose pose=new Pose();
    @Override protected void loadTextures(){}
    Pose pose(Unit unit){
            Planet start=unit.getStart(),target=unit.getTarget();
            double angle=start.position.center.angleTo(target.position.center),dx=Math.cos(angle),dy=Math.sin(angle);
            double progress=unit.acceleratableFactor.isInAppearState()?unit.acceleratableFactor.value:1-unit.acceleratableFactor.value;
            double rx=Math.min(w*.038,w*(.020+Math.sqrt(unit.value)*.0012)),ry=rx*.73;
            if(unit.value>50){rx=w*.029;ry=w*.019;}
            double from=start.viewPosition.radius+rx*.82,to=unit.link.distance-target.viewPosition.radius-rx*.82;
            double d=from+progress*(to-from),x=start.viewPosition.center.x+dx*d,y=start.viewPosition.center.y+dy*d;
            double intake=0;
            if(unit.digger&&unit.link.isBlueprint())intake=1-BattlefieldArt.smooth(Math.max(0,from+unit.link.constructionProgress()*(to-from)-d)/(w*.046));
            double outgoing=1-BattlefieldArt.smooth((d-rx-start.viewPosition.radius+rx*.18)/(w*.027));
            boolean friendly=getObjectsLayer().tactics.alliances.friendly(unit.fraction,target.fraction);
            double incoming=unit.digger?0:friendly?1-BattlefieldArt.smooth((unit.link.distance-target.viewPosition.radius-d-rx+rx*.18)/(w*.027)):0;
            if(yio.tro.opacha.menu.UiMotion.reduced){outgoing=0;incoming=0;}
            pose.start=start;pose.target=target;pose.angle=angle;pose.dx=dx;pose.dy=dy;pose.rx=rx;pose.ry=ry;pose.d=d;pose.x=x;pose.y=y;pose.outgoing=outgoing;pose.incoming=incoming;pose.intake=intake;
            return pose;
    }
    public double getVisualRouteDistance(Unit unit){return pose(unit).d;}
    @Override public void render(){
        BattlefieldArt art=BattlefieldArt.get();
        for(Unit unit:getObjectsLayer().unitManager.units){
            if(!unit.isAlive()||!unit.isCurrentlyVisible()||BattlefieldVisuals.working(unit))continue;
            Pose p=pose(unit);Planet start=p.start,target=p.target;
            double angle=p.angle,dx=p.dx,dy=p.dy,rx=p.rx,ry=p.ry,d=p.d,x=p.x,y=p.y,outgoing=p.outgoing,incoming=p.incoming;
            if(unit.value<=50){
                double sourceRoot=start.viewPosition.radius+w*.0077*outgoing,targetRoot=target.viewPosition.radius+w*.0077*incoming;
                if(outgoing>.003)art.neck(batchMovable,start.viewPosition.center.x+dx*sourceRoot,start.viewPosition.center.y+dy*sourceRoot,
                        angle,Math.max(0,d-sourceRoot-rx*.25),start.viewPosition.radius*.32*outgoing,ry*.85,outgoing,BattlefieldArt.troopFace(unit.fraction));
                if(incoming>.003)art.neck(batchMovable,target.viewPosition.center.x-dx*targetRoot,target.viewPosition.center.y-dy*targetRoot,
                        angle+Math.PI,Math.max(0,unit.link.distance-d-targetRoot-rx*.25),target.viewPosition.radius*.32*incoming,ry*.85,incoming,BattlefieldArt.troopFace(unit.fraction));
                double sx=rx*(1+.06*outgoing-.10*incoming)*(1-.36*p.intake),sy=ry*(1-.26*p.intake);
                if(outgoing<.003&&incoming<.003)art.ellipse(batchMovable,x,y,sx+w*.0018,sy+w*.0018,angle,UiStyle.BOARD,1);
                art.ellipse(batchMovable,x,y,sx,sy,angle,BattlefieldArt.troopFace(unit.fraction),1);
                art.ovalOutline(batchMovable,x,y,sx,sy,angle,w*.0019,BattlefieldArt.troopEdge(unit.fraction),outgoing,incoming);
            }else art.tank(batchMovable,x,y,rx,angle,unit.fraction);
        }
        // Keep the font together instead of flushing shapes for every fleet count.
        for(Unit unit:getObjectsLayer().unitManager.units){
            if(!unit.isAlive()||!unit.isCurrentlyVisible()||BattlefieldVisuals.working(unit))continue;
            Pose p=pose(unit);
            art.count(batchMovable,unit.title.string,p.x,p.y,Math.min(w*.0225,p.ry*1.32),p.rx*1.65);
        }
    }
    @Override protected void disposeTextures(){}
}
