package yio.tro.opacha.stuff;


import yio.tro.opacha.RefreshRateDetector;
import yio.tro.opacha.YioGdxGame;

public abstract class RepeatYio<ProviderType> {

    protected ProviderType parent;
    int frequency;
    double countDown;
    boolean ignoreRefreshRate;


    public RepeatYio(ProviderType parent, int frequency) {
        this(parent, frequency, YioGdxGame.random.nextInt(Math.max(1, frequency)));
    }


    public RepeatYio(ProviderType parent, int frequency, int defCount) {
        this.parent = parent;
        ignoreRefreshRate = false;
        setFrequency(frequency);
        countDown = defCount;
    }


    public void move(int quantity) {
        for (int i = 0; i < quantity; i++) {
            move();
        }
    }


    public void move() {
        if (frequency == 0) { performAction(); return; }
        countDown -= ignoreRefreshRate ? 1 : RefreshRateDetector.getInstance().multiplier;
        while (countDown < -1e-7) {
            countDown += frequency + 1;
            performAction();
        }
    }


    public void resetCountDown() {
        countDown = frequency;
    }


    public abstract void performAction();


    public void setCountDown(int countDown) {
        this.countDown = countDown;
    }


    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }


    public void randomizeCountDown() {
        setCountDown(YioGdxGame.random.nextInt(Math.max(1, frequency)));
    }


    public void setIgnoreRefreshRate(boolean ignoreRefreshRate) {
        this.ignoreRefreshRate = ignoreRefreshRate;
    }
}
