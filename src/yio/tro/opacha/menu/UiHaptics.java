package yio.tro.opacha.menu;

/** Platform feedback stays optional; rapid drags cannot produce continuous vibration. */
public final class UiHaptics {
    private static volatile Runnable speedTick = () -> { };
    private static long lastTick;
    private static volatile Runnable captureTick = () -> { };
    private static long lastCapture;
    private static volatile Runnable selectionTick = () -> { }, orderTick = () -> { }, upgradeTick = () -> { };
    private static volatile java.util.function.Supplier<String> status = () -> "ui_haptics_unavailable";
    private static long lastSelection, lastOrder, lastUpgrade;

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

    public static void setSelectionTick(Runnable handler) { selectionTick=handler==null?()->{}:handler; lastSelection=0; }
    public static void setOrderTick(Runnable handler) { orderTick=handler==null?()->{}:handler; lastOrder=0; }
    public static void setUpgradeTick(Runnable handler) { upgradeTick=handler==null?()->{}:handler; lastUpgrade=0; }
    public static void setStatus(java.util.function.Supplier<String> provider) { status=provider; }
    public static String getStatus() { return status.get(); }

    public static void nodeSelected() {
        if (!yio.tro.opacha.SettingsManager.getInstance().hapticsEnabled) return;
        long now=System.nanoTime(); if(lastSelection!=0 && now-lastSelection<80_000_000L)return;
        lastSelection=now; selectionTick.run();
    }
    public static void orderIssued() {
        if (!yio.tro.opacha.SettingsManager.getInstance().hapticsEnabled) return;
        long now=System.nanoTime(); if(lastOrder!=0 && now-lastOrder<100_000_000L)return;
        lastOrder=now; orderTick.run();
    }
    public static void nodeUpgraded() {
        if (!yio.tro.opacha.SettingsManager.getInstance().hapticsEnabled) return;
        long now=System.nanoTime(); if(lastUpgrade!=0 && now-lastUpgrade<120_000_000L)return;
        lastUpgrade=now; upgradeTick.run();
    }
    public static void test() {
        if (yio.tro.opacha.SettingsManager.getInstance().hapticsEnabled) captureTick.run();
    }
}
