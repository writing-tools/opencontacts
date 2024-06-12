package opencontacts.open.com.opencontacts.utils;

import static android.content.Context.ALARM_SERVICE;
import static opencontacts.open.com.opencontacts.activities.CrashReportingActivity.ERROR_CONTENT_BUNDLE_EXTRA_KEY;
import static opencontacts.open.com.opencontacts.activities.CrashReportingActivity.EXCEPTION_BUNDLE_EXTRA_KEY;
import static opencontacts.open.com.opencontacts.activities.CrashReportingActivity.IS_NOT_CRASH_BUNDLE_EXTRA_KEY;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import opencontacts.open.com.opencontacts.activities.CrashReportingActivity;

public class CrashUtils {
    public static Intent getIntentToReportCrash(Throwable throwable, Context context) {
        return new Intent(context, CrashReportingActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXCEPTION_BUNDLE_EXTRA_KEY, throwable);
    }

    public static Intent getIntentToReportError(Throwable throwable, Context context) {
        return new Intent(context, CrashReportingActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(IS_NOT_CRASH_BUNDLE_EXTRA_KEY, true)
            .putExtra(EXCEPTION_BUNDLE_EXTRA_KEY, throwable);
    }

    public static Intent getIntentToReportError(String errorContent, Context context) {
        return new Intent(context, CrashReportingActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(IS_NOT_CRASH_BUNDLE_EXTRA_KEY, true)
            .putExtra(ERROR_CONTENT_BUNDLE_EXTRA_KEY, errorContent);
    }

    public static void reportCrash(Throwable throwable, Context context) {
        context.startActivity(getIntentToReportCrash(throwable, context));
    }

    public static void reportError(Throwable throwable, Context context) {
        context.startActivity(getIntentToReportError(throwable, context));
    }

    public static void reportError(String errorContent, Context context) {
        context.startActivity(getIntentToReportError(errorContent, context));
    }

    public static void setUpCrashHandler(Context context) {
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            scheduleCrashReporting(e, context);
            defaultUncaughtExceptionHandler.uncaughtException(t, e);
            System.exit(1);
        });
    }

    private static void scheduleCrashReporting(Throwable e, Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, PendingIntent.getActivity(context, 123, getIntentToReportCrash(e, context), PendingIntent.FLAG_IMMUTABLE));
    }

}
