package yio.tro.opacha.menu;

/** Platform feedback stays optional; rapid drags cannot produce continuous vibration. */
public final class UiHaptics {
    private static volatile Runnable speedTick = () -> { };
    private static long lastTick;
    private static volatile Runnable captureTick = () -> { };
    private static long lastCapture;

    private UiHaptics() { }

    public static void setSpeedTick(Runnable handler) {
        lastTick = 0;
        speedTick = handler == null ? () -> { } : handler;
    }

    public static void speedStep() {
        if (!yio.tro.opacha.SettingsManager.getInstance().hapticsEnabled) return;
        long now = System.nanoTime();
        if (lastTick != 0 && now - lastTick < 70_000_000L) return;
        lastTick = now;
        speedTick.run();
    }

    public static void setCaptureTick(Runnable handler) {
        lastCapture = 0;
        captureTick = handler == null ? () -> { } : handler;
    }

    public static void nodeCaptured() {
        if (!yio.tro.opacha.SettingsManager.getInstance().hapticsEnabled) return;
        long now = System.nanoTime();
        if (lastCapture != 0 && now - lastCapture < 150_000_000L) return;
        lastCapture = now;
        captureTick.run();
    }
}
