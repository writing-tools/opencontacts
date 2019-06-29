package opencontacts.open.com.opencontacts.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;

import opencontacts.open.com.opencontacts.R;

import static android.content.Context.MODE_PRIVATE;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getBoolean;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getStringFromPreferences;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.isWhatsappInstalled;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.updatePreference;

public class SharedPreferencesUtils {
    public static final String IS_DARK_THEME_ACTIVE_PREFERENCES_KEY = "IS_DARK_THEME_ACTIVE_PREFERENCES_KEY";
    public static final String DEFAULT_WHATSAPP_COUNTRY_CODE_PREFERENCES_KEY = "DEFAULT_WHATSAPP_COUNTRY_CODE";
    public static final String CALLER_ID_X_POSITION_ON_SCREEN_PREFERENCE_KEY = "CALLER_ID_X_POSITION_ON_SCREEN";
    public static final String CALLER_ID_Y_POSITION_ON_SCREEN_PREFERENCE_KEY = "CALLER_ID_Y_POSITION_ON_SCREEN";
    public static final String WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY = "WHATSAPP_INTEGRATION_ENABLED";
    public static final String ADDRESSBOOK_URL_SHARED_PREFS_KEY = "ADDRESSBOOK_URL";
    public static final String BASE_SYNC_URL_SHARED_PREFS_KEY = "BASE_SYNC_URL";
    public static final String PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY = "preftimeformat12hours";
    public static final String SYNC_TOKEN_SHARED_PREF_KEY = "sync_token";
    public static final String T9_SEARCH_ENABLED_SHARED_PREF_KEY = "t9searchenabled";
    public static final String LAST_CALL_LOG_READ_TIMESTAMP_SHARED_PREF_KEY = "preference_last_call_log_saved_date";
    public static final String COMMON_SHARED_PREFS_FILE_NAME = "OpenContacts";


    public static String getDefaultWhatsAppCountryCode(Context context) {
        return getAppsSharedPreferences(context)
                .getString(DEFAULT_WHATSAPP_COUNTRY_CODE_PREFERENCES_KEY, "");

    }

    public static SharedPreferences getAppsSharedPreferences(Context context){
        return context.getSharedPreferences(COMMON_SHARED_PREFS_FILE_NAME, MODE_PRIVATE);
    }

    public static void saveCallerIdLocationOnScreen(int x, int y, Context context) {
        getAppsSharedPreferences(context)
                .edit()
                .putInt(CALLER_ID_X_POSITION_ON_SCREEN_PREFERENCE_KEY, x)
                .putInt(CALLER_ID_Y_POSITION_ON_SCREEN_PREFERENCE_KEY, y)
                .apply();
    }
    public static Point getCallerIdLocationOnScreen(Context context) {
        return new Point(getAppsSharedPreferences(context).getInt(CALLER_ID_X_POSITION_ON_SCREEN_PREFERENCE_KEY, 0), getAppsSharedPreferences(context).getInt(CALLER_ID_Y_POSITION_ON_SCREEN_PREFERENCE_KEY, 100));
    }

    public static int getCurrentTheme(Context context) {
        return isDarkThemeActive(context) ? R.style.Theme_AppCompat_NoActionBar_Customized : R.style.Theme_AppCompat_Light_NoActionBar_Customized;
    }

    private static boolean isDarkThemeActive(Context context) {
        return getAppsSharedPreferences(context).getBoolean(IS_DARK_THEME_ACTIVE_PREFERENCES_KEY, false);
    }

    public static void enableWhatsappIntegration(String selectedCountryCodeWithPlus, Context context) {
        getAppsSharedPreferences(context)
                .edit()
                .putString(DEFAULT_WHATSAPP_COUNTRY_CODE_PREFERENCES_KEY, selectedCountryCodeWithPlus)
                .putBoolean(WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY, true)
                .apply();
    }

    public static void disableWhatsappIntegration(Context  context) {
        getAppsSharedPreferences(context)
                .edit()
                .putBoolean(WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY, false)
                .apply();
    }

    public static boolean isWhatsappIntegrationEnabled(Context  context) {
        return getAppsSharedPreferences(context)
                .getBoolean(WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY, false)
                && isWhatsappInstalled(context);
    }

    public static void setSharedPreferencesChangeListener(SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener, Context context) {
        getAppsSharedPreferences(context).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    public static boolean is12HourFormatEnabled(Context context) {
        return getBoolean(PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY, true, context);
    }

    public static boolean isT9SearchEnabled(Context context){
        return getBoolean(T9_SEARCH_ENABLED_SHARED_PREF_KEY, true, context);
    }

    public static String getLastSavedCallLogDate(Context context) {
        return getStringFromPreferences(LAST_CALL_LOG_READ_TIMESTAMP_SHARED_PREF_KEY, "0", context);
    }

    public static void setLastSavedCallLogDate(String date, Context context) {
        updatePreference(LAST_CALL_LOG_READ_TIMESTAMP_SHARED_PREF_KEY, date, context);
    }

}
