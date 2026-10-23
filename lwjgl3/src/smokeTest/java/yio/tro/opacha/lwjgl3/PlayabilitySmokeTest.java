package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.model.*;
import yio.tro.opacha.game.touch_modes.TouchMode;
import yio.tro.opacha.menu.scenes.Scenes;
import yio.tro.opacha.menu.UiStyle;
import yio.tro.opacha.stuff.*;
import java.util.*;

/** Exercises real pointer input and renders, plus directed flag placement on the production model. */
public final class PlayabilitySmokeTest extends PlaytestLab {
    private int frames, stage;
    private long slowDrag;
    private boolean complete;
    private Planet source, target;
    private Link route;
    private final long deadline = System.nanoTime() + 75_000_000_000L;

    private PlayabilitySmokeTest(String path) { super(path, 7); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        PlayabilitySmokeTest game = new PlayabilitySmokeTest(args[2]);
        Lwjgl3ApplicationConfiguration config = Lwjgl3Launcher.configuration(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        config.setTitle("Opacha touch verification"); config.setInitialVisible(false); config.disableAudio(true);
        config.setPreferencesConfig(game.output.child("prefs").path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, config);
        require(game.complete, "Touch test ended early");
        System.out.println("PLAYABILITY PASSED: " + args[0] + " x " + args[1]);
    }

    @Override public void render() {
        super.render();
        if (gameController == null || splashVisible || complete) return;
        require(System.nanoTime() < deadline, "Timed out at stage " + stage);
        require(!Scenes.catchedException.isCurrentlyVisible(), "Exception screen at stage " + stage);
        if (++frames % 24 != 0) return;
        switch (stage++) {
            case 0:
                Scenes.campaignBrief.destroy();
                fairnessAudit(); fixture(); break;
            case 1:
                click(source); break;
            case 2:
                require(Scenes.selectionOverlay.isCurrentlyVisible(), "Selection actions missing");
                capture("01-selected"); down(source); drag(target); break;
            case 3:
                require(TouchMode.tmDefault.getDragTarget() == target, "Preview chose the wrong node");
                capture("02-repeat-preview"); up(target);
                require(source.autoTarget == target, "Release differs from preview");
                source.setAutoTarget(null);
                down(source); drag(target); slowDrag = System.nanoTime(); break;
            case 4:
                if (System.nanoTime() - slowDrag < 1_200_000_000L) { stage--; break; }
                require(TouchMode.tmDefault.getDragTarget() == target, "Slow aiming lost preview");
                up(target); require(source.autoTarget == target, "Slow drag was silently discarded");
                source.setAutoTarget(null); down(source); drag(target); drag(source); up(source);
                require(source.autoTarget == null, "Returning to source should cancel");
                down(source); drag(target); TouchMode.tmDefault.onReachedTwoFingerState(); up(target);
                require(source.autoTarget == null, "Pinch created a troop order");
                down(source); drag(target); source.setFraction(FractionType.red);
                require(TouchMode.tmDefault.getDragTarget() == null, "Lost source still previews an order");
                up(target); require(source.autoTarget == null, "Lost source received an order");
                source.setFraction(FractionType.green);
                route.constructionCost = 80; route.builder = FractionType.red;
                down(source); drag(target);
                require(TouchMode.tmDefault.getDragTarget() == null, "Hostile blueprint is selectable");
                up(target); route.constructionCost = 0; route.builder = null;
                // Screen-space hit radius at a zoom where the original world-space target is too small.
                float zoom = gameController.cameraController.orthoCam.zoom;
                gameController.cameraController.orthoCam.zoom = 2;
                double radius = gameController.objectsLayer.planetsManager.getTouchRadius(source);
                require(radius >= 44 * w / 390.0 - .01, "Zoomed-out hit target is smaller than 44 points");
                PointYio point = new PointYio(); point.set(source.position.center.x, source.position.center.y - radius * .9);
                require(gameController.objectsLayer.planetsManager.findTouchedPlanet(point) == source, "Near-edge finger tap missed");
                point.y = (float) (source.position.center.y - radius * 1.2);
                require(gameController.objectsLayer.planetsManager.findTouchedPlanet(point) == null, "Far tap grabbed a node");
                gameController.cameraController.orthoCam.zoom = zoom;
                gameController.objectsLayer.planetsManager.onLongTappedPlanet(source); down(source); drag(target); break;
            case 5:
                capture("03-half-preview"); up(target);
                require(source.halvedSelectionMode && source.autoTarget == target, "Half-send mode lost during drag: half=" + source.halvedSelectionMode + ", target=" + source.autoTarget);
                UiStyle.applyTheme(true); source.setAutoTarget(null); down(source); drag(target); break;
            case 6:
                capture("04-night-preview"); up(target); source.setAutoTarget(null);
                startLevel(0); break;
            case 7:
                require(GameRules.levelIndex == 0 && Scenes.campaignBrief.isCurrentlyVisible(), "Direct map reload failed");
                require(gameController.speedManager.getSpeed() == 0, "Briefing should pause");
                capture("05-map-brief"); Scenes.campaignBrief.destroy(); startLevel(12); break;
            case 8:
                require(GameRules.levelIndex == 12, "Map navigation failed");
                capture("06-flag-brief"); complete = true; exitApp(); break;
            default: throw new AssertionError("Unexpected stage");
        }
    }

    private void fixture() {
        GameRules.playType = PlayType.conquest; GameRules.features = RuleFeatures.ALL;
        gameController.createGameObjects(); gameController.initLevelSize(LevelSize.tiny);
        ObjectsLayer layer = gameController.objectsLayer;
        source = node(layer,.28,.72,FractionType.green); target = node(layer,.72,.72,FractionType.neutral);
        Planet enemy = node(layer,.72,1.25,FractionType.red), rear = node(layer,.28,1.25,FractionType.green);
        source.setUnitsInside(40); rear.setUnitsInside(25); rear.setType(PlanetType.economic);
        route = layer.linksManager.addLink(source,target);
        layer.linksManager.addLink(target,enemy); layer.linksManager.addLink(source,rear); layer.linksManager.addLink(rear,enemy);
        gameController.onEndCreation(); layer.aiManager.entities.clear();
        gameController.speedManager.setSpeed(0); Scenes.speedControls.onSpeedStateChanged();
    }

    private Planet node(ObjectsLayer layer,double x,double y,FractionType fraction) {
        CircleYio circle = new CircleYio(); circle.center.set(x*w,y*w); circle.setRadius(.05*w);
        return layer.planetsManager.spawnPlanet(circle,fraction);
    }

    private void fairnessAudit() {
        GameRules.playType = PlayType.conquest; GameRules.aiOnlyMode = false;
        gameController.createGameObjects(); ObjectsLayer layer = gameController.objectsLayer;
        Planet green = node(layer,.2,.4,FractionType.green), red = node(layer,.8,.4,FractionType.red);
        Planet biased = node(layer,.5,.65,FractionType.neutral), fair = node(layer,.5,1.0,FractionType.neutral);
        layer.linksManager.addLink(green,biased); layer.linksManager.addLink(biased,red).directional = true;
        layer.linksManager.addLink(green,fair); layer.linksManager.addLink(red,fair);
        require(FlagPlacement.choose(layer.planetsManager.planets)==fair, "Flag ignores reverse travel penalty");
        double[] forward = FlagPlacement.travelDistances(green,layer.planetsManager.planets);
        double[] reverse = FlagPlacement.travelDistances(red,layer.planetsManager.planets);
        require(Math.abs(reverse[2]/forward[2]-1/.3)<.001, "Directed travel cost is wrong");
        StringBuilder csv = new StringBuilder("seed,old_spread,new_spread,changed\n"); int improved=0, changed=0;
        for (int seed=1; seed<=64; seed++) {
            predictableRandom.setSeed(seed*3127L); random.setSeed(seed*109L);
            gameController.createGameObjects(); layer=gameController.objectsLayer;
            gameController.initLevelSize(LevelSize.tiny); layer.mapGenerator.generateLinkedMap(1,4);
            int i=0; for(Link link:layer.linksManager.links) link.directional=i++%4==0;
            Planet old=legacyFlag(layer), next=FlagPlacement.choose(layer.planetsManager.planets);
            double a=spread(layer,old), b=spread(layer,next);
            require(b<=a+1e-8, "Travel fairness regressed on seed "+seed);
            if(b<a-1e-8) improved++; if(old!=next) changed++;
            csv.append(String.format(Locale.ROOT,"%d,%.6f,%.6f,%s%n",seed,a,b,old!=next));
        }
        output.child("flag-fairness.csv").writeString(csv.toString(),false);
        System.out.println("FLAG TRAVEL: "+improved+" improved / 64; "+changed+" placements changed");
    }

    private Planet legacyFlag(ObjectsLayer layer) {
        Planet best=null; double score=Double.MAX_VALUE;
        for(Planet candidate:layer.planetsManager.planets) {
            if(!candidate.isNeutral()) continue;
            int min=Integer.MAX_VALUE,max=0,total=0;
            for(Planet start:layer.planetsManager.planets) if(!start.isNeutral()) {
                ArrayList<Planet> path=layer.planetsManager.pathFinder.findWay(start,candidate);
                int distance=path==null?1000:path.size(); min=Math.min(min,distance); max=Math.max(max,distance); total+=distance;
            }
            double current=100*(max-min)+total;
            if(current<score){score=current;best=candidate;}
        }
        return best;
    }

    private double spread(ObjectsLayer layer, Planet target) {
        require(target!=null,"No reachable neutral flag");
        int index=layer.planetsManager.planets.indexOf(target); double min=Double.MAX_VALUE,max=0;
        for(Planet start:layer.planetsManager.planets) if(!start.isNeutral()) {
            double d=FlagPlacement.travelDistances(start,layer.planetsManager.planets)[index]; min=Math.min(min,d);max=Math.max(max,d);
        }
        return (max-min)/max;
    }
    private void click(Planet p) { down(p); up(p); }
    private int x(Planet p) { OrthographicCamera c=gameController.cameraController.orthoCam;return Math.round((p.viewPosition.center.x-c.position.x)/c.zoom+w/2f); }
    private int y(Planet p) { OrthographicCamera c=gameController.cameraController.orthoCam;return h-Math.round((p.viewPosition.center.y-c.position.y)/c.zoom+h/2f); }
    private void down(Planet p) { Gdx.input.getInputProcessor().touchDown(x(p),y(p),0,Input.Buttons.LEFT); }
    private void drag(Planet p) { Gdx.input.getInputProcessor().touchDragged(x(p),y(p),0); }
    private void up(Planet p) { Gdx.input.getInputProcessor().touchUp(x(p),y(p),0,Input.Buttons.LEFT); }
    private static void require(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
