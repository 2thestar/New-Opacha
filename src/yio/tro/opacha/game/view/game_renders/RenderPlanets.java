package yio.tro.opacha.game.view.game_renders;

import com.badlogic.gdx.graphics.Color;
import yio.tro.opacha.SettingsManager;
import yio.tro.opacha.game.GameMode;
import yio.tro.opacha.game.gameplay.ai.AiType;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.stuff.*;

public class RenderPlanets extends GameRender {
    private static final Color CONVEYOR_LIGHT=Color.valueOf("E6FFCE");
    private Object homesLayer;
    @Override protected void loadTextures(){
        BattlefieldArt.get();
    }
    @Override public void render(){
        if(homesLayer!=getObjectsLayer()){
            homesLayer=getObjectsLayer();getObjectsLayer().unitManager.visuals.onEndCreation();
        }
        BattlefieldArt art=BattlefieldArt.get();
        for(Planet p:getObjectsLayer().planetsManager.planets){
            if(!p.isCurrentlyVisible())continue;
            selection(p);node(p);accessibleMark(p);
            shields(p);economy(p);order(p);
        }
        for(Planet p:getObjectsLayer().planetsManager.planets){
            if(!p.isCurrentlyVisible())continue;
            float r=p.viewPosition.radius;
            art.count(batchMovable,p.text.string,p.viewPosition.center.x,p.viewPosition.center.y,r*.70,r*1.44);
            homeName(p);
        }
        batchMovable.setColor(Color.WHITE);
    }
    public void renderShadows(){/* Contact shadows are part of the managed candy atlas. */}
    private void selection(Planet p){
        double x=p.viewPosition.center.x,y=p.viewPosition.center.y,r=p.viewPosition.radius;
        if(p.highlightEngine.isSelected())arc(x,y,r*1.32,0,Math.PI*2,w*.0017,UiStyle.GOLD,p.highlightEngine.getAlpha());
        if(p.selectionEngineYio.isSelected())arc(x,y,r*1.29,-Math.PI/2,p.halvedSelectionMode?Math.PI/2:Math.PI*1.5,w*.0028,UiStyle.GOLD,1);
        if(p.capturePulse>0)arc(x,y,r*(1.18+.15*(1-p.capturePulse)),0,Math.PI*2,w*.0022,UiStyle.GOLD,p.capturePulse*.65f);
    }
    private void shields(Planet p){
        if(p.isEconomic())return;
        int count=(int)p.currentDefense;if(count==0)return;
        double r=p.viewPosition.radius,x=p.viewPosition.center.x,y=p.viewPosition.center.y,step=Math.PI*2/7;
        for(int i=0;i<count;i++){
            double a,b;
            if(p.isDefensive()){int gap=i/2,slot=i%2;double center=Math.PI/2-gap*step-step/2+(slot==0?.12:-.12);a=center-.072;b=center+.072;}
            else{a=-Math.PI/2+i*step+.20;b=a+.49;}
            arc(x,y,r*1.085,a,b,Math.max(w*.0030,r*.057),BattlefieldArt.SHIELD,1);
            arc(x,y+r*.008,r*1.085,a,b,Math.max(w*.0019,r*.042),BattlefieldArt.SHIELD_LIGHT,1);
        }
    }
    private void economy(Planet p){
        if(!p.isEconomic())return;
        double r=p.viewPosition.radius,x=p.viewPosition.center.x,y=p.viewPosition.center.y,phase=yio.tro.opacha.menu.UiMotion.reduced?0:getObjectsLayer().tactics.elapsed*.40;
        arc(x,y,r*.77,0,Math.PI*2,r*.10,BattlefieldArt.troopEdge(p.fraction),.32f);
        for(int i=0;i<15;i++){
            double a=phase+i*Math.PI*2/15;
            BattlefieldArt.get().smallBox(batchMovable,x+Math.cos(a)*r*.77,y+Math.sin(a)*r*.77,
                    r*.094,r*.044,r*.02,a+Math.PI/2,i%5==0?CONVEYOR_LIGHT:BattlefieldArt.face(p.fraction));
        }
    }
    private void order(Planet p){
        if(p.atFactor.get()==0)return;
        double r=p.viewPosition.radius,a=p.atViewPosition.angle,d=r*1.65;
        UiStyle.get().triangle(batchMovable,p.viewPosition.center.x+Math.cos(a)*d,p.viewPosition.center.y+Math.sin(a)*d,a,r*.25,UiStyle.TEXT,(float)p.atFactor.get());
    }
    private void node(Planet p){
        double now=getObjectsLayer().tactics.elapsed,extension=0,angle=0,r=p.viewPosition.radius;
        if(!yio.tro.opacha.menu.UiMotion.reduced){
            RenderUnits renderer=GameRendersList.getInstance().renderUnits;
            for(Unit unit:getObjectsLayer().unitManager.units){
                if(!unit.isAlive()||unit.value>50||BattlefieldVisuals.working(unit))continue;
                if(unit.getStart()!=p&&unit.getTarget()!=p)continue;
                RenderUnits.Pose pose=renderer.pose(unit);double strength=0,a=pose.angle;
                if(pose.start==p)strength=pose.outgoing;
                else if(pose.target==p){strength=pose.incoming;a+=Math.PI;}
                if(strength*r*.24>extension){extension=strength*r*.24;angle=a;}
            }
        for(BattlefieldVisuals.Contact hit:getObjectsLayer().unitManager.visuals.contacts){
            double age=now-hit.time;if(hit.node!=p||!hit.friendly||age<0||age>.38)continue;
            double amount=r*.24*(1-BattlefieldArt.smooth(age/.38));if(amount>extension){extension=amount;angle=hit.angle;}
        }
        }
        BattlefieldArt.get().node(batchMovable,p,angle,extension);
    }
    private void accessibleMark(Planet p){
        if(!SettingsManager.getInstance().colorblindMode||p.fraction==FractionType.neutral)return;
        // Transparent notches on the same candy surface, never an inset replacement disc.
        int marks=p.fraction.ordinal();double r=p.viewPosition.radius;
        for(int i=0;i<marks;i++){
            double a=.62+(i-(marks-1)/2.0)*.14;
            UiStyle.get().line(batchMovable,p.viewPosition.center.x+Math.cos(a)*r*.63,p.viewPosition.center.y+Math.sin(a)*r*.63,
                    p.viewPosition.center.x+Math.cos(a)*r*.72,p.viewPosition.center.y+Math.sin(a)*r*.72,r*.04,Color.WHITE,.72f);
        }
    }
    private void homeName(Planet p){
        if(gameController.gameMode==GameMode.editor)return;
        AiType type=getObjectsLayer().aiManager.getType(p.fraction);
        if(type==null||getObjectsLayer().unitManager.visuals.homes[p.fraction.ordinal()]!=p)return;
        String name=type==AiType.savvy?"Savvy Jane":type==AiType.silly?"Silly Sam":"Simple Joe";
        BattlefieldArt.get().label(batchMovable,name,p.viewPosition.center.x,p.viewPosition.center.y-p.viewPosition.radius*1.62,
                Math.max(w*.014,p.viewPosition.radius*.29),p.viewPosition.radius*3,UiStyle.TEXT);
    }
    void arc(double x,double y,double r,double from,double to,double width,Color color,float alpha){
        int steps=Math.max(2,(int)((to-from)*12));
        for(int i=0;i<steps;i++){
            double a=from+(to-from)*i/steps,b=from+(to-from)*(i+1)/steps;
            UiStyle.get().line(batchMovable,x+r*Math.cos(a),y+r*Math.sin(a),x+r*Math.cos(b),y+r*Math.sin(b),width,color,alpha);
        }
    }
    @Override protected void disposeTextures(){BattlefieldArt.dispose();}
}
