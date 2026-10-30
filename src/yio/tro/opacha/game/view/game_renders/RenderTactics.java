package yio.tro.opacha.game.view.game_renders;

import com.badlogic.gdx.graphics.Color;
import yio.tro.opacha.Fonts;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.stuff.*;
import yio.tro.opacha.menu.UiStyle;
import java.util.IdentityHashMap;

public final class RenderTactics extends GameRender {
    private TacticsTextures textures;
    private final RenderableTextYio cost = new RenderableTextYio();
    private final IdentityHashMap<Link,Double> rigPockets=new IdentityHashMap<>();
    private static final class RigMotion {double front,time;int cost;RigMotion(double front,double time,int cost){this.front=front;this.time=time;this.cost=cost;}}
    private final IdentityHashMap<Link,RigMotion> rigMotions=new IdentityHashMap<>();
    private Object pocketLayer;
    private long routeRenderIndex;
    public long getRouteRenderIndex(){return routeRenderIndex;}
    @Override protected void loadTextures() {
        if (textures == null) textures = new TacticsTextures();
        cost.setFont(Fonts.unitFont);
    }
    private void line(double x1, double y1, double x2, double y2, double width) {
        GraphicsYio.drawLine(batchMovable, textures.get("pixel"), x1, y1, x2, y2, width);
    }
    private void tint(FractionType owner, float alpha) {
        switch (owner) {
            case green: batchMovable.setColor(.52f, .87f, .60f, alpha); break;
            case red: batchMovable.setColor(.88f, .51f, .56f, alpha); break;
            case cyan: batchMovable.setColor(.50f, .86f, .88f, alpha); break;
            case yellow: batchMovable.setColor(.87f, .88f, .51f, alpha); break;
            default: batchMovable.setColor(.98f, .84f, .51f, alpha);
        }
    }
    public void renderRoutes() {
        routeRenderIndex++;
        if(pocketLayer!=getObjectsLayer()){pocketLayer=getObjectsLayer();rigPockets.clear();rigMotions.clear();}
        rigPockets.keySet().removeIf(link->!link.isBlueprint());
        rigMotions.keySet().removeIf(link->!link.isBlueprint());
        for (Link link : getObjectsLayer().linksManager.links) {
            if (!link.isBlueprint() || !link.isCurrentlyVisible()) continue;
            double x = link.one.viewPosition.center.x, y = link.one.viewPosition.center.y;
            double dx = Math.cos(link.angle), dy = Math.sin(link.angle);
            if (link.isBlueprint()) {
                updateRigMotion(link);
                double size=w*.029,front=rigDistance(link,size),end=link.distance-link.two.viewPosition.radius*1.14;
                double pocket=updateRigPocket(link,size,front),rear=rigRear(link,size);
                for(double d=front+size*1.55;d<end;d+=w*.012)
                    UiStyle.get().line(batchMovable,x+dx*d,y+dy*d,x+dx*Math.min(end,d+w*.002),y+dy*Math.min(end,d+w*.002),w*.0015,UiStyle.MUTED,.45f);
                double start=link.one.viewPosition.radius*1.14;
                if(front+rear>start)UiStyle.get().line(batchMovable,x+dx*start,y+dy*start,x+dx*(front+rear),y+dy*(front+rear),w*.0018,UiStyle.OUTLINE,.85f);
                int crew=BattlefieldVisuals.crew(link);
                double phase=getRigVisualPhase(link);
                BattlefieldArt.get().rig(batchMovable,x+dx*front,y+dy*front,size,link.angle,crew,yio.tro.opacha.menu.UiMotion.reduced?0:phase*.36,pocket);
                if(crew>0&&!yio.tro.opacha.menu.UiMotion.reduced)for(int i=0;i<3;i++){
                    double age=(phase*.13+i/3.0)%1;
                    BattlefieldArt.get().ellipse(batchMovable,x+dx*(front+size*(1.4+age*.4))-dy*(i-1)*size*.5,
                            y+dy*(front+size*(1.4+age*.4))+dx*(i-1)*size*.5,size*.045,size*.045,0,BattlefieldArt.BUTTER,(float)(.5*Math.sin(Math.PI*age)));
                }
            }
        }
        TacticsManager t = getObjectsLayer().tactics;
        Planet gun = t.pending == TacticsManager.Action.artillery ? t.source : getObjectsLayer().planetsManager.getCurrentlySelectedPlanet();
        if (gun != null && gun.type == PlanetType.artillery) {
            if (UiStyle.isNight()) batchMovable.setColor(UiStyle.GOLD.r, UiStyle.GOLD.g, UiStyle.GOLD.b, .50f);
            else batchMovable.setColor(.50f, .36f, .10f, .38f);
            circle(gun.viewPosition.center.x, gun.viewPosition.center.y, t.artilleryRange(), 1, true);
            if (t.pending == TacticsManager.Action.artillery) for (Planet target : getObjectsLayer().planetsManager.planets) {
                if (t.canFire(gun, target)) circle(target.viewPosition.center.x, target.viewPosition.center.y, 1.4 * target.viewPosition.radius, 1, false);
            }
        }
        batchMovable.setColor(Color.WHITE);
    }

    @Override public void render() {
        TacticsManager t = getObjectsLayer().tactics;
        BattlefieldArt art=BattlefieldArt.get();
        for (RouteBomb bomb : t.bombs) if (bomb.active && bomb.owner == FractionType.green) {
            double x = bomb.link.one.viewPosition.center.x + Math.cos(bomb.link.angle) * bomb.link.distance * bomb.position;
            double y = bomb.link.one.viewPosition.center.y + Math.sin(bomb.link.angle) * bomb.link.distance * bomb.position;
            art.bomb(batchMovable,x,y,w*.022,0,bomb.owner,t.elapsed,null);
        }
        for (BombCourier courier : t.couriers) if (courier.visibleTo(FractionType.green)) {
            PointYio p = courier.position();
            art.bomb(batchMovable,p.x,p.y,w*.022,courier.link.angle,courier.owner,t.elapsed,Integer.toString(TacticsManager.BOMB_CREW));
        }
        for (MobileBomb bomb : t.mobileBombs) if (bomb.visibleTo(FractionType.green)) {
            PointYio p = bomb.position();
            if(bomb.node==null)art.bomb(batchMovable,p.x,p.y,w*.025,bomb.link.angle,bomb.owner,t.elapsed,null);
            else art.bomb(batchMovable,p.x,p.y,
                    bomb.node.viewPosition.radius*.39,0,bomb.owner,t.elapsed,null);
        }
        for (Planet node : getObjectsLayer().planetsManager.planets) {
            if(!node.isCurrentlyVisible())continue;
            float x = node.viewPosition.center.x, y = node.viewPosition.center.y, r = node.viewPosition.radius;
            int visibleTraps = gameController.gameMode == GameMode.editor
                    ? node.nodeBombs : node.visibleNodeBombs(FractionType.green);
            if (visibleTraps > 0) {
                for (int i = 0; i < visibleTraps; i++) {
                    double angle=2.65+i*.58;
                    art.bomb(batchMovable,x+Math.cos(angle)*r*1.50,y+Math.sin(angle)*r*1.50,
                            r*.30,0,node.fraction,t.elapsed,null);
                }
            }
            if (node.type == PlanetType.artillery) {
                cannon(node);
                if (node.artilleryReload > 0) {
                    if (UiStyle.isNight()) batchMovable.setColor(UiStyle.GOLD);
                    else batchMovable.setColor(.58f, .40f, .08f, .9f);
                    GameRendersList.getInstance().renderPlanets.arc(x,y,r*.84,Math.PI*1.1,
                            Math.PI*1.1+Math.PI*.8*(1-node.artilleryReload/TacticsManager.RELOAD_SECONDS),r*.045,UiStyle.GOLD,.85f);
                }
            }
            if (GameRules.playType == PlayType.capture_flag && node == t.flagPlanet) {
                tint(FractionType.neutral, 1); glyph("flag", x + 1.30 * r, y + .90 * r, .60 * r);
            }
            if (GameRules.playType == PlayType.home_flags) for (FractionType owner : FractionType.values()) {
                if (owner != FractionType.neutral && t.homeFlags[owner.ordinal()] == node) {
                    tint(owner, 1); glyph("flag", x + 1.30 * r, y + .90 * r, .60 * r);
                }
            }
        }
        effects();
        batchMovable.setColor(Color.WHITE);
    }
    private void glyph(String key, double x, double y, double size) {
        batchMovable.draw(textures.get(key), (float) (x - size / 2), (float) (y - size / 2), (float) size, (float) size);
    }
    static double rigDistance(Link link,double size){
        double bubble=size*(.020+Math.sqrt(12)*.0012)/.029;
        double start=link.one.viewPosition.radius+bubble*.82,end=link.distance-link.two.viewPosition.radius-bubble*.82;
        // Incoming centers follow ordinary travel all the way to real model contact.
        // The rig sits beyond that point so the bubble reaches its rear intake without stopping.
        return start+Math.max(0,end-start)*GameRendersList.getInstance().renderTactics.getRigVisualFront(link)+size*1.7;
    }
    private void updateRigMotion(Link link){
        double raw=BattlefieldVisuals.front(link),now=getObjectsLayer().tactics.elapsed;
        RigMotion motion=rigMotions.get(link);
        if(motion==null||motion.cost!=link.constructionCost){rigMotions.put(link,new RigMotion(raw,now,link.constructionCost));return;}
        double dt=Math.max(0,now-motion.time);motion.time=now;
        // Resource ticks can make a worker catch up to the new integer front. Smooth only
        // the displayed pose across those intervals, using simulation time so pause holds.
        // At zero retain the exact last displayed position and drum phase.
        if(BattlefieldVisuals.crew(link)>0&&dt>0)motion.front+=(Math.max(motion.front,raw)-motion.front)*(1-Math.exp(-dt/.12));
    }
    public double getRigVisualFront(Link link){RigMotion m=rigMotions.get(link);return m==null?BattlefieldVisuals.front(link):m.front;}
    public double getRigVisualPhase(Link link){return getRigVisualFront(link)*link.constructionCost;}
    /** Keep the inset crew pocket on its route, clear of crossing traffic corridors.
     * Only the cozy hull adapts; the drill front and all simulation positions stay fixed. */
    private double updateRigPocket(Link link,double size,double front){
        double best=0,bestScore=Double.POSITIVE_INFINITY;
        for(int candidate=0;candidate<=4;candidate++){
            double offset=candidate*.6*size;
            double d=front+offset;
            if(d<link.one.viewPosition.radius+size||d>link.distance-link.two.viewPosition.radius-size)continue;
            double x=link.one.viewPosition.center.x+Math.cos(link.angle)*d,y=link.one.viewPosition.center.y+Math.sin(link.angle)*d;
            double score=Math.abs(offset)/size*.025;
            for(Link other:getObjectsLayer().linksManager.links){
                if(other==link||Math.abs(Math.sin(other.angle-link.angle))<.35)continue;
                double ax=other.one.viewPosition.center.x,ay=other.one.viewPosition.center.y;
                double dx=other.two.viewPosition.center.x-ax,dy=other.two.viewPosition.center.y-ay,length2=dx*dx+dy*dy;
                if(length2<1)continue;
                double t=((x-ax)*dx+(y-ay)*dy)/length2;
                if(t<=0||t>=1)continue;
                double distance=Math.hypot(x-ax-t*dx,y-ay-t*dy),overlap=Math.max(0,1-distance/(w*.034));
                score+=overlap*overlap;
            }
            if(score<bestScore){bestScore=score;best=offset;}
        }
        Double previous=rigPockets.get(link);
        double eased=previous==null?best:previous+(best-previous)*(1-Math.exp(-Math.min(.05,com.badlogic.gdx.Gdx.graphics.getDeltaTime())/ .15));
        rigPockets.put(link,eased);return eased;
    }
    double rigRear(Link link,double size){return Math.min(-size,rigPockets.getOrDefault(link,0.0)-size*.78);}
    private void cannon(Planet node){
        TacticsManager t=getObjectsLayer().tactics;
        double angle=t.shotSource==node&&t.shotTarget!=null?node.position.center.angleTo(t.shotTarget.position.center):Math.PI/4;
        double shotAge=-1;
        for(BattlefieldVisuals.Shot shot:getObjectsLayer().unitManager.visuals.shots)if(shot.source==node){
            double age=t.elapsed-shot.time;if(age>=0&&(shotAge<0||age<shotAge))shotAge=age;
        }
        BattlefieldArt.get().tentacle(batchMovable,node,angle,shotAge,t.pending==TacticsManager.Action.artillery&&t.source==node);
    }
    private void effects(){
        BattlefieldArt art=BattlefieldArt.get();double now=getObjectsLayer().tactics.elapsed;
        for(BattlefieldVisuals.Contact hit:getObjectsLayer().unitManager.visuals.contacts)
            if(!hit.friendly)art.puff(batchMovable,hit.node,hit.angle,now-hit.time);
        for(BattlefieldVisuals.Shot shot:getObjectsLayer().unitManager.visuals.shots){
            double age=now-shot.time,dx=Math.cos(shot.angle),dy=Math.sin(shot.angle),r=shot.source.viewPosition.radius;
            double ax=shot.source.viewPosition.center.x+dx*r*1.75,ay=shot.source.viewPosition.center.y+dy*r*1.75;
            double bx=shot.target.viewPosition.center.x-dx*shot.target.viewPosition.radius,by=shot.target.viewPosition.center.y-dy*shot.target.viewPosition.radius;
            double flight=age-BattlefieldArt.SHOT_RELEASE;
            if(flight>=0&&flight<BattlefieldArt.SHOT_FLIGHT&&!yio.tro.opacha.menu.UiMotion.reduced){
                double p=flight/BattlefieldArt.SHOT_FLIGHT;
                art.cannonBall(batchMovable,ax+(bx-ax)*p,ay+(by-ay)*p,Math.max(w*.0055,r*.12));
            }
            art.puff(batchMovable,shot.target,shot.angle+Math.PI,flight-BattlefieldArt.SHOT_FLIGHT);
        }
    }
    private void circle(double x, double y, double radius, double portion, boolean dashed) {
        int steps = 96;
        for (int i = 0; i < steps * portion; i++) {
            if (dashed && i % 3 == 2) continue;
            double a = Math.PI * 2 * i / steps, b = Math.PI * 2 * (i + 1) / steps;
            if (dashed) {
                double px = x + radius * Math.cos(a), py = y + radius * Math.sin(a);
                double qx = x + radius * Math.cos(b), qy = y + radius * Math.sin(b);
                if (px < 0 || qx < 0 || py < 0 || qy < 0 || px > gameController.boundWidth || qx > gameController.boundWidth
                        || py > gameController.boundHeight || qy > gameController.boundHeight) continue;
            }
            line(x + radius * Math.cos(a), y + radius * Math.sin(a), x + radius * Math.cos(b), y + radius * Math.sin(b), .0025 * w);
        }
    }
    @Override protected void disposeTextures() { if (textures != null) { textures.dispose(); textures = null; } }
}
