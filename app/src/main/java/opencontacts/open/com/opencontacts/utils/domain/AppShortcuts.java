package opencontacts.open.com.opencontacts.utils.domain;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.graphics.drawable.IconCompat;

import java.util.Arrays;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.activities.EditContactActivity;
import opencontacts.open.com.opencontacts.activities.MainActivity;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

import static opencontacts.open.com.opencontacts.activities.MainActivity.DIALER_TAB_INDEX;

public class AppShortcuts {
    public static final String ADD_CONTACT_SHORTCUT_ID = "ADD_CONTACT_SHORTCUT_ID";
    public static final String DIALER_SHORTCUT_ID = "DIALER_SHORTCUT_ID";
    public static String TAB_INDEX_INTENT_EXTRA = "TAB_INDEX_INTENT_EXTRA";
    public static void addShortcutsIfNotAddedAlreadyAsync(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) return;
        AndroidUtils.runOnMainDelayed(() -> addDynamicShortcuts(context), 3000);
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private static void addDynamicShortcuts(Context context) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if(!shortcutManager.getDynamicShortcuts().isEmpty()) return;
        shortcutManager.addDynamicShortcuts(
                Arrays.asList(
                    getAddContactShortcut(context),
                    getDialerShortcut(context)
                )
        );
    }

    @NonNull
    private static ShortcutInfo getAddContactShortcut(Context context) {
        Intent addContactIntent = new Intent(context, EditContactActivity.class)
                .putExtra(EditContactActivity.INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, true)
                .setAction(Intent.ACTION_VIEW);
        return new ShortcutInfoCompat.Builder(context, ADD_CONTACT_SHORTCUT_ID)
                .setShortLabel(context.getString(R.string.add_contact))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_add_24dp))
                .setIntent(addContactIntent)
                .build()
                .toShortcutInfo();
    }

    @NonNull
    private static ShortcutInfo getDialerShortcut(Context context) {
        Intent dialerIntent = new Intent(context, MainActivity.class)
                .putExtra(TAB_INDEX_INTENT_EXTRA, DIALER_TAB_INDEX)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setAction(Intent.ACTION_VIEW);
        return new ShortcutInfoCompat.Builder(context, DIALER_SHORTCUT_ID)
                .setShortLabel(context.getString(R.string.dialer))
                .setIcon(IconCompat.createWithResource(context, R.drawable.dial_pad))
                .setIntent(dialerIntent)
                .build()
                .toShortcutInfo();
    }


}
