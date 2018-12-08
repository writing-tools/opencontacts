package opencontacts.open.com.opencontacts.utils;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;


import java.util.ArrayList;

import opencontacts.open.com.opencontacts.activities.AddToContactActivity;
import opencontacts.open.com.opencontacts.activities.ContactDetailsActivity;
import opencontacts.open.com.opencontacts.activities.EditContactActivity;
import opencontacts.open.com.opencontacts.activities.MainActivity;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;

import static android.content.Intent.ACTION_VIEW;

/**
 * Created by sultanm on 7/17/17.
 */

public class AndroidUtils {

    public static final String ONE_HAND_MODE_ENABLED = "ONE_HAND_MODE_ENABLED";
    public static final String IS_LIGHT_THEME_ACTIVE_PREFERENCES_KEY = "IS_LIGHT_THEME_ACTIVE_PREFERENCES_KEY";
    public static final String DEFAULT_WHATSAPP_COUNTRY_CODE_PREFERENCES_KEY = "DEFAULT_WHATSAPP_COUNTRY_CODE";
    private static Handler mainThreadHandler;

    public static float dpToPixels(int dp) {
        return Resources.getSystem().getDisplayMetrics().density * dp;
    }

    public static void processAsync(final Runnable someRunnable){
        new Thread(){
            @Override
            public void run() {
                someRunnable.run();
            }
        }.start();
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
        Intent callIntent = getCallIntent(number, context);
        context.startActivity(callIntent);
    }

    public static void whatsapp(String number, Context context) {
        try{
            context.startActivity(getWhatsappIntent(number, context));
        }
        catch (Exception e){
            Toast.makeText(context, context.getString(R.string.could_not_open_whatsapp), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @NonNull
    private static Intent getWhatsappIntent(String number, Context context) {
        String numberWithCountryCode = number.contains("+") ? number : getDefaultWhatsAppCountryCode(context) + number;
        return new Intent(ACTION_VIEW, Uri.parse(
                context.getString(R.string.whatsapp_uri_with_phone_number_placeholder, numberWithCountryCode)
        ));
    }

    public static String getDefaultWhatsAppCountryCode(Context context) {
        return getAppsSharedPreferences(context)
                .getString(DEFAULT_WHATSAPP_COUNTRY_CODE_PREFERENCES_KEY, "");

    }

    @NonNull
    public static Intent getCallIntent(String number, Context context) {
        Uri numberUri = Uri.parse("tel:" + number);
        Intent callIntent;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            callIntent = new Intent(Intent.ACTION_DIAL, numberUri);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        else{
            callIntent = new Intent(Intent.ACTION_CALL, numberUri);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return callIntent;
    }

    public static void message(String number, Context context){
        context.startActivity(getMessageIntent(number));
    }

    public static Intent getMessageIntent(String number) {
        return new Intent(ACTION_VIEW, Uri.parse("sms:" + number)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent getIntentToShowContactDetails(long contactId, Context context){
     return new Intent(context, ContactDetailsActivity.class)
                    .putExtra(MainActivity.INTENT_EXTRA_LONG_CONTACT_ID, contactId);
    }
    public static SharedPreferences getAppsSharedPreferences(Context context){
        return context.getSharedPreferences(context.getString(R.string.app_name), context.MODE_PRIVATE);
    }

    public static Intent getIntentToAddContact(String phoneNumber, Context context){
        return new Intent(context, EditContactActivity.class)
            .putExtra(EditContactActivity.INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, true)
            .putExtra(EditContactActivity.INTENT_EXTRA_STRING_PHONE_NUMBER, phoneNumber);
    }

    public static Intent getIntentToLaunchAddToContactActivity(String phoneNumber, Context context){
        return new Intent(context, AddToContactActivity.class)
                .putExtra(EditContactActivity.INTENT_EXTRA_STRING_PHONE_NUMBER, phoneNumber)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    }

    @NonNull
    public static Intent getIntentToExportContactToNativeContactsApp(Contact contact) {
        Intent exportToContactsAppIntent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);

        ArrayList<ContentValues> data = new ArrayList<>();
        for(String phoneNumber : contact.phoneNumbers){
            ContentValues row = new ContentValues();
            row.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            row.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
            data.add(row);
        }
        exportToContactsAppIntent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data)
                .putExtra(ContactsContract.Intents.Insert.NAME, contact.name);
        return exportToContactsAppIntent;
    }


    public static void showAlert(Context context, String title, String message){
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

    public static void copyToClipboard(String text, Context context) {

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

    public static void setBackButtonInToolBar(Toolbar toolBar, final AppCompatActivity appCompatActivity){
        toolBar.setNavigationOnClickListener(v -> appCompatActivity.onBackPressed());
    }

    public static android.app.AlertDialog getAlertDialogToAddContact(final String phoneNumber, final Context context){
        return new android.app.AlertDialog.Builder(context)
                .setItems(new CharSequence[]{context.getString(R.string.create_new_contact), context.getString(R.string.add_to_existing)}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                context.startActivity(AndroidUtils.getIntentToAddContact(phoneNumber, context));
                                break;
                            case 1:
                                context.startActivity(AndroidUtils.getIntentToLaunchAddToContactActivity(phoneNumber, context));
                                break;
                        }
                    }
                })
                .create();
    }

    public static void askForPermissionsIfNotGranted(final AppCompatActivity activity) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.canDrawOverlays(activity)) {
                new AlertDialog.Builder(activity)
                        .setTitle("Enable draw over apps")
                        .setMessage("This will allow app to show the calling person's name on screen during call")
                        .setNeutralButton(R.string.okay, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                activity.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName())));
                            }
                        })
                        .create()
                        .show();
            }
            if(activity.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED){
                new AlertDialog.Builder(activity)
                        .setTitle("Grant phone permission")
                        .setMessage("Grant manage phone permission to be able to read call log")
                        .setNeutralButton(R.string.okay, null)
                        .setOnDismissListener(dialog -> activity.requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, 123))
                        .create()
                        .show();
            }
            if(activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                new AlertDialog.Builder(activity)
                        .setTitle("Grant storage permission")
                        .setMessage("Grant storage phone permission to be able to export and import contacts")
                        .setNeutralButton(R.string.okay, null)
                        .setOnDismissListener(dialog -> activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123))
                        .create()
                        .show();
            }
        }
    }

    public static void saveCallerIdLocationOnScreen(int x, int y, Context context) {
        getAppsSharedPreferences(context)
                .edit()
                .putInt("CALLER_ID_X_POSITION_ON_SCREEN", x)
                .putInt("CALLER_ID_Y_POSITION_ON_SCREEN", y)
                .apply();
    }
    public static Point getCallerIdLocationOnScreen(Context context) {
        return new Point(getAppsSharedPreferences(context).getInt("CALLER_ID_X_POSITION_ON_SCREEN", 0), getAppsSharedPreferences(context).getInt("CALLER_ID_Y_POSITION_ON_SCREEN", 100));
    }

    public static void goToUrl (String url, Context context) {
        Uri uri = Uri.parse(url);
        context.startActivity(new Intent(ACTION_VIEW, uri));
    }

    public static Handler getMainThreadHandler(){
        if(mainThreadHandler == null)
            mainThreadHandler = new Handler(Looper.getMainLooper());
        return mainThreadHandler;
    }

    public static void switchActiveThemeInPreferences(Context context) {
        getAppsSharedPreferences(context)
                .edit()
                .putBoolean(IS_LIGHT_THEME_ACTIVE_PREFERENCES_KEY, !isLightThemeActive(context))
                .apply();
    }

    public static int getCurrentTheme(Context context) {
        return isLightThemeActive(context) ? R.style.Theme_AppCompat_Light_NoActionBar : R.style.Theme_AppCompat_NoActionBar;
    }

    private static boolean isLightThemeActive(Context context) {
        return getAppsSharedPreferences(context).getBoolean(IS_LIGHT_THEME_ACTIVE_PREFERENCES_KEY, true);
    }

    public static void applyOptedTheme(Context context) {
        context.getTheme().applyStyle(AndroidUtils.getCurrentTheme(context), true);
    }

    public static void saveDefaultWhatsAppCountryCode(String selectedCountryCodeWithPlus, Context context) {
        getAppsSharedPreferences(context)
                .edit()
                .putString(DEFAULT_WHATSAPP_COUNTRY_CODE_PREFERENCES_KEY, selectedCountryCodeWithPlus)
                .apply();
    }
}
