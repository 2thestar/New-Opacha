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
    @Override public void onResume(){
        super.onResume();
        yio.tro.opacha.menu.UiMotion.reduced=!android.animation.ValueAnimator.areAnimatorsEnabled();
    }
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
        new AndroidHaptics(this,graphics.getView()).install();
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
    }
}
