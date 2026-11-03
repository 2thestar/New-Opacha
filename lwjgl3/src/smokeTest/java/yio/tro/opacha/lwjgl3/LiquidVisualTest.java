package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Vector3;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.UiMotion;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.CircleYio;

/** Deterministic native poses: real unit steps and transfers; PNG export never sets animation time. */
public final class LiquidVisualTest extends PlaytestLab {
    private final boolean night,accessible;
    private boolean prepared,complete,launched,pausedOnce;
    private int warmup,frame,stage,pauseFrame,pose;
    private Planet[] from,to;
    private Unit[] troops;
    private double contactAt=-1,pauseTime;
    private long pauseHash,reducedHash;
    private final java.util.concurrent.ThreadPoolExecutor writer=new java.util.concurrent.ThreadPoolExecutor(1,1,0,
            java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<Runnable>(3),
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    private final java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
    private final StringBuilder trace=new StringBuilder("frame,time,cyan_progress,cyan_alive,cyan_target,yellow_target,red_target,contacts\n");
    private final long deadline=System.nanoTime()+100_000_000_000L;
    private LiquidVisualTest(String out,boolean night,boolean accessible){super(out,0);this.night=night;this.accessible=accessible;}
    public static void main(String[] args){
        platformType=PlatformType.pc;
        LiquidVisualTest game=new LiquidVisualTest(args[2],args.length>3&&args[3].equals("night"),args.length>4&&args[4].equals("colorblind"));
        Lwjgl3ApplicationConfiguration config=Lwjgl3Launcher.configuration(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
        config.setInitialVisible(false);config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs").path(),Files.FileType.Absolute);
        try{new Lwjgl3Application(game,config);}finally{
            game.writer.shutdown();try{check(game.writer.awaitTermination(30,java.util.concurrent.TimeUnit.SECONDS),"PNG export timeout");}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}
        }
        check(game.failure.get()==null,"PNG export failure: "+game.failure.get());check(game.complete,"Liquid check ended early");
        System.out.println("LIQUID VISUALS PASSED "+args[0]+"x"+args[1]+": transfers once, friendly absorption, hostile splash, pause, reduced motion, managed texture recovery; "+game.frame+" sampled frames");
    }
    @Override public void render(){
        super.render();if(gameController==null||splashVisible||complete)return;
        check(System.nanoTime()<deadline,"Liquid test timed out at "+stage);
        check(!Scenes.catchedException.isCurrentlyVisible(),"Game exception during liquid test");
        if(!prepared){
            if(++warmup==45)Scenes.campaignBrief.destroy();
            if(warmup<65||gameView.isInMotion())return;
            fixture(false);prepared=true;warmup=0;return;
        }
        ObjectsLayer l=gameController.objectsLayer;
        if(!launched){if(++warmup<35)return;launch();launched=true;return;}
        if(stage==0){
            double progress=troops[0].acceleratableFactor.value;
            if(!pausedOnce&&progress>=.94){pausedOnce=true;stage=1;pauseFrame=0;pauseTime=l.tactics.elapsed;setGamePaused(true);return;}
            saveFrame();
            if(pose==0){capture("00-bulge");pose++;}
            if(pose==1&&progress>=.035){capture("01-pinch");pose++;}
            if(pose==2&&progress>=.14){capture("02-separated");pose++;}
            if(pose==3&&progress>=.89){capture("03-approach");pose++;}
            if(pose==4&&progress>=.98){capture("04-connected");pose++;}
            if(!troops[0].isAlive()){
                if(contactAt<0){
                    contactAt=l.tactics.elapsed;capture("05-transfer");
                    check(l.unitManager.visuals.contacts.size()==3,"Expected exactly one contact per group");
                    check(to[0].unitsInside==30&&to[2].unitsInside==50&&to[1].unitsInside==40,"Arrival counts or combat changed");
                    check(l.unitManager.visuals.contacts.get(0).value==12,"Contact failed to snapshot fleet size");
                }
                double age=l.tactics.elapsed-contactAt;
                if(pose==5&&age>=.10){capture("06-absorb-and-splash");pose++;}
                if(age>=.65){
                    capture("07-settled");
                    check(to[0].unitsInside==30&&to[2].unitsInside==50,"Presentation transferred troops twice");
                    output.child("simulation.csv").writeString(trace.toString(),false,"UTF-8");
                    fixture(true);launched=false;warmup=0;stage=2;return;
                }
            }
            // Two ordinary 60 Hz unit steps per recorded 1/30-second pose. No production,
            // AI or repeating orders are stepped in this isolated transfer fixture.
            for(int i=0;i<2;i++){
                RefreshRateDetector.getInstance().multiplier=1;VelocityManager.getInstance().value=1;
                l.tactics.elapsed+=1.0/60;l.unitManager.moveActually();
            }
            return;
        }
        if(stage==1){
            check(l.tactics.elapsed==pauseTime,"Pause changed liquid time");
            if(++pauseFrame==12){pauseHash=cropHash(to[0]);capture("08-paused-contact");}
            if(pauseFrame<25)return;
            check(cropHash(to[0])==pauseHash,"Paused liquid pixels changed");
            setGamePaused(false);stage=0;return;
        }
        if(stage==2){
            if(++pauseFrame<50)return;
            reducedHash=cropHash(from[0]);capture("09-reduced");l.tactics.elapsed+=3;stage=3;pauseFrame=0;return;
        }
        if(stage==3){
            if(++pauseFrame<8)return;
            check(cropHash(from[0])==reducedHash,"Reduced motion still changed the liquid shape");
            Texture.invalidateAllTextures(Gdx.app);stage=4;pauseFrame=0;return;
        }
        if(stage==4&&++pauseFrame>=8){
            check(cropHash(from[0])==reducedHash,"Managed arrow/shape changed after texture restoration");
            capture("10-textures-restored");complete=true;exitApp();
        }
    }
    private void saveFrame(){
        ObjectsLayer l=gameController.objectsLayer;
        trace.append(frame).append(',').append(l.tactics.elapsed).append(',').append(troops[0].acceleratableFactor.value)
                .append(',').append(troops[0].isAlive()).append(',').append(to[0].unitsInside).append(',')
                .append(to[2].unitsInside).append(',').append(to[1].unitsInside).append(',').append(l.unitManager.visuals.contacts.size()).append('\n');
        if(Boolean.getBoolean("liquidFrames")){
            final Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            final com.badlogic.gdx.files.FileHandle file=output.child(String.format(java.util.Locale.ROOT,"frames/%04d.png",frame));
            writer.execute(()->{try{PixmapIO.writePNG(file,p,-1,true);}catch(Throwable e){failure.set(e);}finally{p.dispose();}});
        }
        frame++;
    }
    private long cropHash(Planet node){
        Vector3 p=new Vector3(node.viewPosition.center.x,node.viewPosition.center.y,0);gameController.cameraController.orthoCam.project(p);
        float r=node.viewPosition.radius/gameController.cameraController.orthoCam.zoom;
        int x=Math.max(0,(int)(p.x-r*2.3)),y=Math.max(0,(int)(p.y-r*1.5)),width=Math.min(w-x,(int)(r*4.6)),height=Math.min(h-y,(int)(r*3));
        Pixmap pixels=Pixmap.createFromFrameBuffer(x,y,width,height);long hash=0xcbf29ce484222325L;
        try{for(int row=0;row<height;row++)for(int col=0;col<width;col++)hash=(hash^pixels.getPixel(col,row))*0x100000001b3L;}
        finally{pixels.dispose();}return hash;
    }
    private void fixture(boolean reduced){
        UiStyle.applyTheme(night);UiMotion.reduced=reduced;SettingsManager.getInstance().colorblindMode=accessible;
        GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;
        gameController.gameMode=GameMode.skirmish;gameController.createGameObjects();gameController.initLevelSize(LevelSize.tiny);
        ObjectsLayer l=gameController.objectsLayer;
        from=new Planet[]{node(.20,1.34,FractionType.cyan,24,PlanetType.economic),node(.20,.90,FractionType.green,48,PlanetType.empty),node(.20,.46,FractionType.yellow,64,PlanetType.empty)};
        to=new Planet[]{node(.80,1.34,FractionType.cyan,18,PlanetType.empty),node(.80,.90,FractionType.red,64,PlanetType.empty),node(.80,.46,FractionType.yellow,18,PlanetType.defensive)};
        for(int i=0;i<3;i++)l.linksManager.addLink(from[i],to[i]);
        gameController.onEndCreation();l.aiManager.entities.clear();l.linksManager.syncFractions();
        for(int i=0;i<3;i++){from[i].setAutoTarget(to[i]);to[i].setAutoTarget(from[i]);}
        gameController.speedManager.setSpeed(0);Scenes.speedControls.onSpeedStateChanged();setGamePaused(false);
        gameController.createMenuOverlay();Scenes.quickVelocityTutorial.destroy();gameView.forceAppear();
        menuControllerYio.forceDyingElementsToEnd();pauseFrame=0;
    }
    private void launch(){
        ObjectsLayer l=gameController.objectsLayer;troops=new Unit[3];
        for(int i=0;i<3;i++){
            check(l.planetsManager.sendUnitsPercent(from[i],to[i],50),"Fixture send failed");
            troops[i]=l.unitManager.units.get(l.unitManager.units.size()-1);
        }
    }
    private Planet node(double x,double y,FractionType owner,int count,PlanetType type){
        Planet p=gameController.objectsLayer.planetsManager.spawnPlanet(new CircleYio(x*w,y*w,w*.075),owner);
        p.setType(type);p.setUnitsInside(count);p.setCurrentDefense(0);return p;
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
