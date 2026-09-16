package lk.zenova.gomart.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import lk.zenova.gomart.activity.VisitShopActivity;
import lk.zenova.gomart.databinding.FragmentSettingsBinding;
import lk.zenova.gomart.helper.AppPreferences;
import lk.zenova.gomart.helper.BrightnessAdvisor;
import lk.zenova.gomart.helper.LightSensorManager;

public class SettingsFragment extends Fragment implements SensorEventListener {

    private FragmentSettingsBinding binding;
    private AppPreferences appPreferences;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private BrightnessAdvisor brightnessAdvisor;
    private LightSensorManager lightSensorManager;

    private Boolean lastAppliedDark = null;
    private boolean isApplyingAutoTheme = false;

    private static final String STORE_PHONE    = "+94112345678";
    private static final String WHATSAPP_PHONE = "+94714649470";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appPreferences     = new AppPreferences(requireContext());
        lightSensorManager = new LightSensorManager(requireContext());
        brightnessAdvisor  = new BrightnessAdvisor(
                requireActivity(), binding.getRoot());

        setupSensorDisplay();
        setupThemeToggle();
        setupNotificationToggles();
        setupTelephony();
        setupAppInfo();
        setupThemeToggle();
        setupAboutUs();

    }

    private void setupAboutUs() {
        binding.settingsBtnVisitShop.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), VisitShopActivity.class);
            startActivity(intent);
        });
    }

    private void setupSensorDisplay() {
        sensorManager = (SensorManager) requireContext()
                .getSystemService(Context.SENSOR_SERVICE);
        lightSensor   = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
                : null;

        if (lightSensor == null) {
            binding.settingsSensorStatus.setText("No light sensor on this device");
            binding.settingsSensorAvailable.setText("Not Available");
            binding.settingsSensorInfo.setText("Not Available");
            binding.settingsLuxValue.setText("N/A");
            binding.settingsEnvironment.setText("N/A");
            binding.settingsBrightnessValue.setText("N/A");
            binding.settingsAutoBrightnessToggle.setEnabled(false);
            binding.settingsAutoBrightnessToggle.setChecked(false);
            binding.settingsAutoThemeToggle.setEnabled(false);
            binding.settingsAutoThemeToggle.setChecked(false);
            return;
        }

        binding.settingsSensorAvailable.setText( lightSensor.getName());
        binding.settingsSensorInfo.setText("Available");

        boolean autoEnabled = appPreferences.isAutoBrightnessEnabled();
        binding.settingsAutoBrightnessToggle.setChecked(autoEnabled);
        updateAutoBrightnessUI(autoEnabled);

        binding.settingsAutoBrightnessToggle.setOnCheckedChangeListener(
                (btn, isChecked) -> {
                    appPreferences.setAutoBrightnessEnabled(isChecked);
                    updateAutoBrightnessUI(isChecked);

                    if (!isChecked) {
                        brightnessAdvisor.restoreSystemBrightness();
                        binding.settingsBrightnessValue.setText("System");
                        binding.settingsSensorStatus.setText(
                                "Sensor active — auto brightness OFF");
                    }

                    Toast.makeText(getContext(),
                            isChecked
                                    ? "Auto brightness enabled"
                                    : "Auto brightness disabled",
                            Toast.LENGTH_SHORT).show();
                });

        boolean autoThemeEnabled = appPreferences.isAutoThemeEnabled();
        binding.settingsAutoThemeToggle.setChecked(autoThemeEnabled);
        updateAutoThemeUI(autoThemeEnabled);

        binding.settingsAutoThemeToggle.setOnCheckedChangeListener(
                (btn, isChecked) -> {
                    appPreferences.setAutoThemeEnabled(isChecked);
                    updateAutoThemeUI(isChecked);

                    binding.settingsDarkModeToggle.setEnabled(!isChecked);

                    if (isChecked) {
                        Toast.makeText(getContext(),
                                " Auto theme enabled — theme will follow ambient light",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        binding.settingsDarkModeToggle.setEnabled(!autoThemeEnabled);
    }

    private void updateAutoBrightnessUI(boolean enabled) {
        if (binding == null) return;
        binding.settingsAutoBrightnessSubtitle.setText(
                enabled
                        ? "ON — adjusting brightness automatically"
                        : "OFF — using system brightness");
    }

    private void updateAutoThemeUI(boolean enabled) {
        if (binding == null) return;
        binding.settingsAutoThemeSubtitle.setText(
                enabled
                        ? "ON — theme follows ambient light (Dark / Light)"
                        : "OFF — using manual theme setting");
    }

    private void setupThemeToggle() {
        boolean isDark = appPreferences.isDarkTheme();
        binding.settingsDarkModeToggle.setChecked(isDark);

        binding.settingsDarkModeToggle.setEnabled(
                !appPreferences.isAutoThemeEnabled());

        binding.settingsDarkModeToggle.setOnCheckedChangeListener(
                (btn, isChecked) -> {

                    if (isApplyingAutoTheme) return;
                    if (isChecked) {
                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES);
                        appPreferences.setTheme(AppPreferences.THEME_DARK);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO);
                        appPreferences.setTheme(AppPreferences.THEME_LIGHT);
                    }
                });
    }

    private void setupNotificationToggles() {
        binding.settingsNotifOrders.setChecked(
                appPreferences.isOrderNotifEnabled());
        binding.settingsNotifPromo.setChecked(
                appPreferences.isPromoNotifEnabled());

        binding.settingsNotifOrders.setOnCheckedChangeListener(
                (btn, isChecked) -> {
                    appPreferences.setOrderNotifEnabled(isChecked);
                    Toast.makeText(getContext(),
                            isChecked
                                    ? "Order notifications enabled"
                                    : "Order notifications disabled",
                            Toast.LENGTH_SHORT).show();
                });

        binding.settingsNotifPromo.setOnCheckedChangeListener(
                (btn, isChecked) -> {
                    appPreferences.setPromoNotifEnabled(isChecked);
                    Toast.makeText(getContext(),
                            isChecked
                                    ? "Promo notifications enabled"
                                    : "Promo notifications disabled",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupTelephony() {
        binding.settingsBtnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + STORE_PHONE));
            startActivity(intent);
        });

        binding.settingsBtnWhatsapp.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(
                        "https://wa.me/" + WHATSAPP_PHONE
                                + "?text=Hello GoMart, I need help!"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(),
                        "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAppInfo() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            binding.settingsAppVersion.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            binding.settingsAppVersion.setText("1.0.0");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;
        if (binding == null || !isAdded()) return;

        float lux = event.values[0];

        requireActivity().runOnUiThread(() -> {
            if (binding == null) return;

            binding.settingsLuxValue.setText(
                    String.format("%.1f lux", lux));

            String environment = LightSensorManager.getEnvironment(lux);
            binding.settingsEnvironment.setText(environment);

            boolean isDark = LightSensorManager.isDarkEnvironment(lux);

            if (appPreferences.isAutoBrightnessEnabled()) {
                float brightness = BrightnessAdvisor.getBrightnessForLux(lux);
                int percent      = Math.round(brightness * 100);
                binding.settingsBrightnessValue.setText(percent + "%");
                binding.settingsSensorStatus.setText(
                         environment + " — brightness " + percent + "%");
                brightnessAdvisor.evaluate(lux);
            } else {
                binding.settingsBrightnessValue.setText("System");
                binding.settingsSensorStatus.setText( environment);
            }

            if (appPreferences.isAutoThemeEnabled()) {
                if (lastAppliedDark == null || lastAppliedDark != isDark) {
                    lastAppliedDark = isDark;

                    isApplyingAutoTheme = true;

                    if (isDark) {
                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES);
                        appPreferences.setTheme(AppPreferences.THEME_DARK);
                        binding.settingsDarkModeToggle.setChecked(true);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO);
                        appPreferences.setTheme(AppPreferences.THEME_LIGHT);
                        binding.settingsDarkModeToggle.setChecked(false);
                    }

                    isApplyingAutoTheme = false;

                }
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (brightnessAdvisor != null) {
            brightnessAdvisor.restoreSystemBrightness();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}