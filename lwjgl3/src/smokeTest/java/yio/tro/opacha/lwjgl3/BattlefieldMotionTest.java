package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.view.game_renders.GameRendersList;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.CircleYio;

/** Records actual simulation and native rendering, rather than the HTML design fixture. */
public final class BattlefieldMotionTest extends PlaytestLab {
    private final boolean night,accessible;
    private int warmup,frame,saved;
    private int preRoll;
    private boolean prepared,started,complete,sawIdle,sawRestart,sawFriendly,sawEnemy;
    private long previousRender=-1;
    private final java.util.concurrent.ExecutorService writer=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final java.util.concurrent.atomic.AtomicReference<Throwable> writerFailure=new java.util.concurrent.atomic.AtomicReference<>();
    private double initialTime,previousFront,previousPhase,previousIncoming;
    private int previousCrew,workingPlateau,incomingPlateau;
    private boolean previousApproaching;
    private Unit friendly,enemy,incoming;
    private Link dig;
    private final StringBuilder samples=new StringBuilder("frame,time,front,model_front,phase,crew,incoming,incoming_distance,friendly,enemy\n");
    private BattlefieldMotionTest(String out,boolean night,boolean accessible){super(out,0);this.night=night;this.accessible=accessible;}
    public static void main(String[] args){
        platformType=PlatformType.pc;
        BattlefieldMotionTest game=new BattlefieldMotionTest(args[0],args.length>1&&args[1].equals("night"),args.length>2&&args[2].equals("colorblind"));
        Lwjgl3ApplicationConfiguration config=Lwjgl3Launcher.configuration(390,844);
        config.setInitialVisible(false);config.disableAudio(true);config.setPreferencesConfig(game.output.child("prefs").path(),Files.FileType.Absolute);
        try{new Lwjgl3Application(game,config);}finally{game.writer.shutdown();try{require(game.writer.awaitTermination(30,java.util.concurrent.TimeUnit.SECONDS),"PNG writer timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}}
        require(game.writerFailure.get()==null,"PNG export failed: "+game.writerFailure.get());require(game.complete,"Motion capture ended early");
        System.out.println("NATIVE MOTION PASSED: "+game.saved+" frames; actual contacts, idle crew, restart and forward progress");
    }
    @Override public void render(){
        super.render();if(gameController==null||splashVisible||complete)return;
        if(!started){
            if(!prepared){if(++warmup==45){Scenes.campaignBrief.destroy();return;}if(warmup<45||briefStillDrawn())return;fixture();prepared=true;gameController.speedManager.setSpeed(0);return;}
            if(++preRoll<30)return;
            initialTime=gameController.objectsLayer.tactics.elapsed;gameController.speedManager.setSpeed(1);Scenes.speedControls.onSpeedStateChanged();started=true;return;
        }
        ObjectsLayer layer=gameController.objectsLayer;
        // Every displayed frame must contain a fresh world, including under load.
        long rendered=GameRendersList.getInstance().renderTactics.getRouteRenderIndex();
        boolean fresh=rendered!=previousRender;previousRender=rendered;
        require(fresh,"Displayed a stale world frame");
        double front=GameRendersList.getInstance().renderTactics.getRigVisualFront(dig),phase=GameRendersList.getInstance().renderTactics.getRigVisualPhase(dig);int crew=BattlefieldVisuals.crew(dig);
        if(fresh){
        require(front+1e-6>=previousFront,"Drill moved backward");
        if(dig.isBlueprint()&&crew==0){sawIdle=true;if(previousCrew==0){require(Math.abs(front-previousFront)<1e-9,"Drill advanced without crew");require(Math.abs(phase-previousPhase)<1e-9,"Drum turned without crew");}}
        if(dig.isBlueprint()&&crew>0&&previousCrew>0){workingPlateau=front-previousFront<1e-7?workingPlateau+1:0;require(workingPlateau<2,"Visible working rig stalled at frame "+frame+" time "+(layer.tactics.elapsed-initialTime)+" front "+front+" previous "+previousFront+" raw "+BattlefieldVisuals.front(dig)+" crew "+crew);}
        boolean approaching=incoming.isAlive()&&!BattlefieldVisuals.working(incoming);
        double incomingDistance=approaching?GameRendersList.getInstance().renderUnits.getVisualRouteDistance(incoming):0;
        if(approaching&&previousApproaching){incomingPlateau=incomingDistance-previousIncoming<1e-5?incomingPlateau+1:0;require(incomingPlateau<2,"Incoming troop stopped before actual contact");}
        if(sawIdle&&crew>0)sawRestart=true;
        previousFront=front;previousPhase=phase;previousCrew=crew;previousIncoming=incomingDistance;previousApproaching=approaching;
        for(BattlefieldVisuals.Contact contact:layer.unitManager.visuals.contacts){if(contact.friendly)sawFriendly=true;else sawEnemy=true;}
        samples.append(frame).append(',').append(layer.tactics.elapsed-initialTime).append(',').append(front).append(',').append(BattlefieldVisuals.front(dig)).append(',').append(phase).append(',').append(crew).append(',').append(incoming.isAlive()?incoming.value:0).append(',').append(incomingDistance).append(',').append(friendly.isAlive()?friendly.value:0).append(',').append(enemy.isAlive()?enemy.value:0).append('\n');
        }
        if(frame%3==0){
            Pixmap pixels=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            com.badlogic.gdx.files.FileHandle file=output.child(String.format(java.util.Locale.ROOT,"frames/%04d.png",saved++));
            writer.execute(()->{try{PixmapIO.writePNG(file,pixels,-1,true);}catch(Throwable e){writerFailure.set(e);}finally{pixels.dispose();}});
        }
        if(++frame>=480){
            output.child("simulation.csv").writeString(samples.toString(),false,"UTF-8");
            require(sawIdle&&sawRestart,"Rig idle/restart was not observed");require(sawFriendly&&sawEnemy,"Real contacts were not observed");
            require(!friendly.isAlive()&&!enemy.isAlive(),"Troops did not arrive");complete=true;exitApp();
        }
    }
    private boolean briefStillDrawn(){
        for(yio.tro.opacha.menu.elements.InterfaceElement element:Scenes.campaignBrief.getLocalElementsList())
            if(element.getFactor().get()>0)return true;
        return false;
    }
    private Planet node(double x,double y,FractionType f,int count){
        Planet p=gameController.objectsLayer.planetsManager.spawnPlanet(new CircleYio(x*w,y*w,w*.065),f);p.setType(PlanetType.empty);p.setUnitsInside(count);p.setCurrentDefense(0);return p;
    }
    private Unit send(Planet a,Planet b,int count){ObjectsLayer l=gameController.objectsLayer;l.unitManager.launchUnit(a,b,count);return l.unitManager.units.get(l.unitManager.units.size()-1);}
    private void fixture(){
        UiStyle.applyTheme(night);SettingsManager.getInstance().colorblindMode=accessible;
        GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;
        gameController.createGameObjects();gameController.initLevelSize(LevelSize.tiny);ObjectsLayer l=gameController.objectsLayer;
        Planet a=node(.20,1.30,FractionType.cyan,48),b=node(.80,1.30,FractionType.cyan,18);
        Planet c=node(.20,.88,FractionType.green,48),d=node(.80,.88,FractionType.red,36);
        Planet e=node(.20,.46,FractionType.green,40),f=node(.80,.46,FractionType.green,18);
        l.linksManager.addLink(a,b);l.linksManager.addLink(c,d);dig=l.linksManager.addLink(e,f);dig.builder=FractionType.green;dig.constructionCost=24;dig.constructionSpent=8;
        gameController.onEndCreation();l.aiManager.entities.clear();l.linksManager.syncFractions();
        friendly=send(a,b,12);enemy=send(c,d,12);
        Unit worker=send(e,f,4);worker.acceleratableFactor.value=dig.constructionProgress();worker.moveActually();incoming=send(e,f,12);
        previousFront=BattlefieldVisuals.front(dig);initialTime=l.tactics.elapsed;
        setGamePaused(false);gameController.speedManager.setSpeed(1);Scenes.speedControls.onSpeedStateChanged();
    }
    private static void require(boolean state,String message){if(!state)throw new AssertionError(message);}
}
