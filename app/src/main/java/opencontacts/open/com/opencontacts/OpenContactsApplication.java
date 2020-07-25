package opencontacts.open.com.opencontacts;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.support.multidex.MultiDexApplication;

import com.orm.SugarContext;

import opencontacts.open.com.opencontacts.actions.AutoContactsExporter;
import opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.utils.CrashUtils;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static opencontacts.open.com.opencontacts.utils.domain.AppShortcuts.addShortcutsIfNotAddedAlreadyAsync;

public class OpenContactsApplication extends MultiDexApplication {

    public static final String MISSED_CALLS_CHANEL_ID = "6477";

    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);
        ContactsDataStore.init();
        CallLogDataStore.init(getApplicationContext());
        createNotificationChannels();
        CrashUtils.setUpCrashHandler(getApplicationContext());
        new AutoContactsExporter(this).exportContactsAsPerPreferences();
        addShortcutsIfNotAddedAlreadyAsync(getApplicationContext());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(MISSED_CALLS_CHANEL_ID, getString(R.string.notification_channel_missed_calls), IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.notification_channel_missed_calls_description));
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
    }
}
