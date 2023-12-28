package opencontacts.open.com.opencontacts.utils;

import static android.content.Context.MODE_PRIVATE;
import static android.text.TextUtils.isEmpty;
import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Collections.emptySet;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getBoolean;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getLong;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getStringFromPreferences;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.updatePreference;
import static opencontacts.open.com.opencontacts.utils.Common.hasItBeen;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import opencontacts.open.com.opencontacts.BuildConfig;
import opencontacts.open.com.opencontacts.R;

public class SharedPreferencesUtils {
    public static final String IS_DARK_THEME_ACTIVE_PREFERENCES_KEY = "IS_DARK_THEME_ACTIVE_PREFERENCES_KEY";//also hard coded in xml
    public static final String DEFAULT_SOCIAL_COUNTRY_CODE_PREFERENCES_KEY = "DEFAULT_SOCIAL_COUNTRY_CODE";
    public static final String CALLER_ID_X_POSITION_ON_SCREEN_PREFERENCE_KEY = "CALLER_ID_X_POSITION_ON_SCREEN";
    public static final String CALLER_ID_Y_POSITION_ON_SCREEN_PREFERENCE_KEY = "CALLER_ID_Y_POSITION_ON_SCREEN";
    public static final String SOCIAL_INTEGRATION_ENABLED_PREFERENCE_KEY = "SOCIAL_INTEGRATION_ENABLED";//also hard coded in xml
    public static final String ADDRESSBOOK_URL_SHARED_PREFS_KEY = "ADDRESSBOOK_URL";
    public static final String BASE_SYNC_URL_SHARED_PREFS_KEY = "BASE_SYNC_URL";
    public static final String CARD_DAV_SERVER_TYPE_SHARED_PREFS_KEY = "CARD_DAV_SERVER_TYPE";
    public static final String PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY = "preftimeformat12hours";//also hard coded in xml
    public static final String SYNC_TOKEN_SHARED_PREF_KEY = "sync_token";
    public static final String T9_SEARCH_ENABLED_SHARED_PREF_KEY = "t9searchenabled";//also hard coded in xml
    public static final String T9_PINYIN_ENABLED_SHARED_PREF_KEY = "T9_PINYIN_ENABLED";//also hard coded in xml
    public static final String LAST_CALL_LOG_READ_TIMESTAMP_SHARED_PREF_KEY = "preference_last_call_log_saved_date";
    public static final String COMMON_SHARED_PREFS_FILE_NAME = "OpenContacts";
    public static final String DATA_SHARE_SHARED_PREFS_FILE_NAME = "DATASHARE";
    public static final String SIM_PREFERENCE_SHARED_PREF_KEY = "defaultCallingSim";
    public static final String EXPORT_CONTACTS_EVERY_WEEK_SHARED_PREF_KEY = "exportContactsEveryWeek";
    public static final String LAST_EXPORT_TIME_STAMP = "lastExportTimeStamp";
    public static final int WEEKS_TIME_IN_HOURS = 24 * 7;
    public static final String ENCRYPTING_CONTACTS_EXPORT_KEY = "encryptingContactsExportKey";
    public static final String SORT_USING_FIRST_NAME = "sortUsingFirstName";
    public static final String LOCK_TO_PORTRAIT = "lockToPortrait";
    public static final String SINGLE_CONTACT_WIDGET_TO_CONTACT_MAPPING = "singleContactWidgetToContactMapping";
    public static final String SHOULD_ASK_FOR_PERMISSIONS = "SHOULD_ASK_FOR_PERMISSIONS";
    public static final String LAST_DEFAULT_TAB_LAUNCH_TIME_SHARED_PREF_KEY = "LAST_DEFAULT_TAB_LAUNCH_TIME";
    public static final String DEFAULT_TAB_SHARED_PREF_KEY = "DEFAULT_TAB";
    public static final String TOGGLE_CONTACT_ACTIONS = "TOGGLE_CONTACT_ACTIONS";
    public static final String DEFAULT_SIM_SELECTION_SYSTEM_DEFAULT = "-2";
    public static final String DEFAULT_SIM_SELECTION_ALWAYS_ASK = "-1";
    public static final String SHOULD_USE_SYSTEM_PHONE_APP = "SHOULD_USE_SYSTEM_PHONE_APP";
    public static final String SHORTCUTS_ADDED_IN_VERSION_SHARED_PREF_KEY = "SHORTCUTS_ADDED_IN_VERSION";
    public static final String KEYBOARD_RESIZE_VIEWS_SHARED_PREF_KEY = "KEYBOARD_RESIZE_VIEWS";
    public static final String BOTTOM_MENU_OPEN_DEFAULT_SHARED_PREF_KEY = "BOTTOM_MENU_OPEN_DEFAULT";
    public static final String LAST_VISITED_GROUP_SHARED_PREF_KEY = "LAST_VISITED_GROUP";
    public static final String DEFAULT_SOCIAL_APP= "default_social_app";
    public static final String TELEGRAM = "Telegram";
    public static final String SIGNAL = "Signal";
    public static final String WHATSAPP = "Whatsapp";

    public static final String SHOULD_AUTO_CANCEL_MISSED_CALL_NOTIF_SHARED_PREF_KEY = "SHOULD_AUTO_CANCEL_MISSED_CALL_NOTIF";
    public static final String SHOULD_SHOW_BOTTOM_MENU_SHARED_PREF_KEY = "SHOULD_SHOW_BOTTOM_MENU";
    public static final String ENABLE_CALL_FILTERING_SHARED_PREF_KEY = "enableCallFiltering";
    public static final String CALL_FILTER_REJECT_CALLS_SHARED_PREF_KEY = "rejectCalls";

    public static String getDefaultSocialCountryCode(Context context) {
        return getAppsSharedPreferences(context)
            .getString(DEFAULT_SOCIAL_COUNTRY_CODE_PREFERENCES_KEY, "");

    }

    public static SharedPreferences getAppsSharedPreferences(Context context) {
        return context.getSharedPreferences(COMMON_SHARED_PREFS_FILE_NAME, MODE_PRIVATE);
    }

    public static SharedPreferences getContactsDataSharePreferences(Context context) {
        return context.getSharedPreferences(DATA_SHARE_SHARED_PREFS_FILE_NAME, MODE_PRIVATE);
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

    public static void enableSocialappIntegration(String selectedCountryCodeWithPlus, Context context) {
        getAppsSharedPreferences(context)
            .edit()
            .putString(DEFAULT_SOCIAL_COUNTRY_CODE_PREFERENCES_KEY, selectedCountryCodeWithPlus)
            .putBoolean(SOCIAL_INTEGRATION_ENABLED_PREFERENCE_KEY, true)
            .apply();
    }

    public static void disableSocialIntegration(Context context) {
        getAppsSharedPreferences(context)
            .edit()
            .putBoolean(SOCIAL_INTEGRATION_ENABLED_PREFERENCE_KEY, false)
            .apply();
    }

    public static boolean isSocialIntegrationEnabled(Context context) {
        return getAppsSharedPreferences(context)
            .getBoolean(SOCIAL_INTEGRATION_ENABLED_PREFERENCE_KEY, false);
    }

    public static String defaultSocialAppEnabled(Context context) {
        return getAppsSharedPreferences(context)
            .getString(DEFAULT_SOCIAL_APP, TELEGRAM);
    }
    public static void setSharedPreferencesChangeListener(SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener, Context context) {
        getAppsSharedPreferences(context).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    public static boolean is12HourFormatEnabled(Context context) {
        return getBoolean(PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY, true, context);
    }

    public static boolean isT9SearchEnabled(Context context) {
        return getBoolean(T9_SEARCH_ENABLED_SHARED_PREF_KEY, true, context);
    }

    public static boolean isT9PinyinEnabled(Context context) {
        return getBoolean(T9_PINYIN_ENABLED_SHARED_PREF_KEY, false, context);
    }

    public static String getLastSavedCallLogDate(Context context) {
        return getStringFromPreferences(LAST_CALL_LOG_READ_TIMESTAMP_SHARED_PREF_KEY, "0", context);
    }

    public static void setLastSavedCallLogDate(String date, Context context) {
        updatePreference(LAST_CALL_LOG_READ_TIMESTAMP_SHARED_PREF_KEY, date, context);
    }

    public static String getPreferredSim(Context context) {
        return getStringFromPreferences(SIM_PREFERENCE_SHARED_PREF_KEY, DEFAULT_SIM_SELECTION_SYSTEM_DEFAULT, context);
    }

    public static boolean shouldExportContactsEveryWeek(Context context) {
        return getBoolean(EXPORT_CONTACTS_EVERY_WEEK_SHARED_PREF_KEY, true, context);
    }

    public static boolean hasItBeenAWeekSinceLastExportOfContacts(Context context) {
        long lastExportTimeStamp = getAppsSharedPreferences(context).getLong(LAST_EXPORT_TIME_STAMP, 0);
        return hasItBeen(WEEKS_TIME_IN_HOURS, HOUR, lastExportTimeStamp);
    }

    public static String getEncryptingContactsKey(Context context) {
        return getStringFromPreferences(ENCRYPTING_CONTACTS_EXPORT_KEY, context);
    }

    public static boolean hasEncryptingContactsKey(Context context) {
        return !isEmpty(getEncryptingContactsKey(context));
    }

    public static void markAutoExportComplete(Context context) {
        updatePreference(LAST_EXPORT_TIME_STAMP, new Date().getTime(), context);
    }

    public static boolean shouldSortUsingFirstName(Context context) {
        return getBoolean(SORT_USING_FIRST_NAME, true, context);
    }

    public static boolean shouldLockToPortrait(Context context) {
        return getBoolean(LOCK_TO_PORTRAIT, true, context);
    }

    public static boolean shouldAskForPermissions(Context context) {
        return getBoolean(SHOULD_ASK_FOR_PERMISSIONS, true, context);
    }

    public static void markPermissionsAksed(Context context) {
        updatePreference(SHOULD_ASK_FOR_PERMISSIONS, false, context);
    }

    public static boolean shouldLaunchDefaultTab(Context context) {
        boolean shouldLaunchDefaultTab = hasItBeen(5, MINUTE, getLong(LAST_DEFAULT_TAB_LAUNCH_TIME_SHARED_PREF_KEY, 0, context));
        updatePreference(LAST_DEFAULT_TAB_LAUNCH_TIME_SHARED_PREF_KEY, new Date().getTime(), context);
        return shouldLaunchDefaultTab;
    }

    public static int getDefaultTab(Context context) {
        return Integer.parseInt(getStringFromPreferences(DEFAULT_TAB_SHARED_PREF_KEY, "0", context));
    }

    public static boolean shouldToggleContactActions(Context context) {
        return getBoolean(TOGGLE_CONTACT_ACTIONS, false, context);
    }

    public static boolean shouldUseSystemCallingApp(Context context) {
        return getBoolean(SHOULD_USE_SYSTEM_PHONE_APP, false, context);
    }

    public static boolean dynamicShortcutsAddedAlready(Context context) {
        return BuildConfig.VERSION_NAME.equals(getStringFromPreferences(SHORTCUTS_ADDED_IN_VERSION_SHARED_PREF_KEY, context));
    }

    public static void markAddedDynamicShortcuts(Context context) {
        updatePreference(SHORTCUTS_ADDED_IN_VERSION_SHARED_PREF_KEY, BuildConfig.VERSION_NAME, context);
    }

    public static boolean shouldKeyboardResizeViews(Context context) {
        return getBoolean(KEYBOARD_RESIZE_VIEWS_SHARED_PREF_KEY, false, context);
    }

    public static boolean shouldBottomMenuOpenByDefault(Context context) {
        return getBoolean(BOTTOM_MENU_OPEN_DEFAULT_SHARED_PREF_KEY, true, context);
    }

    public static String getLastVisistedGroup(Context context) {
        return getStringFromPreferences(LAST_VISITED_GROUP_SHARED_PREF_KEY, "", context);
    }

    public static void setLastVisistedGroup(String groupName, Context context) {
        updatePreference(LAST_VISITED_GROUP_SHARED_PREF_KEY, groupName, context);
    }

    public static boolean shouldAutoCancelMissedCallNotification(Context context) {
        return getBoolean(SHOULD_AUTO_CANCEL_MISSED_CALL_NOTIF_SHARED_PREF_KEY, false, context);
    }

    public static boolean shouldShowBottomMenu(Context context) {
        return getBoolean(SHOULD_SHOW_BOTTOM_MENU_SHARED_PREF_KEY, true, context);
    }

    public static void removeSyncProgress(Context context) {
        updatePreference(SYNC_TOKEN_SHARED_PREF_KEY, "", context);
    }

    public static void enableCallFiltering(Context context) {
        updatePreference(ENABLE_CALL_FILTERING_SHARED_PREF_KEY, true, context);
    }

    public static boolean isCallFilteringEnabled(Context context) {
        return getBoolean(ENABLE_CALL_FILTERING_SHARED_PREF_KEY, false, context);
    }

    public static boolean shouldBlockCalls(Context context) {
        return getBoolean(CALL_FILTER_REJECT_CALLS_SHARED_PREF_KEY, false, context);
    }

    private static String authCodeKey(String packageName) {
        return packageName + "-auth";
    }

    private static String permissionsKeyForPackage(String packageName) {
        return packageName + "-permissions";
    }

    public static boolean isValidAuthCode(Context context, String packageName, String authCode) {
        return Objects.equals(getContactsDataSharePreferences(context).getString(authCodeKey(packageName), ""), authCode);
    }

    public static Set<String> permissions(Context context, String packageName) {
        return getContactsDataSharePreferences(context).getStringSet(permissionsKeyForPackage(packageName), emptySet());
    }

    public static void savePermissionsGranted(Context context, String packageName, List<String> permissions) {
        SharedPreferences contactsDataSharePreferences = getContactsDataSharePreferences(context);
        String permissionsKeyForPackage = permissionsKeyForPackage(packageName);
        Set<String> permissionsSet = new HashSet<>(contactsDataSharePreferences.getStringSet(permissionsKeyForPackage, new HashSet<>()));
        permissionsSet.addAll(permissions);
        contactsDataSharePreferences.edit().putStringSet(permissionsKeyForPackage, permissionsSet).apply();
    }

    public static void saveAuthCode(Context context, String packageName, String authCode) {
        getContactsDataSharePreferences(context).edit().putString(authCodeKey(packageName), authCode).apply();
    }

}
