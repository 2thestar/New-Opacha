package io.github.twothestar.newopacha;

import android.os.Bundle;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import yio.tro.opacha.PlatformType;
import yio.tro.opacha.YioGdxGame;
import yio.tro.opacha.menu.UiHaptics;

public final class AndroidLauncher extends AndroidApplication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        config.useImmersiveMode = true;
        config.renderUnderCutout = false;
        YioGdxGame.platformType = PlatformType.android;
        initialize(new YioGdxGame(), config);
        UiHaptics.setSpeedTick(() -> runOnUiThread(() -> getWindow().getDecorView().performHapticFeedback(
                Build.VERSION.SDK_INT >= 34 ? HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                        : HapticFeedbackConstants.CLOCK_TICK)));
        UiHaptics.setCaptureTick(() -> runOnUiThread(() -> getWindow().getDecorView().performHapticFeedback(
                Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.CLOCK_TICK)));
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
    }
}
