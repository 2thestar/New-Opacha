package yio.tro.opacha.game;

/** Campaign lessons reveal one tool at a time. Other matches default to the complete rules. */
public final class RuleFeatures {
    public static final int HALF = 1, GROW = 2, DEFEND = 4, DIRECTIONAL = 8,
            BUILD = 16, BOMBS = 32, ARTILLERY = 64, ALL = 127;
    private RuleFeatures() { }
    public static boolean has(int feature) { return (GameRules.features & feature) == feature; }
}
