package lk.zenova.gomart;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import lk.zenova.gomart.helper.AppPreferences;
import lk.zenova.gomart.helper.NotificationHelper;

public class GoMartApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationHelper.createNotificationChannels(this);

        AppPreferences prefs = new AppPreferences(this);
        if (prefs.isDarkTheme()) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}