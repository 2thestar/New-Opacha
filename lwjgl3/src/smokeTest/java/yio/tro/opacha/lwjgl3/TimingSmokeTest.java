package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.*;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;
import java.util.*;

/** Compare the actual world after equal wall time at different display rates. */
public final class TimingSmokeTest extends PlaytestLab {
    private boolean complete;
    private TimingSmokeTest(String out){super(out,0);}
    public static void main(String[] args){
        platformType=PlatformType.pc;TimingSmokeTest game=new TimingSmokeTest(args[0]);
        Lwjgl3ApplicationConfiguration config=Lwjgl3Launcher.configuration(args.length>1?Integer.parseInt(args[1]):390,args.length>2?Integer.parseInt(args[2]):844);
        config.setInitialVisible(false);config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs").path(),Files.FileType.Absolute);
        new Lwjgl3Application(game,config);
        require(game.complete,"Timing check ended early");
    }
    @Override public void render(){
        super.render();if(gameController==null||splashVisible||complete)return;
        Scenes.campaignBrief.destroy();VelocityManager.getInstance().value=1;
        checkCameraFit();
        SimulationClock clock=new SimulationClock();
        require(clock.advance(30)==6,"A long stall must have bounded catch-up");
        clock.reset();require(clock.advance(Double.NaN)==0&&clock.advance(-1)==0,"Invalid delta");
        for(int rate:new int[]{15,30,60,90,120,144}){
            clock.reset();int ticks=0;for(int i=0;i<rate*12;i++)ticks+=clock.advance(1d/rate);
            require(ticks==720,"Clock drift at "+rate+": "+ticks);
        }
        StringBuilder csv=new StringBuilder("speed,fps,game_seconds,economy,garrison,shield,route_work,couriers,node_bombs,reload,repeat_events\n");
        for(int speed:new int[]{1,3}){
            String baseline=null;
            for(int rate:new int[]{60,15,30,90,120,144}){
                String state=run(rate,speed);csv.append(speed).append(',').append(rate).append(',').append(state).append('\n');
                if(baseline==null)baseline=state;else require(baseline.equals(state),"World drift at "+rate+" FPS / "+speed+"x:\n"+baseline+"\n"+state);
            }
        }
        output.child("timing.csv").writeString(csv.toString(),false,"UTF-8");
        System.out.println("TIMING PASSED: identical world at 15/30/60/90/120/144 FPS, 1x/3x; pause and bounded resume");
        complete=true;exitApp();
    }
    private String run(int rate,int speed){
        random.setSeed(816);predictableRandom.setSeed(937);
        RefreshRateDetector.getInstance().multiplier=60f/rate;
        GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;
        gameController.createGameObjects();gameController.initLevelSize(LevelSize.tiny);
        ObjectsLayer l=gameController.objectsLayer;
        Planet depot=node(.15,.35,FractionType.green,150,PlanetType.economic);
        Planet fort=node(.75,.35,FractionType.green,60,PlanetType.defensive);
        Planet gun=node(.15,1.1,FractionType.green,80,PlanetType.artillery);
        Planet target=node(.75,1.1,FractionType.red,200,PlanetType.empty);
        Link supply=l.linksManager.addLink(depot,fort);supply.directional=true;
        Link dig=l.linksManager.addLink(fort,gun);dig.builder=FractionType.green;dig.constructionCost=80;dig.constructionSpent=10;
        l.linksManager.addLink(gun,target);
        gameController.onEndCreation();l.aiManager.entities.clear();l.linksManager.syncFractions();
        require(l.tactics.buyBomb(depot)&&l.tactics.sendBomb(depot,fort),"Courier setup");
        require(l.tactics.fire(gun,target),"Artillery setup");
        l.unitManager.launchUnit(fort,gun,16);
        l.unitManager.launchUnit(depot,fort,12);
        l.unitManager.launchUnit(fort,depot,8);
        gameController.speedManager.setSpeed(speed);
        final int[] events={0};RepeatYio<int[]> repeat=new RepeatYio<int[]>(events,6,2){public void performAction(){parent[0]++;}};
        for(int frame=0;frame<rate*12;frame++){
            RefreshRateDetector.getInstance().multiplier=60f/rate;
            l.moveFrame(1d/rate);repeat.move();
        }
        double elapsed=l.tactics.elapsed,pop=depot.unitsInside;
        gameController.speedManager.setSpeed(0);
        for(int frame=0;frame<rate*2;frame++)l.moveFrame(1d/rate);
        require(l.tactics.elapsed==elapsed&&depot.unitsInside==pop,"Pause changed the world");
        gameController.speedManager.setSpeed(speed);
        l.moveFrame(1d/120);require(l.tactics.elapsed==elapsed,"Pause left a partial simulation tick");
        l.resetClock();
        return String.format(Locale.ROOT,"%.6f,%.6f,%.6f,%.6f,%d,%d,%d,%.6f,%d",elapsed,depot.unitsInside,fort.unitsInside,fort.currentDefense,dig.constructionSpent,l.tactics.couriers.size(),fort.nodeBombs,gun.artilleryReload,events[0]);
    }
    private Planet node(double x,double y,FractionType faction,int count,PlanetType type){
        Planet p=gameController.objectsLayer.planetsManager.spawnPlanet(new CircleYio(x*w,y*w,w*.05),faction);
        p.setType(type);p.setUnitsInside(count);p.setCurrentDefense(0);return p;
    }
    private void checkCameraFit(){
        for(LevelSize size:LevelSize.values()){
            gameController.defaultValues();gameController.createGameObjects();gameController.initLevelSize(size);gameController.createCamera();
            for(int i=0;i<4;i++){
                float x=gameController.boundWidth*(i%2==0?.06f:.94f),y=gameController.boundHeight*(i<2?.06f:.94f);
                gameController.objectsLayer.planetsManager.spawnPlanet(new CircleYio(x,y,w*.035),i<2?FractionType.green:FractionType.red);
            }
            gameController.onEndCreation();
            com.badlogic.gdx.graphics.OrthographicCamera cam=gameController.cameraController.orthoCam;
            for(Planet p:gameController.objectsLayer.planetsManager.planets){
                double x=(p.position.center.x-cam.position.x)/cam.zoom+w/2d,y=(p.position.center.y-cam.position.y)/cam.zoom+h/2d,r=1.6*p.position.radius/cam.zoom;
                require(x-r>=.025*w&&x+r<=.975*w,"Camera clips horizontal edge at "+size);
                require(y-r>=.075*h&&y+r<=.815*h,"Camera puts a node under controls at "+size+": "+(y+r)/h);
            }
        }
        System.out.println("CAMERA FIT PASSED: every map size, including top and bottom edge nodes");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
