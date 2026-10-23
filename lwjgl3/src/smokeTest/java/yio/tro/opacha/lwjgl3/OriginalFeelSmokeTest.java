package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.campaign.CampaignLevels;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.menu.UiHaptics;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.menu.elements.ScrollableAreaYio;
import yio.tro.opacha.menu.elements.InterfaceElement;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;
import java.util.*;

/** Zero openings, refresh-independent waves, honest shield state and real native renders. */
public final class OriginalFeelSmokeTest extends PlaytestLab {
    private int frames,stage;
    private boolean complete;
    private Planet fort;
    private final long deadline=System.nanoTime()+60_000_000_000L;
    OriginalFeelSmokeTest(String output) { super(output,0); }
    public static void main(String[] args) {
        platformType=PlatformType.pc;
        OriginalFeelSmokeTest game=new OriginalFeelSmokeTest(args[2]);
        Lwjgl3ApplicationConfiguration config=Lwjgl3Launcher.configuration(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
        config.setInitialVisible(false);config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs").path(),Files.FileType.Absolute);
        new Lwjgl3Application(game,config);
        require(game.complete,"Original-feel test ended early");
        System.out.println("ORIGINAL FEEL PASSED: "+args[0]+" x "+args[1]);
    }
    @Override public void render() {
        super.render();
        if(gameController==null||splashVisible||complete)return;
        require(System.nanoTime()<deadline,"Original-feel timeout at "+stage);
        require(!Scenes.catchedException.isCurrentlyVisible(),"Exception screen at "+stage);
        if(++frames%30!=0)return;
        switch(stage++) {
            case 0:
                Scenes.campaignBrief.destroy(); openings(); rhythm(); haptics(); fixture(); break;
            case 1: capture("01-day-shields-orders-routes");UiStyle.applyTheme(true);break;
            case 2: capture("02-night-shields-orders-routes");fort.setCurrentDefense(3);break;
            case 3: capture("03-three-shield-points");SettingsManager.getInstance().colorblindMode=true;break;
            case 4: capture("04-colorblind-shields");SettingsManager.getInstance().colorblindMode=false;UiStyle.applyTheme(false);startLevel(7);Scenes.campaignBrief.destroy();break;
            case 5: capture("05-zero-opening-map-8");startLevel(32);break;
            case 6: capture("06-zero-opening-map-33");startLevel(0);break;
            case 7: capture("07-opening-brief");Scenes.campaignBrief.destroy();setGamePaused(true);gameView.destroy();Scenes.settingsMenu.create();break;
            case 8:
                require(Scenes.settingsMenu.isCurrentlyVisible(),"Settings did not open");
                capture("08-settings-top");scrollSettings();break;
            case 9: capture("09-settings-vibration");UiStyle.applyTheme(true);break;
            case 10: capture("10-settings-vibration-night");complete=true;exitApp();break;
            default:throw new AssertionError("Unexpected stage");
        }
    }
    private void scrollSettings() {
        for(InterfaceElement element:Scenes.settingsMenu.getLocalElementsList())if(element instanceof ScrollableAreaYio)
            for(int i=0;i<70;i++)element.onMouseWheelScrolled(1);
    }
    private Planet node(ObjectsLayer layer,double x,double y,FractionType owner) {
        return layer.planetsManager.spawnPlanet(new CircleYio(x*w,y*w,.05*w),owner);
    }
    private void openings() {
        for(int index=0;index<48;index++) {
            gameController.createGameObjects(); gameController.importManager.perform(CampaignLevels.get(index).code);
            for(Planet p:gameController.objectsLayer.planetsManager.planets)
                if(!p.isNeutral())require(p.unitsInside==0,"Nonzero campaign opening: map "+(index+1));
        }
        System.out.println("ZERO OPENINGS: every faction on all 48 maps");
    }
    private void rhythm() {
        StringBuilder csv=new StringBuilder("refresh_hz,velocity,wave,game_seconds\n");
        for(int hz:new int[]{60,90,120,144,240})for(float speed:new float[]{.5f,1,2}) {
            RefreshRateDetector.getInstance().multiplier=60f/hz;VelocityManager.getInstance().value=speed;
            GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;
            gameController.createGameObjects();ObjectsLayer layer=gameController.objectsLayer;
            Planet a=node(layer,.2,.4,FractionType.green),b=node(layer,.8,.4,FractionType.green);
            layer.linksManager.addLink(a,b);a.setUnitsInside(10000);a.setAutoTarget(b);
            double previous=0;int waves=0;
            for(int tick=1;tick<=Math.ceil(10.81*hz/speed);tick++) {
                a.moveActually();
                if(layer.unitManager.units.size()==waves)continue;
                double time=tick*speed/hz;
                require(layer.unitManager.units.size()==waves+1,"Multiple waves on one tick");
                require(Math.abs((time-previous)-Planet.AUTO_SEND_SECONDS)<speed/hz+.0001,"Irregular wave at "+hz+" Hz");
                previous=time;waves++;csv.append(hz).append(',').append(speed).append(',').append(waves).append(',').append(time).append('\n');
            }
            require(waves==6,"Wave count changes with display refresh: "+hz+", "+speed+", "+waves);
        }
        RefreshRateDetector.getInstance().multiplier=1;VelocityManager.getInstance().value=1;
        output.child("wave-timing.csv").writeString(csv.toString(),false);
        System.out.println("WAVE TIMING: 60/90/120/144/240 Hz at 0.5x/1x/2x");
    }
    private void haptics() {
        GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;GameRules.aiOnlyMode=false;
        gameController.createGameObjects();ObjectsLayer layer=gameController.objectsLayer;
        Planet a=node(layer,.25,.4,FractionType.green),b=node(layer,.75,.4,FractionType.red);
        layer.linksManager.addLink(a,b);a.setUnitsInside(40);
        int[] events=new int[4];SettingsManager.getInstance().hapticsEnabled=true;
        UiHaptics.setSelectionTick(()->events[0]++);UiHaptics.setOrderTick(()->events[1]++);
        UiHaptics.setUpgradeTick(()->events[2]++);UiHaptics.setCaptureTick(()->events[3]++);
        layer.planetsManager.onClick(a.viewPosition.center);a.selectionEngineYio.factorYio.setValue(1);
        require(events[0]==1,"Selection haptic missing");
        layer.planetsManager.onClick(b.viewPosition.center);require(events[1]==1,"Manual order haptic missing");
        a.select();a.selectionEngineYio.factorYio.setValue(1);a.setUnitsInside(40);
        layer.planetsManager.onEconomicUpgradeButtonClicked();require(events[2]==1,"Upgrade haptic missing");
        b.setFraction(FractionType.green);require(events[3]==1,"Capture haptic missing");
        UiHaptics.setOrderTick(()->events[1]++);int count=events[1];a.setAutoTarget(b);a.prepareToSendAutoTargetQuickly();
        for(int tick=0;tick<240;tick++)a.moveActually();
        require(events[1]==count,"Automatic packets should not vibrate");
        SettingsManager.getInstance().hapticsEnabled=false;
        UiHaptics.setSelectionTick(()->events[0]++);UiHaptics.setOrderTick(()->events[1]++);
        UiHaptics.setUpgradeTick(()->events[2]++);UiHaptics.setCaptureTick(()->events[3]++);
        int total=Arrays.stream(events).sum();UiHaptics.nodeSelected();UiHaptics.orderIssued();UiHaptics.nodeUpgraded();UiHaptics.nodeCaptured();UiHaptics.test();
        require(Arrays.stream(events).sum()==total,"Disabled haptics fired");
        UiHaptics.setSelectionTick(null);UiHaptics.setOrderTick(null);UiHaptics.setUpgradeTick(null);UiHaptics.setCaptureTick(null);
        SettingsManager.getInstance().hapticsEnabled=true;
        System.out.println("HAPTICS: selection, manual orders, upgrades, capture, mute, automatic-wave silence");
    }
    private void fixture() {
        GameRules.playType=PlayType.conquest;GameRules.features=RuleFeatures.ALL;
        gameController.createGameObjects();gameController.initLevelSize(LevelSize.tiny);
        ObjectsLayer layer=gameController.objectsLayer;
        Planet a=node(layer,.25,.40,FractionType.green),b=node(layer,.75,.40,FractionType.neutral);
        fort=node(layer,.25,1.0,FractionType.green);Planet d=node(layer,.75,1.0,FractionType.red),e=node(layer,.5,1.46,FractionType.yellow);
        a.setUnitsInside(18);a.setCurrentDefense(5);b.setUnitsInside(3);b.setCurrentDefense(0);
        fort.setType(PlanetType.defensive);fort.setCurrentDefense(8);fort.setUnitsInside(14);
        d.setType(PlanetType.defensive);d.setCurrentDefense(14);d.setUnitsInside(20);
        e.setType(PlanetType.economic);e.setUnitsInside(22);
        layer.linksManager.addLink(a,b);layer.linksManager.addLink(b,d).directional=true;
        layer.linksManager.addLink(a,fort);layer.linksManager.addLink(fort,d);layer.linksManager.addLink(d,e);
        Link dig=layer.linksManager.addLink(fort,e);dig.directional=true;dig.builder=FractionType.green;dig.constructionCost=80;dig.constructionSpent=20;
        gameController.onEndCreation();layer.aiManager.entities.clear();layer.linksManager.syncFractions();
        layer.planetsManager.sendUnitsPercent(a,b,50);
        for(int i=0;i<65;i++)layer.unitManager.moveActually();
        a.setAutoTarget(b);fort.setAutoTarget(d);
        gameController.speedManager.setSpeed(0);Scenes.speedControls.onSpeedStateChanged();
    }
    private static void require(boolean okay,String message) { if(!okay)throw new AssertionError(message); }
}
