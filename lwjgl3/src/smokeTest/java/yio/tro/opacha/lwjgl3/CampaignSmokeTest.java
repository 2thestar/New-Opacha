package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.campaign.*;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.loading.*;
import yio.tro.opacha.menu.*;
import yio.tro.opacha.menu.elements.*;
import yio.tro.opacha.menu.elements.customizable_list.*;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.stuff.*;

/** Touch lessons, free map access, editing and capture feedback; render every authored board. */
public final class CampaignSmokeTest extends YioGdxGame {
    private final FileHandle output;
    private boolean clicking, complete, rulesOnly, flowOnly;
    private int frames, lastFrame, step, lesson, board;
    private final int[] lessons = {0,1,2,3,4,5,6,7,8,9,10,11,12,24,36};
    private final long deadline = System.nanoTime() + 300_000_000_000L;
    private CampaignSmokeTest(String path) { output = new FileHandle(path); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        CampaignSmokeTest game = new CampaignSmokeTest(args[2]); game.rulesOnly = args.length > 3 && args[3].equals("rules");
        game.flowOnly=args.length>3 && args[3].equals("flow");
        Lwjgl3ApplicationConfiguration c = Lwjgl3Launcher.configuration(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
        c.setInitialVisible(false); c.setTitle("New Opacha campaign checks");
        c.useVsync(false); c.setForegroundFPS(60); c.setIdleFPS(60);
        c.setPreferencesConfig(game.output.child("prefs-" + System.currentTimeMillis()).path(),Files.FileType.Absolute);
        new Lwjgl3Application(game,c);
        require(game.complete,"Campaign tour ended early");
        System.out.println((game.rulesOnly?"CAMPAIGN RULES":game.flowOnly?"CAMPAIGN FLOW":"CAMPAIGN UI")+" PASSED: " + args[0] + " x " + args[1]);
    }
    @Override public void render() {
        super.render();
        if (clicking || gameController == null || splashVisible || ++frames-lastFrame < (step==19 || step==20 ? 45 : 90)) return;
        lastFrame = frames;
        require(System.nanoTime() < deadline,"Campaign tour timed out at " + step);
        require(!Scenes.catchedException.isCurrentlyVisible(),"Campaign exception at " + step);
        if (rulesOnly) { CampaignChecks.run(gameController); complete=true; exitApp(); return; }
        if (flowOnly && step==0) { loadLesson(0); step=31; return; }
        switch (step++) {
            case 31: click(Scenes.campaignBrief.startButton); step=25; break;
            case 0: click(Scenes.mainMenu.playButton); break;
            case 1: screenshot("01-game-modes"); click(Scenes.chooseGameMode.mapsButton); break;
            case 2:
                require(Scenes.campaign.freePlay,"Maps did not open free play"); screenshot("02-map-library");
                for (AbstractCustomListItem item : Scenes.campaign.customizableListYio.items) if (item instanceof CampaignCustomItem) {
                    CciInnerItem first=((CampaignCustomItem)item).items.get(0);
                    tap(first.position.x+first.position.width/2,first.position.y+first.position.height/2); break;
                }
                break;
            case 3: require(Scenes.mapDetails.isCurrentlyVisible(),"Map details did not open"); screenshot("03-map-details"); click(Scenes.mapDetails.editButton); break;
            case 4:
                require(gameController.gameMode==GameMode.editor,"Map did not open in editor"); screenshot("04-editor-board"); click(Scenes.editorOverlay.optionsButton); break;
            case 5:
                screenshot("05-editor-options"); click(Scenes.editorOptionsPanel.modeButton); click(Scenes.editorOptionsPanel.routeButton); break;
            case 6:
                require(GameRules.playType==PlayType.capture_flag && gameController.objectsLayer.editorWorker.nextRouteStyle==1,"Editor mode/route controls failed");
                screenshot("06-editor-direction"); key(Input.Keys.ESCAPE); break;
            case 7:
                clickWorld(gameController.objectsLayer.planetsManager.planets.get(0).position.center); break;
            case 8:
                require(Scenes.editSinglePlanet.isCurrentlyVisible(),"Editor node panel did not open");
                require(gameController.objectsLayer.planetsManager.planets.get(0).unitsInside==28,"Opening the editor panel altered the exact garrison");
                screenshot("07-editor-node");
                click(Scenes.editSinglePlanet.aiButton); click(Scenes.editSinglePlanet.stockButton); click(Scenes.editSinglePlanet.trapsButton); break;
            case 9:
                require(gameController.objectsLayer.planetsManager.planets.get(0).bombStock==1,"Editor bomb stock control failed"); require(gameController.objectsLayer.planetsManager.planets.get(0).nodeBombs==1,"Editor node traps control failed"); screenshot("08-editor-stock"); key(Input.Keys.ESCAPE); break;
            case 10:
                LoadingParameters p = new LoadingParameters(); p.addParameter("index",0); loadingManager.startInstantly(LoadingType.campaign_create,p); break;
            case 11:
                require(Scenes.campaignBrief.isCurrentlyVisible() && gameController.speedManager.getSpeed()==0,"First lesson is not paused");
                screenshot("09-first-lesson"); click(Scenes.campaignBrief.startButton); break;
            case 12:
                require(gameController.speedManager.getSpeed()==1,"Lesson Play did not restore speed");
                Planet home=gameController.objectsLayer.planetsManager.planets.get(0); clickWorld(home.position.center); break;
            case 13:
                require(Scenes.selectionOverlay.isCurrentlyVisible(),"First lesson node did not select");
                require(!Scenes.selectionOverlay.actionsButton.isVisible() && !Scenes.selectionOverlay.bEconomic.isVisible(),"Unintroduced tools appeared in first lesson");
                clickWorld(gameController.objectsLayer.planetsManager.planets.get(1).position.center); break;
            case 14:
                if (gameController.objectsLayer.planetsManager.planets.get(1).fraction!=FractionType.green) {step--; return;}
                screenshot("10-first-capture"); click(Scenes.gameOverlay.pauseMenuButton); break;
            case 15: screenshot("11-campaign-pause"); click(Scenes.pauseMenu.hintButton); break;
            case 16:
                require(Scenes.campaignBrief.isCurrentlyVisible() && Scenes.campaignBrief.useHint,"Hint did not open");
                screenshot("12-optional-hint"); click(Scenes.campaignBrief.startButton); step=25; break;
            case 25:
                require(!Scenes.campaign.freePlay,"Campaign retained the free-play browser state");
                forceWinner(FractionType.green); break;
            case 26:
                if (!Scenes.onCampaignLevelCompleted.isCurrentlyVisible()) { step--; return; }
                if (Scenes.onCampaignLevelCompleted.nextButton.getFactor().get()<.99 || gameView.appearFactor.get()>.001) {
                    step--; return;
                }
                require(CampaignManager.getInstance().isLevelCompleted(0),"First campaign victory was not saved");
                screenshot("14-lesson-victory"); click(Scenes.onCampaignLevelCompleted.nextButton); break;
            case 27:
                require(GameRules.levelIndex==1 && Scenes.campaignBrief.isCurrentlyVisible(),"Next did not open the second concept");
                screenshot("15-next-lesson"); click(Scenes.campaignBrief.startButton); break;
            case 28: forceWinner(FractionType.red); break;
            case 29:
                if (!Scenes.campaign.isCurrentlyVisible() || Scenes.campaign.customizableListYio.getFactor().get()<.99 || gameView.appearFactor.get()>.001) { step--; return; }
                require(!Scenes.campaign.freePlay && !CampaignManager.getInstance().isLevelCompleted(1),"A defeat unlocked or skipped the lesson");
                screenshot("16-campaign-after-loss"); step=flowOnly?21:17; break;
            case 17:
                SettingsManager.getInstance().nightMode=true; UiStyle.applyTheme(true); SettingsManager.getInstance().abstractBackgrounds=true;
                loadLesson(lessons[lesson]); break;
            case 18:
                require(Scenes.campaignBrief.isCurrentlyVisible(),"Missing new-concept brief " + lessons[lesson]);
                screenshot(String.format("lesson-%02d-night",lessons[lesson]+1)); click(Scenes.campaignBrief.startButton);
                if (++lesson<lessons.length) step=17;
                break;
            case 19:
                SettingsManager.getInstance().nightMode=false; UiStyle.applyTheme(false); SettingsManager.getInstance().abstractBackgrounds=false;
                LoadingParameters map=new LoadingParameters(); map.addParameter("level_code",CampaignLevels.get(board).code);
                loadingManager.startInstantly(LoadingType.editor_play,map);
                Scenes.quickVelocityTutorial.destroy();
                gameController.speedManager.setSpeed(0); Scenes.speedControls.onSpeedStateChanged(); break;
            case 20:
                require(!Scenes.campaignBrief.isCurrentlyVisible(),"Lesson obscured the map review");
                for (Planet node : gameController.objectsLayer.planetsManager.planets) {
                    OrthographicCamera c=gameController.cameraController.orthoCam;
                    float top=(node.position.center.y+1.6f*node.position.radius-c.position.y)/c.zoom+h/2f;
                    require(top<.82f*h,"HUD covers a node on map "+(board+1));
                }
                screenshot(String.format("map-%02d",board+1));
                if (++board<CampaignLevels.COUNT) step=19;
                break;
            case 21:
                StringBuilder done=new StringBuilder(); for(int i=0;i<47;i++) done.append(i).append(' ');
                Gdx.app.getPreferences("yio.tro.opacha.campaign.v2").putString("progress",done.toString()).flush();
                CampaignManager.getInstance().loadValues(); loadLesson(47); forceWinner(FractionType.green); break;
            case 22:
                if (!Scenes.onCampaignLevelCompleted.isCurrentlyVisible()) { step--; return; }
                if (Scenes.onCampaignLevelCompleted.nextButton.getFactor().get()<.99 || gameView.appearFactor.get()>.001) { step--; return; }
                require(CampaignManager.getInstance().areAllLevelsCompleted(),"Last victory did not complete the campaign");
                click(Scenes.onCampaignLevelCompleted.nextButton); step=30; break;
            case 30:
                require(Scenes.reward.isCurrentlyVisible(),"Last Next did not open campaign completion");
                screenshot("13-campaign-complete"); complete=true; exitApp(); break;
            default: throw new AssertionError("Unexpected tour step " + step);
        }
    }
    private void loadLesson(int index) {
        LoadingParameters p=new LoadingParameters(); p.addParameter("index",index); loadingManager.startInstantly(LoadingType.campaign_create,p);
    }
    private void forceWinner(FractionType winner) {
        gameController.objectsLayer.aiManager.entities.clear();
        for(Planet node:gameController.objectsLayer.planetsManager.planets) node.setFraction(winner);
    }
    private void click(InterfaceElement element) { RectangleYio r=element.getViewPosition(); tap(r.x+r.width/2,r.y+r.height/2); }
    private void tap(float x,float y) {
        clicking=true;
        try { Gdx.input.getInputProcessor().touchDown(Math.round(x),h-Math.round(y),0,Input.Buttons.LEFT); Gdx.input.getInputProcessor().touchUp(Math.round(x),h-Math.round(y),0,Input.Buttons.LEFT); }
        finally { clicking=false; }
    }
    private void clickWorld(PointYio p) {
        OrthographicCamera c=gameController.cameraController.orthoCam;
        tap((p.x-c.position.x)/c.zoom+w/2f,(p.y-c.position.y)/c.zoom+h/2f);
    }
    private void key(int k) { Gdx.input.getInputProcessor().keyDown(k); Gdx.input.getInputProcessor().keyUp(k); }
    private void screenshot(String name) {
        Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(output.child(name+".png"),p,-1,true); } finally { p.dispose(); }
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
