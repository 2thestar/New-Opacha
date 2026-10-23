package yio.tro.opacha.game.gameplay;

/** Fixed game ticks, independent of display rate. Never catch up time spent paused. */
public final class SimulationClock {
    private double remainder;

    public int advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0) return 0;
        // Bound long stalls to six ticks so resume or a debugger cannot freeze input.
        remainder += Math.min(seconds, .1) * 60;
        int steps = (int) Math.floor(remainder + 1e-6);
        remainder = Math.max(0, remainder - steps);
        return steps;
    }

    public void reset() { remainder = 0; }
}
