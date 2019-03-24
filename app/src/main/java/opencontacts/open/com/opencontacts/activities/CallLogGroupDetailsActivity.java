package opencontacts.open.com.opencontacts.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.underscore.U;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.GroupedCallLogEntry;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.utils.Common;

import static opencontacts.open.com.opencontacts.utils.DomainUtils.getTimestampPattern;

public class CallLogGroupDetailsActivity extends AppBaseActivity {

    public static final String CALL_LOG_ENTRIES = "entries";
    private LinearLayout callLogHolder;
    private ArrayList<CallLogEntry> callLogEntries;

    @Override
    int getLayoutResource() {
        return R.layout.activity_call_log_group_entry;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callLogHolder = findViewById(R.id.call_log_holder);
        callLogEntries = (ArrayList<CallLogEntry>) getIntent().getSerializableExtra(CALL_LOG_ENTRIES);
        addCallLogEntriesIntoView(callLogEntries);
        String name = U.first(callLogEntries).getName();
        getSupportActionBar().setTitle(name == null ? getString(R.string.unknown) : name);
    }

    private void addCallLogEntriesIntoView(List<CallLogEntry> callLogEntries) {
        LayoutInflater layoutInflater = getLayoutInflater();
        SimpleDateFormat timeStampFormat = getTimestampPattern(this);
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

    public static Intent getIntentToShowCallLogEntries(GroupedCallLogEntry groupedCallLogEntry, Context context){
        return new Intent(context, CallLogGroupDetailsActivity.class)
                .putExtra(CALL_LOG_ENTRIES, new ArrayList<>(groupedCallLogEntry.callLogEntries));
    }
}
