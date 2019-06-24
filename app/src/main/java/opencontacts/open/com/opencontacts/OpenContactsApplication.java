package opencontacts.open.com.opencontacts;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import opencontacts.open.com.opencontacts.activities.CrashReportingActivity;
import opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

public class OpenContactsApplication extends com.orm.SugarApp{

    public static final String MISSED_CALLS_CHANEL_ID = "6477";

    @Override
    public void onCreate() {
        super.onCreate();
        ContactsDataStore.init();
        CallLogDataStore.init(getApplicationContext());
        createNotificationChannels();
        setCrashHandler();
    }

    private void setCrashHandler() {
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            scheduleCrashReporting(e);
            defaultUncaughtExceptionHandler.uncaughtException(t, e);
            System.exit(1);
        });
    }

    private void scheduleCrashReporting(Throwable e) {
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        if(alarmManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, PendingIntent.getActivity(getApplicationContext(), 123, new Intent(getApplicationContext(), CrashReportingActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("Exception", e), 0));
        }else alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, PendingIntent.getActivity(getApplicationContext(), 123, new Intent(getApplicationContext(), CrashReportingActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("Exception", e), 0));
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
