package yio.tro.opacha;

import com.badlogic.gdx.audio.Sound;

public class SoundThreadYio extends Thread {


    volatile Sound currentSound;
    volatile boolean running = true;
    volatile boolean readyToPlaySound;


    public void shutdown() {
        running = false;
        interrupt();
    }


    public void playSound(Sound sound) {
        setCurrentSound(sound);
        readyToPlaySound = true;
    }


    public void setCurrentSound(Sound currentSound) {
        this.currentSound = currentSound;
    }


    @Override
    public void run() {
        while (running) {
            if (readyToPlaySound) {
                readyToPlaySound = false;
                SoundManager.playSoundDirectly(currentSound);
            }

            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {

            }
        }
    }
}
