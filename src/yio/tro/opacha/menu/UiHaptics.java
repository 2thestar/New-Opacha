package yio.tro.opacha.menu;

/** Platform feedback stays optional; rapid drags cannot produce continuous vibration. */
public final class UiHaptics {
    private static volatile Runnable speedTick = () -> { };
    private static long lastTick;

    private UiHaptics() { }

    public static void setSpeedTick(Runnable handler) {
        lastTick = 0;
        speedTick = handler == null ? () -> { } : handler;
    }

    public static void speedStep() {
        long now = System.nanoTime();
        if (lastTick != 0 && now - lastTick < 70_000_000L) return;
        lastTick = now;
        speedTick.run();
    }
}
