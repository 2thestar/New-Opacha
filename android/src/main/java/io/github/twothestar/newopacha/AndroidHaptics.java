package io.github.twothestar.newopacha;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import yio.tro.opacha.menu.UiHaptics;

/** Semantic game feedback. Respects both the game's switch and Android touch-feedback settings. */
final class AndroidHaptics {
    private enum Cue { TICK, CLICK, CAPTURE }
    private final Activity activity;
    private final View gameView;
    private final Vibrator vibrator;
    private final AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();

    AndroidHaptics(Activity activity, View gameView) {
        this.activity=activity; this.gameView=gameView;
        if(Build.VERSION.SDK_INT>=31) {
            VibratorManager manager=activity.getSystemService(VibratorManager.class);
            vibrator=manager==null?null:manager.getDefaultVibrator();
        } else vibrator=activity.getSystemService(Vibrator.class);
        gameView.setHapticFeedbackEnabled(true);
    }

    void install() {
        UiHaptics.setSpeedTick(()->play("speed",Cue.TICK));
        UiHaptics.setSelectionTick(()->play("select",Cue.TICK));
        UiHaptics.setOrderTick(()->play("order",Cue.CLICK));
        UiHaptics.setUpgradeTick(()->play("upgrade",Cue.CLICK));
        UiHaptics.setCaptureTick(()->play("capture",Cue.CAPTURE));
        UiHaptics.setStatus(this::status);
    }

    private String status() {
        if(vibrator==null || !vibrator.hasVibrator()) return "ui_haptics_unavailable";
        if(Settings.System.getInt(activity.getContentResolver(),Settings.System.HAPTIC_FEEDBACK_ENABLED,1)==0)
            return "ui_haptics_system_off";
        return "";
    }

    private void play(String event,Cue cue) {
        activity.runOnUiThread(()->{
            String blocked=status();
            if(!blocked.isEmpty() || !gameView.hasWindowFocus()) { diagnostic(event,"blocked:"+blocked); return; }
            if(Build.VERSION.SDK_INT>=29) {
                int effect=cue==Cue.TICK?VibrationEffect.EFFECT_TICK:
                        cue==Cue.CLICK?VibrationEffect.EFFECT_CLICK:VibrationEffect.EFFECT_HEAVY_CLICK;
                vibrator.vibrate(VibrationEffect.createPredefined(effect),attributes);
                diagnostic(event,"predefined:"+effect);
            } else {
                boolean delivered=gameView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                diagnostic(event,"view:"+delivered);
            }
        });
    }

    private void diagnostic(String event,String result) {
        if((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0)
            Log.d("OpachaHaptics",event+" "+result);
    }
}
