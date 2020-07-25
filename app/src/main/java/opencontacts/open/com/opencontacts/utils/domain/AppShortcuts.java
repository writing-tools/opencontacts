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

import java.util.Collections;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.activities.EditContactActivity;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

public class AppShortcuts {
    public static void addShortcutsIfNotAddedAlreadyAsync(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) return;
        AndroidUtils.runOnMainDelayed(() -> addDynamicShortcuts(context), 3000);
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private static void addDynamicShortcuts(Context context) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if(!shortcutManager.getDynamicShortcuts().isEmpty()) return;
        shortcutManager.addDynamicShortcuts(Collections.singletonList(getAddContactShortcut(context)));
    }

    @NonNull
    private static ShortcutInfo getAddContactShortcut(Context context) {
        Intent addContactIntent = new Intent(context, EditContactActivity.class)
                .putExtra(EditContactActivity.INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, true)
                .setAction(Intent.ACTION_VIEW);
        return new ShortcutInfoCompat.Builder(context, context.getString(R.string.add_contact))
                .setShortLabel(context.getString(R.string.add_contact))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_add_24dp))
                .setIntent(addContactIntent)
                .build()
                .toShortcutInfo();
    }


}
