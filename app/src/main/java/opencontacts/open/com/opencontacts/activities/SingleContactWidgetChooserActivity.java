package opencontacts.open.com.opencontacts.activities;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import opencontacts.open.com.opencontacts.domain.Contact;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static opencontacts.open.com.opencontacts.data.datastore.SingleContactWidgetDataStore.saveSingleContactWidget;
import static opencontacts.open.com.opencontacts.widgets.SingleContactDialerWidgetProvider.updateWidgetView;

public class SingleContactWidgetChooserActivity extends SingleContactChooserActivityBase {

    private int widgetID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        widgetID = getIntent().getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
        if(widgetID == INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "Widget id is empty", Toast.LENGTH_LONG).show();
            finish();
        }
        setOnContactSelect(this::drawSelectedContact);
    }

    private void drawSelectedContact(Contact contact){
        saveSingleContactWidget(widgetID, contact.id, this);
        updateWidgetView(widgetID, this);
        setResult(RESULT_OK,
                new Intent()
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID));
        finish();

    }
}
