package opencontacts.open.com.opencontacts.activities;

import static opencontacts.open.com.opencontacts.components.TintedDrawablesStore.setDrawableForFAB;
import static opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore.getCallLogEntriesForContactWith;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.getContact;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getIntentToShowContactDetails;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getFullDateTimestampPattern;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.utils.Common;

public class CallLogGroupDetailsActivity extends AppBaseActivity {

    public static final String PHONE_NUMBER_INTENT_EXTRA = "phoneNumber";
    private ListView callLogListView;
    private ArrayAdapter<CallLogEntry> callLogAdapter;
    private List<CallLogEntry> callLogEntries = new ArrayList<>(0);
    private String phoneNumber;

    @Override
    int getLayoutResource() {
        return R.layout.activity_call_log_details;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callLogListView = findViewById(R.id.call_log_holder);
        FloatingActionButton infoButton = findViewById(R.id.info);
        phoneNumber = getIntent().getStringExtra(PHONE_NUMBER_INTENT_EXTRA);
        if (phoneNumber == null) {
            Toast.makeText(this, "No phone number present", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        callLogEntries.addAll(getCallLogEntriesForContactWith(phoneNumber, callLogEntries.size()));
        if (callLogEntries.isEmpty()) {
            Toast.makeText(this, "No call log entries present for this number", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setupCallLogAdapter(callLogEntries);
        addAddMoreButton();
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

    private void addAddMoreButton() {
        AppCompatButton moreButton = new AppCompatButton(this);
        moreButton.setText(R.string.view_more);
        moreButton.setOnClickListener(v -> {
            List<CallLogEntry> newEntries = getCallLogEntriesForContactWith(phoneNumber, callLogEntries.size());
            callLogEntries.addAll(newEntries);
            callLogAdapter.notifyDataSetChanged();
        });
        callLogListView.addFooterView(moreButton);
    }

    private void setupCallLogAdapter(List<CallLogEntry> callLogEntries) {
        SimpleDateFormat timeStampFormat = getFullDateTimestampPattern(this);
        callLogAdapter = new ArrayAdapter<CallLogEntry>(CallLogGroupDetailsActivity.this, R.layout.call_log_entry, callLogEntries) {
            private LayoutInflater layoutInflater = LayoutInflater.from(CallLogGroupDetailsActivity.this);
            @NonNull
            @Override
            public View getView(int position, View callLogEntryView, ViewGroup parent) {
                CallLogEntry callLogEntry = getItem(position);
                if (callLogEntryView == null)
                    callLogEntryView = layoutInflater.inflate(R.layout.call_log_entry, parent, false);

                ((AppCompatTextView) callLogEntryView.findViewById(R.id.text_view_sim)).setText(String.valueOf(callLogEntry.getSimId()));
                ((AppCompatTextView) callLogEntryView.findViewById(R.id.textview_phone_number)).setText(callLogEntry.getPhoneNumber());
                if (callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.INCOMING_TYPE)))
                    ((ImageView) callLogEntryView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_received_black_24dp);
                else if (callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.OUTGOING_TYPE)))
                    ((ImageView) callLogEntryView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_made_black_24dp);
                else if (callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.MISSED_TYPE)))
                    ((ImageView) callLogEntryView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_missed_outgoing_black_24dp);
                ((TextView) callLogEntryView.findViewById(R.id.text_view_duration)).setText(Common.getDurationInMinsAndSecs(Integer.valueOf(callLogEntry.getDuration())));
                ((TextView) callLogEntryView.findViewById(R.id.text_view_sim)).setText(String.valueOf(callLogEntry.getSimId()));
                String timeStampOfCall = timeStampFormat.format(new Date(Long.parseLong(callLogEntry.getDate())));
                ((TextView) callLogEntryView.findViewById(R.id.text_view_timestamp)).setText(timeStampOfCall);

                return callLogEntryView;
            }
        };
        callLogListView.setAdapter(callLogAdapter);
    }

    public static Intent getIntentToShowCallLogEntries(String phoneNumber, Context context) {
        return new Intent(context, CallLogGroupDetailsActivity.class)
            .putExtra(PHONE_NUMBER_INTENT_EXTRA, phoneNumber);
    }
}
