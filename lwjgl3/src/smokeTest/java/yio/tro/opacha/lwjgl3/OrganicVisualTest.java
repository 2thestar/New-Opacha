package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector3;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.UiMotion;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;

/** Native frames and real firing/conversion, with rendered pause/reduced-motion checks. */
public final class OrganicVisualTest extends PlaytestLab {
    private final boolean night;
    private boolean prepared,complete,fired;
    private int warmup,frame,stage,saved,shotCaptures;
    private double started,pauseTime,shotTime;
    private Planet factory,gun,target;
    private long pauseHash,reducedHash;
    private final java.util.concurrent.ExecutorService writer=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final java.util.concurrent.atomic.AtomicReference<Throwable> writerFailure=new java.util.concurrent.atomic.AtomicReference<>();
    private final long deadline=System.nanoTime()+110_000_000_000L;
    private final StringBuilder trace=new StringBuilder("frame,time,factory_troops,gun_troops,target_troops,shots,mobile_bombs\n");
    private OrganicVisualTest(String path,boolean night){super(path,0);this.night=night;}
    public static void main(String[] args){
        platformType=PlatformType.pc;
        OrganicVisualTest game=new OrganicVisualTest(args[2],args.length>3&&args[3].equals("night"));
        Lwjgl3ApplicationConfiguration config=Lwjgl3Launcher.configuration(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
        config.setInitialVisible(false);config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs").path(),Files.FileType.Absolute);
        try{new Lwjgl3Application(game,config);}finally{
            game.writer.shutdown();
            try{require(game.writer.awaitTermination(30,java.util.concurrent.TimeUnit.SECONDS),"PNG writer timed out");}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}
        }
        require(game.writerFailure.get()==null,"PNG export failed: "+game.writerFailure.get());
        require(game.complete,"Organic capture ended early");
        System.out.println("ORGANIC VISUALS PASSED "+args[0]+"x"+args[1]+" "+(game.night?"Night":"Day")+": real shot, conversion, pause and reduced motion");
    }
    @Override public void render(){
        super.render();if(gameController==null||splashVisible||complete)return;
        require(System.nanoTime()<deadline,"Organic capture timed out");
        require(!Scenes.catchedException.isCurrentlyVisible(),"Game exception during organic capture");
        if(!prepared){
            if(++warmup==45)Scenes.campaignBrief.destroy();
            if(warmup<60||briefVisible())return;
            fixture();prepared=true;return;
        }
        ObjectsLayer layer=gameController.objectsLayer;
        double age=layer.tactics.elapsed-started;
        if(stage==0){
            if(!fired&&age>=.55){
                float before=gun.unitsInside,defenders=target.unitsInside;
                require(layer.tactics.fire(gun,target),"Tentacle could not fire");
                require(gun.unitsInside==before-TacticsManager.SHOT_PRICE,"Shot price changed");
                require(target.unitsInside<defenders,"Shot caused no damage");
                fired=true;shotTime=layer.tactics.elapsed;layer.tactics.cancel();
            }
            if(frame%3==0)captureFrame();
            if(frame==30)capture("01-organic-idle");
            double shotAge=layer.tactics.elapsed-shotTime;
            if(fired&&shotCaptures==0&&shotAge>=.04){capture("02-soft-opening");shotCaptures++;}
            if(fired&&shotCaptures==1&&shotAge>=.16){capture("03-black-ball");shotCaptures++;}
            if(fired&&shotCaptures==2&&shotAge>=.40){capture("04-target-burst");shotCaptures++;}
            trace.append(frame).append(',').append(age).append(',').append(factory.unitsInside).append(',')
                    .append(gun.unitsInside).append(',').append(target.unitsInside).append(',')
                    .append(layer.unitManager.visuals.shots.size()).append(',').append(layer.tactics.mobileBombs.size()).append('\n');
            frame++;
            if(age<4.0)return;
            require(fired,"No actual shot was observed");
            setGamePaused(true);pauseTime=layer.tactics.elapsed;frame=0;stage=1;return;
        }
        if(stage==1){
            require(layer.tactics.elapsed==pauseTime,"Paused world time advanced");
            if(++frame==35){pauseHash=factoryHash();capture("05-paused");}
            if(frame<50)return;
            require(factoryHash()==pauseHash,"Factory continued to ripple while paused");
            UiMotion.reduced=true;frame=0;stage=2;return;
        }
        if(stage==2){
            if(++frame==3){reducedHash=factoryHash();capture("06-reduced-motion");layer.tactics.elapsed+=.65;}
            if(frame<6)return;
            require(factoryHash()==reducedHash,"Factory oscillated with reduced motion enabled");
            SettingsManager.getInstance().colorblindMode=true;frame=0;stage=3;return;
        }
        if(stage==3&&++frame==3){
            capture("07-colorblind-reduced");
            output.child("simulation.csv").writeString(trace.toString(),false,"UTF-8");
            complete=true;exitApp();
        }
    }
    private void captureFrame(){
        writeCapture(output.child(String.format(java.util.Locale.ROOT,"frames/%04d.png",saved++)));
    }
    @Override protected void capture(String name){writeCapture(output.child(name+".png"));}
    private void writeCapture(com.badlogic.gdx.files.FileHandle file){
        Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        writer.execute(()->{try{PixmapIO.writePNG(file,p,-1,true);}catch(Throwable e){writerFailure.set(e);}finally{p.dispose();}});
    }
    private long factoryHash(){
        Vector3 center=new Vector3(factory.viewPosition.center.x,factory.viewPosition.center.y,0);
        Vector3 edge=new Vector3(factory.viewPosition.center.x+factory.viewPosition.radius*1.5f,factory.viewPosition.center.y,0);
        gameController.cameraController.orthoCam.project(center);gameController.cameraController.orthoCam.project(edge);
        int radius=(int)Math.ceil(Math.abs(edge.x-center.x));
        int x=Math.max(0,(int)center.x-radius),y=Math.max(0,(int)center.y-radius);
        int width=Math.min(radius*2,Gdx.graphics.getBackBufferWidth()-x),height=Math.min(radius*2,Gdx.graphics.getBackBufferHeight()-y);
        Pixmap pixels=Pixmap.createFromFrameBuffer(x,y,width,height);
        long hash=0xcbf29ce484222325L;
        try{for(int row=0;row<height;row++)for(int col=0;col<width;col++)hash=(hash^pixels.getPixel(col,row))*0x100000001b3L;}
        finally{pixels.dispose();}
        return hash;
    }
    private boolean briefVisible(){
        for(yio.tro.opacha.menu.elements.InterfaceElement e:Scenes.campaignBrief.getLocalElementsList())if(e.getFactor().get()>0)return true;
        return false;
    }
    private Planet node(double x,double y,FractionType faction,int count,PlanetType type){
        Planet p=gameController.objectsLayer.planetsManager.spawnPlanet(new CircleYio(x*w,y*w,w*.062),faction);
        p.setType(type);p.setUnitsInside(count);p.setCurrentDefense(0);return p;
    }
    private void fixture(){
        UiStyle.applyTheme(night);UiMotion.reduced=false;
        SettingsManager.getInstance().colorblindMode=false;
        GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;
        gameController.createGameObjects();gameController.initLevelSize(LevelSize.tiny);
        ObjectsLayer l=gameController.objectsLayer;
        factory=node(.20,1.42,FractionType.green,80,PlanetType.economic);
        Planet second=node(.50,1.42,FractionType.green,120,PlanetType.economic);
        Planet third=node(.80,1.42,FractionType.yellow,42,PlanetType.economic);
        gun=node(.20,.92,FractionType.green,80,PlanetType.artillery);
        target=node(.80,.92,FractionType.red,48,PlanetType.empty);
        Planet left=node(.20,.42,FractionType.green,32,PlanetType.empty);
        Planet right=node(.80,.42,FractionType.green,18,PlanetType.empty);
        l.linksManager.addLink(factory,second);l.linksManager.addLink(second,third);
        l.linksManager.addLink(gun,target);Link lower=l.linksManager.addLink(left,right);
        l.linksManager.addLink(factory,gun);l.linksManager.addLink(target,right);
        gameController.onEndCreation();l.aiManager.entities.clear();l.linksManager.syncFractions();
        l.unitManager.launchUnit(second,third,60);
        Unit army=l.unitManager.units.get(l.unitManager.units.size()-1);army.acceleratableFactor.value=.35f;army.moveVisually();
        require(l.tactics.convertToBomb(army)!=null,"Factory army did not convert to a bomb");
        l.tactics.bombs.add(new RouteBomb(lower,FractionType.green,.5f));left.nodeBombs=1;
        l.tactics.mobileBombs.add(new MobileBomb(FractionType.green,right));
        l.tactics.begin(TacticsManager.Action.artillery,gun);
        setGamePaused(false);gameController.speedManager.setSpeed(1);Scenes.speedControls.onSpeedStateChanged();
        gameView.forceAppear();started=l.tactics.elapsed;
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
