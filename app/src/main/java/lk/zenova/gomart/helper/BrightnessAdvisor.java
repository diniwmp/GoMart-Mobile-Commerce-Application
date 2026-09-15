package lk.zenova.gomart.helper;

import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.snackbar.Snackbar;

public class BrightnessAdvisor {


    private static final float BRIGHTNESS_DARK_ENV  = 0.20f;
    private static final float BRIGHTNESS_LIGHT_ENV = 0.90f;


    private static final float BRIGHTNESS_DEADBAND = 0.05f;
    private float lastSetBrightness = -1;

    private final Activity activity;
    private final View rootView;

    public BrightnessAdvisor(Activity activity, View rootView) {
        this.activity = activity;
        this.rootView = rootView;
    }

    public void evaluate(float lux) {
        float targetBrightness = getBrightnessForLux(lux);

        if (Math.abs(targetBrightness - lastSetBrightness) < BRIGHTNESS_DEADBAND) return;

        lastSetBrightness = targetBrightness;

        applyBrightness(targetBrightness);
        showBrightnessInfo(lux, targetBrightness);
    }

    public static float getBrightnessForLux(float lux) {
        return LightSensorManager.isDarkEnvironment(lux)
                ? BRIGHTNESS_DARK_ENV
                : BRIGHTNESS_LIGHT_ENV;
    }

    private void applyBrightness(float brightness) {
        if (activity == null || activity.isFinishing()) return;

        activity.runOnUiThread(() -> {
            Window window = activity.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            params.screenBrightness = brightness;
            window.setAttributes(params);
        });
    }

    public void restoreSystemBrightness() {
        if (activity == null || activity.isFinishing()) return;

        activity.runOnUiThread(() -> {
            Window window = activity.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            params.screenBrightness =
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            window.setAttributes(params);
            lastSetBrightness = -1;
        });
    }

    private void showBrightnessInfo(float lux, float brightness) {
        if (rootView == null) return;

        String environment = LightSensorManager.getEnvironment(lux);
        int percent        = Math.round(brightness * 100);

        activity.runOnUiThread(() ->
                Snackbar.make(rootView,
                         environment + " — Brightness set to " + percent + "%",
                        Snackbar.LENGTH_SHORT).show()
        );
    }
}