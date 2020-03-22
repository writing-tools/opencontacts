package opencontacts.open.com.opencontacts.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;

import java.util.Date;

import opencontacts.open.com.opencontacts.R;

import static android.content.Context.MODE_PRIVATE;
import static android.text.TextUtils.isEmpty;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getBoolean;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getLong;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getStringFromPreferences;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.isWhatsappInstalled;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.updatePreference;
import static opencontacts.open.com.opencontacts.utils.Common.hasItBeen;

public class SharedPreferencesUtils {
    public static final String IS_DARK_THEME_ACTIVE_PREFERENCES_KEY = "IS_DARK_THEME_ACTIVE_PREFERENCES_KEY";//also hard coded in xml
    public static final String DEFAULT_WHATSAPP_COUNTRY_CODE_PREFERENCES_KEY = "DEFAULT_WHATSAPP_COUNTRY_CODE";
    public static final String CALLER_ID_X_POSITION_ON_SCREEN_PREFERENCE_KEY = "CALLER_ID_X_POSITION_ON_SCREEN";
    public static final String CALLER_ID_Y_POSITION_ON_SCREEN_PREFERENCE_KEY = "CALLER_ID_Y_POSITION_ON_SCREEN";
    public static final String WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY = "WHATSAPP_INTEGRATION_ENABLED";//also hard coded in xml
    public static final String ADDRESSBOOK_URL_SHARED_PREFS_KEY = "ADDRESSBOOK_URL";
    public static final String BASE_SYNC_URL_SHARED_PREFS_KEY = "BASE_SYNC_URL";
    public static final String PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY = "preftimeformat12hours";//also hard coded in xml
    public static final String SYNC_TOKEN_SHARED_PREF_KEY = "sync_token";
    public static final String T9_SEARCH_ENABLED_SHARED_PREF_KEY = "t9searchenabled";//also hard coded in xml
    public static final String LAST_CALL_LOG_READ_TIMESTAMP_SHARED_PREF_KEY = "preference_last_call_log_saved_date";
    public static final String COMMON_SHARED_PREFS_FILE_NAME = "OpenContacts";
    public static final String SIM_PREFERENCE_SHARED_PREF_KEY = "defaultCallingSim";
    public static final String EXPORT_CONTACTS_EVERY_WEEK_SHARED_PREF_KEY = "exportContactsEveryWeek";
    public static final String LAST_EXPORT_TIME_STAMP = "lastExportTimeStamp";
    public static final int WEEKS_TIME_IN_HOURS = 24 * 7;
    public static final String ENCRYPTING_CONTACTS_EXPORT_KEY = "encryptingContactsExportKey";
    public static final String SORT_USING_FIRST_NAME = "sortUsingFirstName";
    public static final String SINGLE_CONTACT_WIDGET_TO_CONTACT_MAPPING = "singleContactWidgetToContactMapping";
    public static final String SHOULD_ASK_FOR_PERMISSIONS = "SHOULD_ASK_FOR_PERMISSIONS";
    public static final String LAST_DEFAULT_TAB_LAUNCH_TIME_SHARED_PREF_KEY = "LAST_DEFAULT_TAB_LAUNCH_TIME";
    public static final String DEFAULT_TAB_SHARED_PREF_KEY = "DEFAULT_TAB";
    public static final String TOGGLE_CONTACT_ACTIONS = "TOGGLE_CONTACT_ACTIONS";


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

    public static int getPreferredSim(Context context){
        return Integer.valueOf(getStringFromPreferences(SIM_PREFERENCE_SHARED_PREF_KEY, "-2", context));
    }

    public static boolean shouldExportContactsEveryWeek(Context context){
        return getBoolean(EXPORT_CONTACTS_EVERY_WEEK_SHARED_PREF_KEY, true, context);
    }

    public static boolean hasItBeenAWeekSinceLastExportOfContacts(Context context){
        long lastExportTimeStamp = getAppsSharedPreferences(context).getLong(LAST_EXPORT_TIME_STAMP, 0);
        return hasItBeen(WEEKS_TIME_IN_HOURS, HOUR, lastExportTimeStamp);
    }

    public static String getEncryptingContactsKey(Context context){
        return getStringFromPreferences(ENCRYPTING_CONTACTS_EXPORT_KEY, context);
    }

    public static boolean hasEncryptingContactsKey(Context context){
        return !isEmpty(getEncryptingContactsKey(context));
    }

    public static void markAutoExportComplete(Context context){
        updatePreference(LAST_EXPORT_TIME_STAMP, new Date().getTime(), context);
    }

    public static boolean shouldSortUsingFirstName(Context context){
        return getBoolean(SORT_USING_FIRST_NAME, true, context);
    }

    public static boolean shouldAskForPermissions(Context context){
        return getBoolean(SHOULD_ASK_FOR_PERMISSIONS, true, context);
    }

    public static void markPermissionsAksed(Context context){
         updatePreference(SHOULD_ASK_FOR_PERMISSIONS, false, context);
    }

    public static boolean shouldLaunchDefaultTab(Context context){
        boolean shouldLaunchDefaultTab = hasItBeen(5, MINUTE, getLong(LAST_DEFAULT_TAB_LAUNCH_TIME_SHARED_PREF_KEY, 0, context));
        updatePreference(LAST_DEFAULT_TAB_LAUNCH_TIME_SHARED_PREF_KEY, new Date().getTime(), context);
        return shouldLaunchDefaultTab;
    }

    public static int getDefaultTab(Context context){
        return Integer.parseInt(getStringFromPreferences(DEFAULT_TAB_SHARED_PREF_KEY, "0", context));
    }

    public static boolean shouldToggleContactActions(Context context){
        return getBoolean(TOGGLE_CONTACT_ACTIONS, false, context);
    }
}
