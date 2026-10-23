package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.view.game_renders.GameRendersList;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.CircleYio;
import java.util.*;

/** No frame readback or PNG compression in measured frames. Identical fixed workloads. */
public final class FramePerformanceTest extends PlaytestLab {
    private GLProfiler profiler;
    private int frames, scenario=-1, sample;
    private boolean complete;
    private final double[] costs=new double[120];
    private final StringBuilder csv=new StringBuilder("scene,frames,fresh_frames,mean_ms,p50_ms,p95_ms,max_ms,draw_calls,texture_binds,vertices\n");
    private long fresh, draws, binds, vertices;
    private long sampleAfter;
    private static final String[] NAMES={"idle","contacts","stress"};
    private FramePerformanceTest(String path){super(path,0);}
    public static void main(String[] args){
        platformType=PlatformType.pc;
        FramePerformanceTest game=new FramePerformanceTest(args[0]);
        Lwjgl3ApplicationConfiguration config=Lwjgl3Launcher.configuration(390,844);
        config.useVsync(false);config.setForegroundFPS(Integer.getInteger("profileFps",0));config.setIdleFPS(0);
        config.setInitialVisible(false);config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs").path(),Files.FileType.Absolute);
        new Lwjgl3Application(game,config);
        if(!game.complete)throw new AssertionError("Frame profiling ended early");
    }
    @Override public void render(){
        long beforeRender=gameController==null?0:GameRendersList.getInstance().renderTactics.getRouteRenderIndex();
        if(profiler!=null)profiler.reset();
        long start=System.nanoTime();super.render();double ms=(System.nanoTime()-start)/1e6;
        if(gameController==null||splashVisible||complete)return;
        if(Scenes.catchedException.isCurrentlyVisible())throw new AssertionError("Exception during frame profiling");
        if(scenario<0){if(++frames<90)return;Scenes.campaignBrief.destroy();profiler=new GLProfiler(Gdx.graphics);profiler.enable();scenario=0;fixture();return;}
        if(++frames<60||System.nanoTime()<sampleAfter)return;
        costs[sample++]=ms;
        if(GameRendersList.getInstance().renderTactics.getRouteRenderIndex()!=beforeRender)fresh++;
        draws+=profiler.getDrawCalls();binds+=profiler.getTextureBindings();vertices+=(long)profiler.getVertexCount().total;
        if(sample<costs.length)return;
        if(Boolean.getBoolean("verifyProfile")){
            if(fresh!=sample)throw new AssertionError("Repeated world frames in "+NAMES[scenario]+": "+fresh+"/"+sample);
            if(draws/(double)sample>new int[]{100,250,900}[scenario])throw new AssertionError("Batching regression in "+NAMES[scenario]);
        }
        double sum=0;for(double cost:costs)sum+=cost;Arrays.sort(costs);
        String row=String.format(Locale.ROOT,"%s,%d,%d,%.3f,%.3f,%.3f,%.3f,%.1f,%.1f,%.1f%n",NAMES[scenario],sample,fresh,sum/sample,costs[sample/2],costs[(int)(sample*.95)],costs[sample-1],draws/(double)sample,binds/(double)sample,vertices/(double)sample);
        csv.append(row);System.out.print(row);output.child("frames.csv").writeString(csv.toString(),false,"UTF-8");
        if(++scenario<NAMES.length){fixture();return;}
        profiler.disable();complete=true;exitApp();
    }
    private void fixture(){
        frames=sample=0;fresh=draws=binds=vertices=0;
        // Settle entry/menu animations by real time, including in uncapped runs.
        sampleAfter=System.nanoTime()+1_000_000_000L;
        SettingsManager.getInstance().soundEnabled=false;SettingsManager.getInstance().colorblindMode=false;
        UiStyle.applyTheme(false);yio.tro.opacha.menu.UiMotion.reduced=false;
        GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;
        gameController.createGameObjects();gameController.initLevelSize(LevelSize.tiny);
        ObjectsLayer layer=gameController.objectsLayer;
        int rows=scenario==2?6:3,cols=scenario==2?8:4;
        Planet[][] nodes=new Planet[rows][cols];
        for(int row=0;row<rows;row++)for(int col=0;col<cols;col++){
            Planet p=layer.planetsManager.spawnPlanet(new CircleYio((.10+.80*col/(cols-1))*w,(.25+1.25*row/(rows-1))*w,w*(scenario==2?.033:.052)),FractionType.values()[1+row%4]);
            p.setUnitsInside(48+col*3);p.setType(col%3==0?PlanetType.economic:col%3==1?PlanetType.defensive:PlanetType.empty);p.setCurrentDefense(p.isDefensive()?14:3);nodes[row][col]=p;
            if(col>0)layer.linksManager.addLink(nodes[row][col-1],p);
            if(row>0)layer.linksManager.addLink(nodes[row-1][col],p);
        }
        gameController.onEndCreation();layer.aiManager.entities.clear();layer.linksManager.syncFractions();
        if(scenario>0)for(int i=0;i<(scenario==2?240:48);i++){
            int row=i%rows,col=(i/rows)%(cols-1);Planet a=nodes[row][col],b=nodes[row][col+1];
            layer.unitManager.launchUnit(a,b,i%9==0?70:12);
            Unit unit=layer.unitManager.units.get(layer.unitManager.units.size()-1);
            unit.acceleratableFactor.value=i%3==0?.002f:i%3==1?.998f:.48f;unit.moveVisually();
        }
        setGamePaused(false);gameController.speedManager.setSpeed(0);Scenes.speedControls.onSpeedStateChanged();gameView.forceAppear();
    }
}
