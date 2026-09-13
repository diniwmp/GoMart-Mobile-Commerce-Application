package lk.zenova.gomart.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREF_NAME = "GoMartPrefs";

    private static final String KEY_REMEMBER_ME     = "remember_me";
    private static final String KEY_SAVED_EMAIL     = "saved_email";
    private static final String KEY_SAVED_PASSWORD  = "saved_password";
    private static final String KEY_THEME           = "theme_mode";
    private static final String KEY_LAST_CATEGORY   = "last_category_id";
    private static final String KEY_LAST_CAT_NAME   = "last_category_name";
    private static final String KEY_LIGHT_THRESHOLD = "light_threshold";
    private static final String KEY_IS_LOGGED_IN    = "is_logged_in";
    private static final String KEY_USER_NAME       = "user_name";
    private static final String KEY_USER_EMAIL      = "user_email";

    private static final String KEY_SENSOR_ENABLED  = "sensor_enabled";
    private static final String KEY_ORDER_NOTIF     = "order_notif_enabled";
    private static final String KEY_PROMO_NOTIF     = "promo_notif_enabled";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness_enabled";

    private static final String KEY_AUTO_THEME      = "auto_theme_enabled";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public AppPreferences(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void setRememberMe(boolean remember, String email, String password) {
        editor.putBoolean(KEY_REMEMBER_ME, remember);
        if (remember) {
            editor.putString(KEY_SAVED_EMAIL, email);
            editor.putString(KEY_SAVED_PASSWORD, password);
        } else {
            editor.remove(KEY_SAVED_EMAIL);
            editor.remove(KEY_SAVED_PASSWORD);
        }
        editor.apply();
    }

    public boolean isRememberMe() { return prefs.getBoolean(KEY_REMEMBER_ME, false); }
    public String getSavedEmail() { return prefs.getString(KEY_SAVED_EMAIL, ""); }
    public String getSavedPassword() { return prefs.getString(KEY_SAVED_PASSWORD, ""); }

    public void setLoggedIn(boolean loggedIn, String name, String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, loggedIn);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public boolean isLoggedIn()    { return prefs.getBoolean(KEY_IS_LOGGED_IN, false); }
    public String getUserName()    { return prefs.getString(KEY_USER_NAME, ""); }
    public String getUserEmail()   { return prefs.getString(KEY_USER_EMAIL, ""); }

    public void clearSession() { editor.clear(); editor.apply(); }

    public static final String THEME_DARK  = "dark";
    public static final String THEME_LIGHT = "light";

    public void setTheme(String theme) {
        editor.putString(KEY_THEME, theme);
        editor.apply();
    }

    public boolean isDarkTheme() {
        return THEME_DARK.equals(prefs.getString(KEY_THEME, THEME_LIGHT));
    }

    public void setLastCategory(String categoryId, String categoryName) {
        editor.putString(KEY_LAST_CATEGORY, categoryId);
        editor.putString(KEY_LAST_CAT_NAME, categoryName);
        editor.apply();
    }

    public String getLastCategoryId()   { return prefs.getString(KEY_LAST_CATEGORY, ""); }
    public String getLastCategoryName() { return prefs.getString(KEY_LAST_CAT_NAME, ""); }

    public void setLightThreshold(float threshold) {
        editor.putFloat(KEY_LIGHT_THRESHOLD, threshold);
        editor.apply();
    }

    public float getLightThreshold() {
        return prefs.getFloat(KEY_LIGHT_THRESHOLD, 50f);
    }

    public void setSensorEnabled(boolean enabled) {
        editor.putBoolean(KEY_SENSOR_ENABLED, enabled);
        editor.apply();
    }

    public boolean isSensorEnabled() {
        return prefs.getBoolean(KEY_SENSOR_ENABLED, true);
    }

    public void setAutoBrightnessEnabled(boolean enabled) {
        editor.putBoolean(KEY_AUTO_BRIGHTNESS, enabled);
        editor.apply();
    }

    public boolean isAutoBrightnessEnabled() {
        return prefs.getBoolean(KEY_AUTO_BRIGHTNESS, false);
    }


    public void setAutoThemeEnabled(boolean enabled) {
        editor.putBoolean(KEY_AUTO_THEME, enabled);
        editor.apply();
    }

    public boolean isAutoThemeEnabled() {
        return prefs.getBoolean(KEY_AUTO_THEME, false);
    }

    public void setOrderNotifEnabled(boolean enabled) {
        editor.putBoolean(KEY_ORDER_NOTIF, enabled);
        editor.apply();
    }

    public boolean isOrderNotifEnabled() {
        return prefs.getBoolean(KEY_ORDER_NOTIF, true);
    }

    public void setPromoNotifEnabled(boolean enabled) {
        editor.putBoolean(KEY_PROMO_NOTIF, enabled);
        editor.apply();
    }

    public boolean isPromoNotifEnabled() {
        return prefs.getBoolean(KEY_PROMO_NOTIF, true);
    }

    private static final String KEY_WELCOME_SEEN = "welcome_seen";

    public void setWelcomeSeen(boolean seen) {
        editor.putBoolean(KEY_WELCOME_SEEN, seen).apply();
    }

    public boolean isWelcomeSeen() {
        return prefs.getBoolean(KEY_WELCOME_SEEN, false);
    }
}