package opencontacts.open.com.opencontacts.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.underscore.U;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.utils.Common;

import static opencontacts.open.com.opencontacts.components.TintedDrawablesStore.setDrawableForFAB;
import static opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore.getCallLogEntriesForContactWith;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.getContact;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getIntentToShowContactDetails;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getFullDateTimestampPattern;

public class CallLogGroupDetailsActivity extends AppBaseActivity {

    public static final String PHONE_NUMBER_INTENT_EXTRA = "phoneNumber";
    private LinearLayout callLogHolder;
    private List<CallLogEntry> callLogEntries;

    @Override
    int getLayoutResource() {
        return R.layout.activity_call_log_details;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callLogHolder = findViewById(R.id.call_log_holder);
        FloatingActionButton infoButton = findViewById(R.id.info);
        String phoneNumber = getIntent().getStringExtra(PHONE_NUMBER_INTENT_EXTRA);
        if(phoneNumber == null) {
            Toast.makeText(this, "No phone number present", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        callLogEntries = getCallLogEntriesForContactWith(phoneNumber);
        if(callLogEntries.isEmpty()) {
            Toast.makeText(this, "No call log entries present for this number", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        addCallLogEntriesIntoView(callLogEntries);
        Contact contact = getContact(phoneNumber);
        if (contact == null) {
            getSupportActionBar().setTitle(getString(R.string.unknown));
            infoButton.hide();
            return;
        }
        getSupportActionBar().setTitle(contact.firstName);
        setDrawableForFAB(R.drawable.ic_outline_info_24, infoButton, this);
        infoButton.show();
        infoButton.setOnClickListener(v -> startActivity(getIntentToShowContactDetails(contact.getId(), this)));
    }

    private void addCallLogEntriesIntoView(List<CallLogEntry> callLogEntries) {
        LayoutInflater layoutInflater = getLayoutInflater();
        SimpleDateFormat timeStampFormat = getFullDateTimestampPattern(this);
        U.forEach(callLogEntries, callLogEntry -> {
            View callLogEntryView = layoutInflater.inflate(R.layout.call_log_entry, callLogHolder, false);
            ((AppCompatTextView)callLogEntryView.findViewById(R.id.text_view_sim)).setText(String.valueOf(callLogEntry.getSimId()));
            ((AppCompatTextView)callLogEntryView.findViewById(R.id.textview_phone_number)).setText(callLogEntry.getPhoneNumber());
            if(callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.INCOMING_TYPE)))
                ((ImageView)callLogEntryView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_received_black_24dp);
            else if(callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.OUTGOING_TYPE)))
                ((ImageView)callLogEntryView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_made_black_24dp);
            else if(callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.MISSED_TYPE)))
                ((ImageView)callLogEntryView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_missed_outgoing_black_24dp);
            ((TextView)callLogEntryView.findViewById(R.id.text_view_duration)).setText(Common.getDurationInMinsAndSecs(Integer.valueOf(callLogEntry.getDuration())));
            ((TextView)callLogEntryView.findViewById(R.id.text_view_sim)).setText(String.valueOf(callLogEntry.getSimId()));
            String timeStampOfCall = timeStampFormat.format(new Date(Long.parseLong(callLogEntry.getDate())));
            ((TextView)callLogEntryView.findViewById(R.id.text_view_timestamp)).setText(timeStampOfCall);
            callLogHolder.addView(callLogEntryView);

        });
    }

    public static Intent getIntentToShowCallLogEntries(String phoneNumber, Context context){
        return new Intent(context, CallLogGroupDetailsActivity.class)
                .putExtra(PHONE_NUMBER_INTENT_EXTRA, phoneNumber);
    }
}
