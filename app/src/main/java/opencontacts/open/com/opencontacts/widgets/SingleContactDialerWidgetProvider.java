package opencontacts.open.com.opencontacts.widgets;

import static opencontacts.open.com.opencontacts.data.datastore.SingleContactWidgetDataStore.getContactForSingleContactWidget;
import static opencontacts.open.com.opencontacts.data.datastore.SingleContactWidgetDataStore.removeSingleContactWidgets;
import static opencontacts.open.com.opencontacts.data.datastore.SingleContactWidgetDataStore.replaceOldWithNewWidgetIds;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getIntentToCall;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.Common;

public class SingleContactDialerWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Common.forEach(appWidgetIds, id -> updateWidgetView(id, context));
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
        replaceOldWithNewWidgetIds(oldWidgetIds, newWidgetIds, context);
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        removeSingleContactWidgets(appWidgetIds, context);
    }

    public static void updateWidgetView(int widgetId, Context context) {
        Contact contact;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews widgetView = new RemoteViews(context.getPackageName(),
            R.layout.widget_single_contact_dialer);
        try {
            contact = getContactForSingleContactWidget(widgetId, context);
        } catch (Exception e) {
            widgetView.setTextViewText(R.id.contact_name, "DELETED");
            widgetView.setImageViewResource(R.id.contact_image, R.drawable.ic_error_outline_black_24dp);
            appWidgetManager.updateAppWidget(widgetId, widgetView);
            return;
        }

        widgetView.setTextViewText(R.id.contact_name, contact.name);
        widgetView.setOnClickPendingIntent(R.id.entire_single_contact_widget,
            PendingIntent.getActivity(context, 0,
                getIntentToCall(contact.primaryPhoneNumber.phoneNumber, context),
                0));
        appWidgetManager.updateAppWidget(widgetId, widgetView);

    }

}
