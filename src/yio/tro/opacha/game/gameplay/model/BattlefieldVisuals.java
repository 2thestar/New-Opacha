package yio.tro.opacha.game.gameplay.model;

import yio.tro.opacha.game.gameplay.ObjectsLayer;
import java.util.ArrayList;

/** Presentation events only. Never changes troop counts, movement, combat or save data. */
public final class BattlefieldVisuals {
    public static final class Contact {
        public final Planet node;
        public final FractionType faction;
        public final double angle, time;
        public final boolean friendly;
        Contact(Planet node, FractionType faction, double angle, double time, boolean friendly) {
            this.node=node;this.faction=faction;this.angle=angle;this.time=time;this.friendly=friendly;
        }
    }
    public static final class Shot {
        public final Planet source,target;
        public final double time,angle;
        Shot(Planet source,Planet target,double time) {
            this.source=source;this.target=target;this.time=time;
            angle=source.position.center.angleTo(target.position.center);
        }
    }
    public final ArrayList<Contact> contacts=new ArrayList<>();
    public final ArrayList<Shot> shots=new ArrayList<>();
    public final Planet[] homes=new Planet[FractionType.values().length];
    private final ObjectsLayer layer;
    public BattlefieldVisuals(ObjectsLayer layer){this.layer=layer;}
    public void clear(){contacts.clear();shots.clear();java.util.Arrays.fill(homes,null);}
    public void onEndCreation(){
        for(Planet node:layer.planetsManager.planets)
            if(!node.isNeutral()&&homes[node.fraction.ordinal()]==null)homes[node.fraction.ordinal()]=node;
    }
    public void contact(Unit unit,Planet target){
        double angle=unit.link.getOppositePlanet(target).position.center.angleTo(target.position.center)+Math.PI;
        if(contacts.size()>=32)contacts.remove(0);
        contacts.add(new Contact(target,unit.fraction,angle,layer.tactics.elapsed,
                layer.tactics.alliances.friendly(unit.fraction,target.fraction)));
    }
    public void shot(Planet source,Planet target){
        if(shots.size()>=16)shots.remove(0);
        shots.add(new Shot(source,target,layer.tactics.elapsed));
    }
    public void prune(){
        double now=layer.tactics.elapsed;
        for(int i=contacts.size()-1;i>=0;i--)if(now-contacts.get(i).time>1)contacts.remove(i);
        for(int i=shots.size()-1;i>=0;i--)if(now-shots.get(i).time>1)shots.remove(i);
    }
    public static boolean working(Unit unit){
        return unit.isAlive()&&unit.digger&&unit.link.isBlueprint()&&unit.workingDigger;
    }
    public static int crew(Link link){
        int count=0;for(Unit unit:link.upWalkers)if(working(unit))count+=unit.value;return count;
    }
    public static double front(Link link){
        double partial=0;for(Unit unit:link.upWalkers)if(working(unit))partial+=unit.digFraction();
        return Math.min(1,(link.constructionSpent+partial)/Math.max(1,link.constructionCost));
    }
}
