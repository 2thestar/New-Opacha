package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.CircleYio;

/** Native render coverage plus real arrival, digging, shield and cannon event checks. */
public final class BattlefieldSmokeTest extends PlaytestLab {
    private int frames,stage;
    private boolean complete;
    private Planet a,b,fort,gun,enemy;
    private Unit wave,crew,incoming;
    private Link dig;
    private final int expectedWidth,expectedHeight;
    private final long deadline=System.nanoTime()+90_000_000_000L;
    private BattlefieldSmokeTest(String output,int width,int height){super(output,0);expectedWidth=width;expectedHeight=height;}
    public static void main(String[] args){
        platformType=PlatformType.pc;
        BattlefieldSmokeTest game=new BattlefieldSmokeTest(args[2],Integer.parseInt(args[0]),Integer.parseInt(args[1]));
        Lwjgl3ApplicationConfiguration config=Lwjgl3Launcher.configuration(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
        if(Integer.parseInt(args[1])>1000){config.setDecorated(false);config.setWindowSizeLimits(Integer.parseInt(args[0]),Integer.parseInt(args[1]),Integer.parseInt(args[0]),Integer.parseInt(args[1]));}
        config.setInitialVisible(false);config.disableAudio(true);config.setPreferencesConfig(game.output.child("prefs").path(),Files.FileType.Absolute);
        new Lwjgl3Application(game,config);
        require(game.complete,"Battlefield test ended early");System.out.println("BATTLEFIELD PASSED "+args[0]+"x"+args[1]);
    }
    @Override public void render(){
        super.render();if(gameController==null||splashVisible||complete)return;
        require(System.nanoTime()<deadline,"Battlefield timeout");require(!Scenes.catchedException.isCurrentlyVisible(),"Game exception at stage "+stage);
        if(++frames%25!=0)return;
        ObjectsLayer layer=gameController.objectsLayer;
        switch(stage++){
            case 0:Scenes.campaignBrief.destroy();fixture();break;
            case 1:capture("01-day-battlefield");UiStyle.applyTheme(true);break;
            case 2:capture("02-night-battlefield");fort.setCurrentDefense(3);break;
            case 3:require((int)fort.currentDefense==3,"Shield state");capture("03-three-shields");UiStyle.applyTheme(false);wave.acceleratableFactor.value=0;wave.moveActually();break;
            case 4:capture("04-fluid-departure");wave.acceleratableFactor.value=.99f;wave.moveActually();break;
            case 5:capture("05-fluid-approach");friendlyArrival();break;
            case 6:capture("06-friendly-merge");enemyArrival();break;
            case 7:capture("07-enemy-puff");
                require(layer.tactics.fire(gun,enemy),"Cannon did not fire");
                require(gun.unitsInside==60,"Shot must cost 20");
                layer.tactics.elapsed+=.06;break;
            case 8:capture("08-cannon-muzzle");layer.tactics.elapsed+=.21;break;
            case 9:capture("09-cannon-impact");crew.setAlive(false);break;
            case 10:require(BattlefieldVisuals.crew(dig)==0,"Rig must stop at zero");capture("10-zero-rig");paletteFixture();SettingsManager.getInstance().colorblindMode=true;accessibleAtlas();break;
            case 11:capture("11-colorblind");UiStyle.applyTheme(true);break;
            case 12:capture("12-colorblind-night");yio.tro.opacha.menu.UiMotion.reduced=true;break;
            case 13:capture("13-reduced-motion");complete=true;exitApp();break;
            default:throw new AssertionError("Unexpected stage");
        }
    }
    private Planet node(double x,double y,FractionType faction,int count,PlanetType type){
        Planet p=gameController.objectsLayer.planetsManager.spawnPlanet(new CircleYio(x*w,y*w,w*.045),faction);
        p.setType(type);p.setUnitsInside(count);p.setCurrentDefense(type==PlanetType.defensive?14:0);return p;
    }
    private void fixture(){
        require(Gdx.graphics.getBackBufferWidth()==expectedWidth&&Gdx.graphics.getBackBufferHeight()==expectedHeight,"Native capture size differs from requested viewport");
        GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;
        gameController.createGameObjects();gameController.initLevelSize(LevelSize.tiny);ObjectsLayer layer=gameController.objectsLayer;
        a=node(.20,.40,FractionType.green,48,PlanetType.empty);b=node(.80,.40,FractionType.green,18,PlanetType.empty);
        Planet economy=node(.20,.88,FractionType.cyan,40,PlanetType.economic),tower=node(.80,.88,FractionType.red,36,PlanetType.defensive);
        fort=node(.20,1.36,FractionType.green,14,PlanetType.defensive);gun=node(.50,1.36,FractionType.green,80,PlanetType.artillery);
        enemy=node(.80,1.36,FractionType.red,24,PlanetType.empty);
        layer.linksManager.addLink(a,b);layer.linksManager.addLink(economy,tower).directional=true;
        layer.linksManager.addLink(b,tower);layer.linksManager.addLink(fort,gun);layer.linksManager.addLink(gun,enemy);
        dig=layer.linksManager.addLink(b,fort);dig.directional=true;dig.builder=FractionType.green;dig.constructionCost=80;dig.constructionSpent=40;
        gameController.onEndCreation();layer.aiManager.entities.clear();layer.linksManager.syncFractions();
        layer.unitManager.launchUnit(a,b,12);wave=layer.unitManager.units.get(layer.unitManager.units.size()-1);wave.acceleratableFactor.value=.45f;wave.moveActually();
        layer.unitManager.launchUnit(economy,tower,60);Unit tank=layer.unitManager.units.get(layer.unitManager.units.size()-1);tank.acceleratableFactor.value=.55f;tank.moveActually();
        layer.unitManager.launchUnit(b,fort,4);crew=layer.unitManager.units.get(layer.unitManager.units.size()-1);crew.acceleratableFactor.value=dig.constructionProgress();crew.moveActually();
        layer.unitManager.launchUnit(b,fort,12);incoming=layer.unitManager.units.get(layer.unitManager.units.size()-1);incoming.acceleratableFactor.value=.15f;incoming.moveActually();
        require(BattlefieldVisuals.crew(dig)>0,"Working crew missing");require(!BattlefieldVisuals.working(incoming),"Incoming troops prematurely joined");
        gameController.speedManager.setSpeed(0);Scenes.speedControls.onSpeedStateChanged();
    }
    private void friendlyArrival(){
        ObjectsLayer layer=gameController.objectsLayer;float before=b.unitsInside;
        for(int i=0;i<10&&wave.isAlive();i++)wave.moveActually();
        require(!wave.isAlive(),"Wave did not arrive");require(b.unitsInside==before+12,"Friendly arrival changed count incorrectly");
        require(!layer.unitManager.visuals.contacts.isEmpty()&&layer.unitManager.visuals.contacts.get(0).friendly,"Friendly event missing");
    }
    private void enemyArrival(){
        ObjectsLayer layer=gameController.objectsLayer;layer.linksManager.addLink(b,enemy);layer.unitManager.launchUnit(b,enemy,12);
        Unit attacker=layer.unitManager.units.get(layer.unitManager.units.size()-1);attacker.acceleratableFactor.value=.999f;
        float before=enemy.unitsInside;for(int i=0;i<10&&attacker.isAlive();i++)attacker.moveActually();
        require(enemy.unitsInside==before-12,"Enemy damage changed");
        require(!layer.unitManager.visuals.contacts.get(layer.unitManager.visuals.contacts.size()-1).friendly,"Enemy event missing");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void paletteFixture(){
        ObjectsLayer layer=gameController.objectsLayer;
        Planet left=node(.20,.63,FractionType.yellow,80,PlanetType.empty),right=node(.80,.63,FractionType.yellow,18,PlanetType.empty);
        layer.linksManager.addLink(left,right);
        layer.unitManager.launchUnit(left,right,14);Unit yellow=layer.unitManager.units.get(layer.unitManager.units.size()-1);yellow.acceleratableFactor.value=0;yellow.moveActually();
        layer.unitManager.launchUnit(left,right,60);Unit tank=layer.unitManager.units.get(layer.unitManager.units.size()-1);tank.acceleratableFactor.value=.72f;tank.moveActually();
        layer.unitManager.launchUnit(enemy,gun,12);Unit red=layer.unitManager.units.get(layer.unitManager.units.size()-1);red.acceleratableFactor.value=.45f;red.moveActually();
    }
    private void accessibleAtlas(){
        try{
            java.lang.reflect.Method method=gameView.getClass().getDeclaredMethod("createAtlas");
            method.setAccessible(true);method.invoke(gameView);
            yio.tro.opacha.game.view.game_renders.GameRendersList.getInstance().updateGameRenders(gameView);
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
}
