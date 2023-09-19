package opencontacts.open.com.opencontacts.utils;

import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.Intent.ACTION_SENDTO;
import static android.content.Intent.ACTION_VIEW;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static android.provider.CalendarContract.Events.ALL_DAY;
import static android.provider.CalendarContract.Events.CONTENT_URI;
import static android.provider.CalendarContract.Events.TITLE;
import static android.text.TextUtils.isEmpty;
import static java.lang.Math.round;
import static opencontacts.open.com.opencontacts.utils.Common.removeSpacesIfAny;
import static opencontacts.open.com.opencontacts.utils.PhoneCallUtils.handleMultiSimCalling;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.SIGNAL;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.TELEGRAM;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.defaultSocialAppEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getAppsSharedPreferences;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getDefaultSocialCountryCode;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldUseSystemCallingApp;
import static opencontacts.open.com.opencontacts.utils.ThemeUtils.getPrimaryColor;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Space;
import android.widget.Toast;

import com.github.underscore.Consumer;
import com.github.underscore.U;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.activities.AddToContactActivity;
import opencontacts.open.com.opencontacts.activities.ContactDetailsActivity;
import opencontacts.open.com.opencontacts.activities.EditContactActivity;
import opencontacts.open.com.opencontacts.activities.MainActivity;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;

/**
 * Created by sultanm on 7/17/17.
 */

public class AndroidUtils {

    public static final String ONE_HAND_MODE_ENABLED = "ONE_HAND_MODE_ENABLED";
    public static final int DRAW_OVERLAY_PERMISSION_RESULT = 3729;
    public static final String EMAIL_SCHEME = "mailto:";
    public static final String MAP_LOCATION_URI = "geo:0,0?q=";
    private static Handler mainThreadHandler;

    public static float dpToPixels(int dp) {
        return Resources.getSystem().getDisplayMetrics().density * dp;
    }

    public static float pixelsToSP(float px) {
        return px / Resources.getSystem().getDisplayMetrics().scaledDensity;
    }

    public static int dpToPixelsClosestInt(int dp) {
        return round(dpToPixels(dp));
    }

    public static void processAsync(final Runnable someRunnable) {
        new Thread() {
            @Override
            public void run() {
                someRunnable.run();
            }
        }.start();
    }

    public static void runOnMainDelayed(final Runnable someRunnable, long delayInMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(someRunnable, delayInMillis);
    }

    public static void showSoftKeyboard(View view, Context context) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static void hideSoftKeyboard(View view, Context context) {
        InputMethodManager imm = (InputMethodManager)
            context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void call(String number, Context context) {
        if (PhoneCallUtils.hasMultipleSims(context)) {
            handleMultiSimCalling(number, context);
            return;
        }
        callWithSystemDefaultSim(number, context);
    }

    public static void callWithSystemDefaultSim(String number, Context context) {
        Intent callIntent = getIntentToCall(number, context);
        context.startActivity(callIntent);
    }

    public static void signal(String number, Context context) {
        try {
            context.startActivity(getSignalIntent(number, context));
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.could_not_open_social_app), Toast.LENGTH_LONG)
                .show();
        }
    }

    public static void telegram(String number, Context context) {
        try {
            context.startActivity(getTelegramIntent(number, context));
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.could_not_open_social_app), Toast.LENGTH_LONG)
                .show();
        }
    }

    public static void openSocialApp(String number, Context context) {
        String socialApp = defaultSocialAppEnabled(context);
        if(socialApp.equals(TELEGRAM)) telegram(number, context);
        else if(socialApp.equals(SIGNAL)) signal(number, context);
        else whatsapp(number, context);
    }

    // boolean return type for helping long press signature supposed to be returning boolean
    public static boolean onSocialLongPress(String phoneNumber, Context context) {
        String[] social_apps = context.getResources().getStringArray(R.array.social_integrations);
        new AlertDialog.Builder(context)
            .setItems(social_apps,
                (dialog, which) -> {
                    switch (which) {
                        case 0: telegram(phoneNumber, context);
                            break;
                        case 1: whatsapp(phoneNumber, context);
                            break;
                        case 2: signal(phoneNumber, context);
                            break;
                    }
                }).show();
        return true;
    }

    public static void whatsapp(String number, Context context) {
        try {
            context.startActivity(getWhatsappIntent(number, context));
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.could_not_open_social_app), Toast.LENGTH_LONG)
                .show();
        }
    }

    public static boolean isAppInstalled(String packageName, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    @NonNull
    private static Intent getSignalIntent(String number, Context context) {
        String numberWithCountryCode = number.contains("+") ? number : getDefaultSocialCountryCode(context) + number;
        return new Intent(ACTION_VIEW, Uri.parse(
            context.getString(R.string.signal_uri_with_phone_number_placeholder, removeSpacesIfAny(numberWithCountryCode))
        ));
    }

    @NonNull
    private static Intent getTelegramIntent(String number, Context context) {
        String numberWithCountryCode = number.contains("+") ? number : getDefaultSocialCountryCode(context) + number;
        return new Intent(ACTION_VIEW, Uri.parse(
            context.getString(R.string.telegram_uri_with_phone_number_placeholder, removeSpacesIfAny(numberWithCountryCode))
        ));
    }
    @NonNull
    private static Intent getWhatsappIntent(String number, Context context) {
        String numberWithCountryCode = number.contains("+") ? number : getDefaultSocialCountryCode(context) + number;
        return new Intent(ACTION_VIEW, Uri.parse(
            context.getString(R.string.whatsapp_uri_with_phone_number_placeholder, removeSpacesIfAny(numberWithCountryCode))
        ));
    }

    @NonNull
    public static Intent getIntentToCall(String number, Context context) {
        Uri numberUri = Uri.parse("tel:" + Uri.encode(number));
        Intent callIntent;
        if (hasPermission(Manifest.permission.CALL_PHONE, context)) {
            callIntent = new Intent(Intent.ACTION_CALL, numberUri);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            callIntent = new Intent(Intent.ACTION_DIAL, numberUri);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (shouldUseSystemCallingApp(context)) {
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(callIntent, PackageManager.MATCH_SYSTEM_ONLY);
            ActivityInfo targetActivityInfo = resolveInfo.activityInfo;
            callIntent.setComponent(new ComponentName(targetActivityInfo.packageName, targetActivityInfo.name));
        }
        return callIntent;
    }

    public static void message(String number, Context context) {
        context.startActivity(getIntentToMessage(number));
    }

    public static void message(Collection<String> numbers, Context context) {
        context.startActivity(getIntentToMessage(numbers));
    }

    public static Intent getIntentToMessage(String number) {
        return new Intent(ACTION_VIEW, Uri.parse("sms:" + number)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getIntentToMessage(Collection<String> numbers) {
        String numbersSeperatedBySemiColon = U.chain(numbers)
            .reject(TextUtils::isEmpty)
            .join(";")
            .item();
        return new Intent(ACTION_VIEW, Uri.parse("sms:" + numbersSeperatedBySemiColon)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getIntentToShowContactDetails(long contactId, Context context) {
        return new Intent(context, ContactDetailsActivity.class)
            .putExtra(MainActivity.INTENT_EXTRA_LONG_CONTACT_ID, contactId);
    }

    public static Intent getIntentToAddContact(Intent intent, Context context) {
        Bundle extras = intent.getExtras();
        return new Intent(context, EditContactActivity.class)
            .putExtra(EditContactActivity.INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, true)
            .putExtras(extras == null ? new Bundle() : extras);
    }

    public static Intent getIntentToAddContact(String phoneNumber, Context context) {
        return new Intent(context, EditContactActivity.class)
            .putExtra(EditContactActivity.INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, true)
            .putExtra(EditContactActivity.INTENT_EXTRA_STRING_PHONE_NUMBER, phoneNumber);
    }

    public static Intent getIntentToLaunchAddToContactActivity(String phoneNumber, Context context) {
        return new Intent(context, AddToContactActivity.class)
            .putExtra(EditContactActivity.INTENT_EXTRA_STRING_PHONE_NUMBER, phoneNumber)
            .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    }

    @NonNull
    public static Intent getIntentToExportContactToNativeContactsApp(Contact contact) {
        Intent exportToContactsAppIntent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);

        ArrayList<ContentValues> data = new ArrayList<>();
        for (PhoneNumber phoneNumber : contact.phoneNumbers) {
            ContentValues row = new ContentValues();
            row.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            row.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber.phoneNumber);
            data.add(row);
        }
        exportToContactsAppIntent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data)
            .putExtra(ContactsContract.Intents.Insert.NAME, contact.name);
        return exportToContactsAppIntent;
    }


    public static void showAlert(Context context, String title, String message) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.okay, null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    public static void copyToClipboard(CharSequence text, Context context) {

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                .getSystemService(context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                .getSystemService(context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                .newPlainText(null, text);
            clipboard.setPrimaryClip(clip);
        }

    }

    public static void wrapInConfirmation(Runnable onYes, Context context) {
        wrapInConfirmation(onYes, () -> {}, R.string.are_you_sure, context);
    }

    public static void wrapInConfirmation(Runnable onYes, Runnable onNo, @StringRes int confirmationTextRes, Context context) {
        new AlertDialog.Builder(context)
            .setMessage(confirmationTextRes)
            .setPositiveButton(R.string.okay,
                (dialogInterface, i) -> onYes.run())
            .setNegativeButton(R.string.no, (dialogInterface, i) -> onNo.run())
            .show();
    }

    public static void copyToClipboard(CharSequence text, boolean shouldShowToast, Context context) {
        copyToClipboard(text, context);
        if (shouldShowToast)
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public static void setBackButtonInToolBar(Toolbar toolBar, final AppCompatActivity appCompatActivity) {
        toolBar.setNavigationOnClickListener(v -> appCompatActivity.onBackPressed());
        Drawable navigationIcon = toolBar.getNavigationIcon();
        if (navigationIcon == null)
            return;
        setColorFilterUsingColor(navigationIcon, getPrimaryColor(appCompatActivity));
    }

    public static android.app.AlertDialog getAlertDialogToAddContact(final String phoneNumber, final Context context) {
        return new android.app.AlertDialog.Builder(context)
            .setItems(new CharSequence[]{context.getString(R.string.create_new_contact), context.getString(R.string.add_to_existing)}, (dialog, which) -> {
                switch (which) {
                    case 0:
                        context.startActivity(AndroidUtils.getIntentToAddContact(phoneNumber, context));
                        break;
                    case 1:
                        context.startActivity(AndroidUtils.getIntentToLaunchAddToContactActivity(phoneNumber, context));
                        break;
                }
            })
            .create();
    }

    public static void askForPermissionsIfNotGranted(final AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                new AlertDialog.Builder(activity)
                    .setTitle(R.string.enable_draw_over_apps)
                    .setMessage(R.string.enable_draw_over_apps_detail)
                    .setNeutralButton(R.string.okay, null)
                    .setOnDismissListener(dialog -> activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName())), DRAW_OVERLAY_PERMISSION_RESULT))
                    .create()
                    .show();
            }
            if (activity.checkSelfPermission(READ_CALL_LOG) != PERMISSION_GRANTED || activity.checkSelfPermission(READ_PHONE_STATE) != PERMISSION_GRANTED || activity.checkSelfPermission(CALL_PHONE) != PERMISSION_GRANTED) {
                new AlertDialog.Builder(activity)
                    .setTitle(R.string.grant_phone_permission)
                    .setMessage(R.string.grant_phone_permission_detail)
                    .setNeutralButton(R.string.okay, null)
                    .setOnDismissListener(dialog -> activity.requestPermissions(new String[]{READ_CALL_LOG, READ_PHONE_STATE, CALL_PHONE}, 123))
                    .create()
                    .show();
            }
            if (activity.checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                new AlertDialog.Builder(activity)
                    .setTitle(R.string.grant_storage_permission)
                    .setMessage(R.string.grant_storage_permisson_detail)
                    .setNeutralButton(R.string.okay, null)
                    .setOnDismissListener(dialog -> activity.requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 123))
                    .create()
                    .show();
            }
        }
    }

    public static boolean doesNotHaveAllPermissions(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;
        return ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context, READ_CALL_LOG) != PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) != PERMISSION_GRANTED
            || !Settings.canDrawOverlays(context);

    }

    public static void goToUrl(String url, Context context) {
        try {
            Uri uri = Uri.parse(url);
            if (isEmpty(uri.getScheme())) uri = Uri.parse("https://" + uri.toString());
            context.startActivity(new Intent(ACTION_VIEW, uri));
        } catch (Exception e) {
            toastFromNonUIThread(R.string.invalid_url, Toast.LENGTH_LONG, context);
        }
    }

    public static Handler getMainThreadHandler() {
        if (mainThreadHandler == null)
            mainThreadHandler = new Handler(Looper.getMainLooper());
        return mainThreadHandler;
    }

    public static int getThemeAttributeColor(int attribute, Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attribute, typedValue, true);
        if (typedValue.resourceId == 0)
            return typedValue.data;
        return ContextCompat.getColor(context, typedValue.resourceId);
    }

    public static void setColorFilterUsingColor(Drawable drawable, int color) {
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    public static void showAlert(Context context, int titleRes, int messageRes) {
        showAlert(context, context.getString(titleRes), context.getString(messageRes));
    }

    public static void email(String emailAddress, Context context) {
        Intent emailAppChooserIntent = Intent.createChooser(
            new Intent(ACTION_SENDTO, Uri.parse(EMAIL_SCHEME + emailAddress))
            , context.getString(R.string.email));
        context.startActivity(emailAppChooserIntent);
    }

    public static boolean isScreenLocked(Context context) {
        return ((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode();
    }

    public static boolean isWhatsappInstalled(Context context) {
        String whatsappPackageName = "com.whatsapp";
        String whatsappBusinessPackageName = "com.whatsapp.w4b";
        return isAppInstalled(whatsappPackageName, context) || isAppInstalled(whatsappBusinessPackageName, context);
    }

    public static void toastFromNonUIThread(int messageRes, int length, Context context) {
        getMainThreadHandler().post(() -> Toast.makeText(context, context.getString(messageRes), length).show());
    }

    public static void toastFromNonUIThread(String message, int length, Context context) {
        getMainThreadHandler().post(() -> Toast.makeText(context, message, length).show());
    }

    public static String getStringFromPreferences(String key, Context context) {
        return getStringFromPreferences(key, null, context);
    }

    public static String getStringFromPreferences(String key, String defaultValue, Context context) {
        return getAppsSharedPreferences(context).getString(key, defaultValue);
    }

    public static float getFloatFromPreferences(String key, float defaultValue, Context context) {
        return getAppsSharedPreferences(context).getFloat(key, defaultValue);
    }

    public static void updatePreference(String key, String value, Context context) {
        getAppsSharedPreferences(context)
            .edit()
            .putString(key, value)
            .apply();
    }

    public static void updatePreference(String key, boolean value, Context context) {
        getAppsSharedPreferences(context)
            .edit()
            .putBoolean(key, value)
            .apply();
    }

    public static void updatePreference(String key, long value, Context context) {
        getAppsSharedPreferences(context)
            .edit()
            .putLong(key, value)
            .apply();
    }

    public static void updatePreference(String key, float value, Context context) {
        getAppsSharedPreferences(context)
            .edit()
            .putFloat(key, value)
            .apply();
    }

    public static boolean getBoolean(String key, boolean defaultValue, Context context) {
        return getAppsSharedPreferences(context).getBoolean(key, defaultValue);
    }

    public static long getLong(String key, long defaultValue, Context context) {
        return getAppsSharedPreferences(context).getLong(key, defaultValue);
    }

    public static void toggleBoolean(String key, boolean defaultValue, Context context) {
        updatePreference(key
            , !getAppsSharedPreferences(context).getBoolean(key, defaultValue)
            , context);

    }

    @NonNull
    public static Intent getIntentToAddFullDayEventOnCalendar(Date birthday, String title) {
        java.util.Calendar dobCalendarInstance = Common.getCalendarInstanceAt(birthday.getTime());
        return new Intent(Intent.ACTION_INSERT)
            .setData(CONTENT_URI)
            .putExtra(EXTRA_EVENT_BEGIN_TIME, dobCalendarInstance.getTimeInMillis())
            .putExtra(EXTRA_EVENT_END_TIME, dobCalendarInstance.getTimeInMillis())
            .putExtra(TITLE, title)
            .putExtra(ALL_DAY, true);
    }

    public static String getFormattedDate(Date date) {
        return SimpleDateFormat.getDateInstance().format(date);
    }

    public static void openMap(String address, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(MAP_LOCATION_URI + Uri.encode(address)));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else
            Toast.makeText(context, R.string.no_app_found_to_open, Toast.LENGTH_SHORT).show();
    }

    public static void shareContact(String vcardAsString, Context context) {
        File tempFile;
        try {
            tempFile = File.createTempFile("tempcontact", ".vcf");
            Writer fileWriter = new FileWriter(tempFile)
                .append(vcardAsString);
            fileWriter.flush();
            fileWriter.close();
            tempFile.deleteOnExit();
        } catch (IOException e) {
            toastFromNonUIThread("Error sharing a contact", Toast.LENGTH_LONG, context);
            return;
        }
        context.startActivity(new ShareCompat.IntentBuilder(context)
            .addStream(FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), tempFile))
            .setType("text/x-vcard")
            .getIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        );
    }

    public static void shareText(String text, Context context) {
        context.startActivity(new ShareCompat.IntentBuilder(context)
            .setText(text)
            .setType("text/plain")
            .getIntent());
    }

    public static boolean hasPermission(String permission, Context context) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isAddContactIntent(Intent intent) {
        return ("vnd.android.cursor.dir/contact".equals(intent.getType()) || intent.getAction() == Intent.ACTION_INSERT) && (intent.getStringExtra(ContactsContract.Intents.Insert.PHONE) != null);
    }

    public static boolean isValidDialIntent(Intent intent) {
        return intent.getData() != null && "tel".equals(intent.getScheme());
    }

    public static String getNumberToDial(Intent intent) {
        return intent.getData() == null ? "" : intent.getData().getSchemeSpecificPart();
    }

    public static void blockUIUntil(Runnable operation, Context context) {
        AlertDialog loadingDialog = new AlertDialog.Builder(context)
            .setCancelable(false)
            .setMessage(R.string.please_wait)
            .show();
        processAsync(() -> {
            operation.run();
            runOnMainDelayed(loadingDialog::dismiss, 0);
        });
    }

    public static OnMenuItemClickListener getMenuItemClickHandlerFor(Runnable runnable) {
        return item -> {
            runnable.run();
            return true;
        };
    }

    public static Space getASpaceOfHeight(int widthInDP, int heightInDP, Context context) {
        Space space = new Space(context);
        space.setMinimumHeight(dpToPixelsClosestInt(widthInDP));
        space.setMinimumHeight(dpToPixelsClosestInt(heightInDP));
        return space;
    }

    public static <T> void handleLongClickWith(LinkedHashMap<String, Consumer<T>> longClickOptionsAndListeners, String[] options, T data, Context context) {
        new AlertDialog.Builder(context)
            .setItems(options, (dialog, which) -> {
                longClickOptionsAndListeners.get(options[which]).accept(data);
            }).show();

    }

    public static <T> void handleLongClickWith(LinkedHashMap<String, Consumer<T>> longClickOptionsAndListeners, T data, Context context) {
        String[] longClickOptions = longClickOptionsAndListeners.keySet().toArray(new String[0]);
        handleLongClickWith(longClickOptionsAndListeners, longClickOptions, data, context);
    }

    public static String getCountryCode(Context context) {
        try {
            TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return telephonyService.getNetworkCountryIso();
        }
        catch(Exception e) {
            e.printStackTrace();
            return Locale.getDefault().getCountry();
        }
    }
}
