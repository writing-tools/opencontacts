package opencontacts.open.com.opencontacts;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.utils.CrashUtils;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

public class OpenContactsApplication extends com.orm.SugarApp{

    public static final String MISSED_CALLS_CHANEL_ID = "6477";

    @Override
    public void onCreate() {
        super.onCreate();
        ContactsDataStore.init();
        CallLogDataStore.init(getApplicationContext());
        createNotificationChannels();
        CrashUtils.setUpCrashHandler(getApplicationContext());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(MISSED_CALLS_CHANEL_ID, getString(R.string.notification_channel_missed_calls), IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.notification_channel_missed_calls_description));
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }
}
