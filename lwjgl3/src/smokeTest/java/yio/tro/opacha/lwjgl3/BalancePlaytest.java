package yio.tro.opacha.lwjgl3;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import yio.tro.opacha.*;
import yio.tro.opacha.game.*;
import yio.tro.opacha.game.gameplay.ObjectsLayer;
import yio.tro.opacha.game.gameplay.ai.AiType;
import yio.tro.opacha.game.gameplay.model.*;
import java.util.*;

/** Reproducible strategy stress tests. Measures proxies, never claims human enjoyment. */
public final class BalancePlaytest extends YioGdxGame {
    private final FileHandle output;
    private int frames;
    private boolean complete;
    private boolean campaign;
    public BalancePlaytest(String path) { output = new FileHandle(path); }
    public static void main(String[] args) {
        platformType = PlatformType.pc;
        BalancePlaytest game = new BalancePlaytest(args[0]);
        game.campaign = args.length > 1 && args[1].equals("campaign");
        Lwjgl3ApplicationConfiguration c = Lwjgl3Launcher.configuration(480, 800);
        c.setInitialVisible(false); c.setTitle("New Opacha balance lab");
        c.setPreferencesConfig(game.output.child("prefs").path(), Files.FileType.Absolute);
        new Lwjgl3Application(game, c);
        if (!game.complete) throw new AssertionError("Balance lab ended early");
    }
    @Override public void render() {
        super.render();
        if (gameController == null || splashVisible || ++frames < 45) return;
        if (complete) return;
        SettingsManager.getInstance().soundEnabled = false;
        RefreshRateDetector.getInstance().multiplier = 1;
        VelocityManager.getInstance().value = 1;
        output.mkdirs();
        SettingsManager.getInstance().adaptiveDifficulty = false;
        StringBuilder csv = new StringBuilder("mode,seed,seat,style,won,winner,seconds,first_capture,captures,recaptures,longest_drought,orders,upgrades,tools,pacts,level\n");
        int firstSeed = Integer.getInteger("labSeedStart", 1);
        int seedCount = Integer.getInteger("labSeeds", campaign ? 2 : 4);
        Set<String> levels = new HashSet<>(Arrays.asList(System.getProperty("labLevels", "").split(",")));
        if (campaign) {
            for (int level = 0; level < yio.tro.opacha.game.campaign.CampaignLevels.COUNT; level++) {
                if (!levels.contains("") && !levels.contains(Integer.toString(level+1))) continue;
                for (int seed = firstSeed; seed < firstSeed+seedCount; seed++) for (HumanStylePlayer.Style style : HumanStylePlayer.Style.values()) {
                    csv.append(run(PlayType.valueOf(yio.tro.opacha.game.campaign.CampaignLevels.get(level).mode), seed, 0, style, level));
                    output.child("matches.csv").writeString(csv.toString(), false);
                }
            }
        } else {
        for (PlayType mode : PlayType.values()) for (int seed = firstSeed; seed < firstSeed+seedCount; seed++)
            for (int seat = 0; seat < 2; seat++) for (HumanStylePlayer.Style style : HumanStylePlayer.Style.values()) {
                csv.append(run(mode, seed, seat, style, -1));
                output.child("matches.csv").writeString(csv.toString(), false);
            }
        }
        complete = true; exitApp();
    }
    private String run(PlayType mode, int seed, int seat, HumanStylePlayer.Style style, int levelIndex) {
        GameRules.playType = mode; GameRules.aiOnlyMode = false; GameRules.difficulty = Difficulty.hard;
        GameRules.features = RuleFeatures.ALL;
        GameRules.aiTypes.clear(); gameController.gameMode = GameMode.skirmish;
        loadingManager.initLevelSize(LevelSize.tiny);
        predictableRandom.setSeed(seed * 3127L); random.setSeed(seed * 109L);
        gameController.createGameObjects(); ObjectsLayer layer = gameController.objectsLayer;
        if (levelIndex >= 0) gameController.importManager.perform(yio.tro.opacha.game.campaign.CampaignLevels.get(levelIndex).code);
        else layer.mapGenerator.generateLinkedMap(1, 4);
        if (seat == 1) for (Planet node : layer.planetsManager.planets) {
            if (node.fraction == FractionType.green) node.setFraction(FractionType.red);
            else if (node.fraction == FractionType.red) node.setFraction(FractionType.green);
        }
        if (levelIndex < 0) {
        GameRules.aiTypes.put(FractionType.red, AiType.simple);
        GameRules.aiTypes.put(FractionType.yellow, AiType.savvy);
        GameRules.aiTypes.put(FractionType.cyan, AiType.silly);
        int idx = 0; for (Link link : layer.linksManager.links) link.directional = idx++ % 4 == 0;
        }
        layer.onEndCreation();
        HumanStylePlayer player = new HumanStylePlayer(style, FractionType.green, seed * 197L + seat);
        FractionType[] previous = new FractionType[layer.planetsManager.planets.size()];
        boolean[] captured = new boolean[previous.length];
        for (int i = 0; i < previous.length; i++) previous[i] = layer.planetsManager.planets.get(i).fraction;
        double first = -1, last = 0, longest = 0; int captures = 0, recaptures = 0;
        FractionType winner = null;
        for (int tick = 0; tick < 18000; tick++) {
            player.move(layer); layer.moveActually();
            if (tick % 30 != 0) continue;
            for (int i = 0; i < previous.length; i++) {
                FractionType now = layer.planetsManager.planets.get(i).fraction;
                if (now != previous[i] && now == FractionType.green) {
                    captures++; if (captured[i]) recaptures++; captured[i] = true;
                    if (first < 0) first = layer.tactics.elapsed;
                    longest = Math.max(longest, layer.tactics.elapsed - last); last = layer.tactics.elapsed;
                }
                previous[i] = now;
            }
            winner = layer.tactics.winner();
            if (winner == null && layer.tactics.alliances.aliveCount() == 1)
                for (FractionType f : FractionType.values()) if (f != FractionType.neutral && layer.tactics.alliances.alive(f)) winner = f;
            if (winner != null || !layer.tactics.alliances.alive(FractionType.green)) break;
        }
        boolean won = winner == FractionType.green || (winner != null && mode == PlayType.alliances
                && layer.tactics.alliances.alive(FractionType.green) && layer.tactics.alliances.peacefulSurvivors());
        longest = Math.max(longest, layer.tactics.elapsed - last);
        String row = String.format(Locale.ROOT, "%s,%d,%d,%s,%s,%s,%.2f,%.2f,%d,%d,%.2f,%d,%d,%d,%d,%d%n",
                mode, seed, seat, style, won, winner, layer.tactics.elapsed, first, captures, recaptures,
                longest, player.orders, player.upgrades, player.tools, player.pactActions, levelIndex + 1);
        if (style == HumanStylePlayer.Style.hesitant) System.out.print(row);
        return row;
    }
}
