package lk.zenova.gomart.helper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class LightSensorManager implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor lightSensor;


    private static final float LUX_THRESHOLD = 10f;

    private static final float LUX_DEADBAND = 2f;
    private float lastReportedLux = -1;

    private static final long READING_COOLDOWN_MS = 3000;
    private long lastReadingTime = 0;

    public interface OnLuxReadingListener {
        void onLuxChanged(float lux);
    }

    private OnLuxReadingListener listener;

    public LightSensorManager(Context context) {
        sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        lightSensor   = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
                : null;
    }

    public boolean isAvailable() {
        return lightSensor != null;
    }

    public String getSensorName() {
        return lightSensor != null ? lightSensor.getName() : "N/A";
    }

    public void setOnLuxReadingListener(OnLuxReadingListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(
                    this, lightSensor,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public void readOnce(OnLuxReadingListener callback) {
        if (sensorManager == null || lightSensor == null) {
            callback.onLuxChanged(-1);
            return;
        }
        SensorEventListener oneShot = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;
                float lux = event.values[0];
                sensorManager.unregisterListener(this);
                callback.onLuxChanged(lux);
            }
            @Override
            public void onAccuracyChanged(Sensor s, int a) {}
        };
        sensorManager.registerListener(oneShot, lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;
        float lux = event.values[0];

        if (Math.abs(lux - lastReportedLux) < LUX_DEADBAND) return;

        long now = System.currentTimeMillis();
        if (now - lastReadingTime < READING_COOLDOWN_MS) return;

        lastReportedLux = lux;
        lastReadingTime = now;

        if (listener != null) listener.onLuxChanged(lux);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    public static String getEnvironment(float lux) {
        if (lux <= LUX_THRESHOLD) return "Dark";
        else                      return "Light";
    }

    public static boolean isDarkEnvironment(float lux) {
        return lux <= LUX_THRESHOLD;
    }

    public static boolean isGoodLightingForPhoto(float lux) {
        return lux > 10;
    }
}