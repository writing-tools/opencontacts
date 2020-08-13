package opencontacts.open.com.opencontacts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.github.underscore.Consumer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import opencontacts.open.com.opencontacts.components.ImageButtonWithTint;
import opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.domain.GroupedCallLogEntry;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;
import opencontacts.open.com.opencontacts.interfaces.EditNumberBeforeCallHandler;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.CallLogGroupingUtil;
import opencontacts.open.com.opencontacts.utils.Common;

import static opencontacts.open.com.opencontacts.activities.CallLogGroupDetailsActivity.getIntentToShowCallLogEntries;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getTimestampPattern;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isWhatsappIntegrationEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.setSharedPreferencesChangeListener;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldToggleContactActions;

/**
 * Created by sultanm on 7/31/17.
 */

public class CallLogListView extends ListView implements DataStoreChangeListener<CallLogEntry> {
    private String UNKNOWN;
    Context context;
    private EditNumberBeforeCallHandler editNumberBeforeCallHandler;
    ArrayAdapter<GroupedCallLogEntry> adapter;
    private boolean isWhatsappIntegrationEnabled;
    //android has weakref to this listener and gets garbage collected hence we should have it here.
    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;
    private SimpleDateFormat timeStampFormat;
    private HashMap<String, Consumer<GroupedCallLogEntry>> longClickOptionsAndTheirActions;


    public CallLogListView(final Context context, EditNumberBeforeCallHandler editNumberBeforeCallHandler) {
        super(context);
        this.context = context;
        this.UNKNOWN = context.getString(R.string.unknown);
        this.editNumberBeforeCallHandler = editNumberBeforeCallHandler;
        prepareLongClickActions();
        boolean shouldToggleContactActions = shouldToggleContactActions(context);
        isWhatsappIntegrationEnabled = isWhatsappIntegrationEnabled(context);
        timeStampFormat = getTimestampPattern(context);
        List<CallLogEntry> callLogEntries = new ArrayList<>();

        final OnClickListener callContact = v -> {
            CallLogEntry callLogEntry = getLatestCallLogEntry((View)v.getParent());
            AndroidUtils.call(callLogEntry.getPhoneNumber(), context);
        };

        final OnClickListener whatsappContact = v -> {
            CallLogEntry callLogEntry = getLatestCallLogEntry((View)v.getParent());
            AndroidUtils.whatsapp(callLogEntry.getPhoneNumber(), context);
        };

        final OnClickListener messageContact = v -> {
            CallLogEntry callLogEntry = getLatestCallLogEntry((View)v.getParent());
            AndroidUtils.message(callLogEntry.getPhoneNumber(), context);
        };
        final OnClickListener showContactDetails = v -> {
            CallLogEntry callLogEntry = getLatestCallLogEntry(v);
            long contactId = callLogEntry.getContactId();
            if(contactId == -1)
                return;
            Contact contact = ContactsDataStore.getContactWithId(contactId);
            if(contact == null)
                return;
            Intent showContactDetails1 = AndroidUtils.getIntentToShowContactDetails(contactId, CallLogListView.this.context);
            context.startActivity(showContactDetails1);
        };

        final OnLongClickListener callLogEntryLongClickListener = v -> {
            GroupedCallLogEntry groupedCallLogEntry = (GroupedCallLogEntry) v.getTag();
            CallLogEntry callLogEntry = groupedCallLogEntry.latestCallLogEntry;
            List<String> longClickOptions = new ArrayList<>(Arrays.asList(longClickOptionsAndTheirActions.keySet().toArray(new String[0])));
            if (callLogEntry.contactId != -1) longClickOptions.remove(context.getString(R.string.add_contact));
            String[] dynamicListOfLongClickActions = longClickOptions.toArray(new String[0]);
            new AlertDialog.Builder(context)
                    .setItems(dynamicListOfLongClickActions, (dialog, which) -> {
                        longClickOptionsAndTheirActions.get(dynamicListOfLongClickActions[which]).accept(groupedCallLogEntry);
                    }).show();
            return true;
        };

        adapter = new ArrayAdapter<GroupedCallLogEntry>(CallLogListView.this.context, R.layout.grouped_call_log_entry, CallLogGroupingUtil.group(callLogEntries)){
            private LayoutInflater layoutInflater = LayoutInflater.from(CallLogListView.this.context);
            @NonNull
            @Override
            public View getView(int position, View reusableView, ViewGroup parent) {
                GroupedCallLogEntry groupedCallLogEntry = getItem(position);
                CallLogEntry callLogEntry = groupedCallLogEntry.latestCallLogEntry;
                if(reusableView == null)
                    reusableView = layoutInflater.inflate(R.layout.grouped_call_log_entry, parent, false);
                ((TextView) reusableView.findViewById(R.id.textview_full_name)).setText(callLogEntry.getContactId() == -1 ? UNKNOWN : callLogEntry.getName());
                ((TextView) reusableView.findViewById(R.id.textview_phone_number)).setText(callLogEntry.getPhoneNumber());
                setCallAndMessageActions(reusableView);

                View whatsappIcon = reusableView.findViewById(R.id.button_whatsapp);
                if(isWhatsappIntegrationEnabled){
                    whatsappIcon.setOnClickListener(whatsappContact);
                    whatsappIcon.setVisibility(VISIBLE);
                }
                else whatsappIcon.setVisibility(GONE);
                if(callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.INCOMING_TYPE)))
                    ((ImageView)reusableView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_received_black_24dp);
                else if(callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.OUTGOING_TYPE)))
                    ((ImageView)reusableView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_made_black_24dp);
                else if(callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.MISSED_TYPE)))
                    ((ImageView)reusableView.findViewById(R.id.image_view_call_type)).setImageResource(R.drawable.ic_call_missed_outgoing_black_24dp);
                ((TextView)reusableView.findViewById(R.id.text_view_duration)).setText(Common.getDurationInMinsAndSecs(Integer.valueOf(callLogEntry.getDuration())));
                ((TextView)reusableView.findViewById(R.id.text_view_sim)).setText(String.valueOf(callLogEntry.getSimId()));
                String timeStampOfCall = timeStampFormat.format(new Date(Long.parseLong(callLogEntry.getDate())));
                ((TextView)reusableView.findViewById(R.id.text_view_timestamp)).setText(timeStampOfCall);

                List<CallLogEntry> callLogEntriesInGroup = groupedCallLogEntry.callLogEntries;
                AppCompatTextView callRepeatCount = reusableView.findViewById(R.id.call_repeat_count);
                int groupSize = callLogEntriesInGroup.size();
                if(groupSize == 1) callRepeatCount.setVisibility(GONE);
                else {
                    callRepeatCount.setText(context.getString(R.string.call_repeat_text, groupSize));
                    callRepeatCount.setVisibility(VISIBLE);
                }

                reusableView.setTag(groupedCallLogEntry);
                reusableView.setOnClickListener(showContactDetails);
                reusableView.setOnLongClickListener(callLogEntryLongClickListener);
                return reusableView;
            }

            private void setCallAndMessageActions(View reusableView) {
                ImageButtonWithTint actionButton1 = reusableView.findViewById(R.id.button_action1);
                ImageButtonWithTint actionButton2 = reusableView.findViewById(R.id.button_action2);
                if(shouldToggleContactActions){
                    actionButton1.setOnClickListener(messageContact);
                    actionButton1.setImageResource(R.drawable.ic_chat_black_24dp);
                    actionButton2.setOnClickListener(callContact);
                    actionButton2.setImageResource(R.drawable.ic_call_black_24dp);
                }
                else {
                    actionButton1.setOnClickListener(callContact);
                    actionButton1.setImageResource(R.drawable.ic_call_black_24dp);
                    actionButton2.setOnClickListener(messageContact);
                    actionButton2.setImageResource(R.drawable.ic_chat_black_24dp);
                }
            }
        };
        this.setAdapter(adapter);
        CallLogDataStore.addDataChangeListener(this);
        reload();
        //android has weakref to this listener and gets garbage collected hence we should have it here.
        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (!WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY.equals(key)
                    && !PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY.equals(key)
                    ) return;
            isWhatsappIntegrationEnabled = isWhatsappIntegrationEnabled(context);
            timeStampFormat = getTimestampPattern(context);
            adapter.notifyDataSetChanged();
        };
        setSharedPreferencesChangeListener(sharedPreferenceChangeListener, context);
        addFooterView(getViewMoreButton());
    }

    @NonNull
    private AppCompatButton getViewMoreButton() {
        AppCompatButton viewMoreButton = new AppCompatButton(getContext());
        viewMoreButton.setText(R.string.view_more);
        viewMoreButton.setOnClickListener(v -> CallLogDataStore.loadNextChunkOfCallLogEntries());
        return viewMoreButton;
    }

    private void prepareLongClickActions() {
        longClickOptionsAndTheirActions = new HashMap<>();
        longClickOptionsAndTheirActions.put(context.getString(R.string.copy_to_clipboard), groupedCallLogEntry -> {
            AndroidUtils.copyToClipboard(groupedCallLogEntry.latestCallLogEntry.getPhoneNumber(), context);
            Toast.makeText(context, R.string.copied_phonenumber_to_clipboard, Toast.LENGTH_SHORT).show();
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.add_contact), groupedCallLogEntry -> {
            AndroidUtils.getAlertDialogToAddContact(groupedCallLogEntry.latestCallLogEntry.getPhoneNumber(), context).show();
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.edit_before_call), groupedCallLogEntry ->{
            this.editNumberBeforeCallHandler.setNumber(groupedCallLogEntry.latestCallLogEntry.getPhoneNumber());
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.delete), groupedCallLogEntry ->{
            CallLogDataStore.delete(groupedCallLogEntry.latestCallLogEntry.getId());
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.show_details), groupedCallLogEntry ->{
            context.startActivity(getIntentToShowCallLogEntries(groupedCallLogEntry, context));
        });

    }

    private CallLogEntry getLatestCallLogEntry(View v) {
        return ((GroupedCallLogEntry) v.getTag()).latestCallLogEntry;
    }

    @Override
    public void onUpdate(CallLogEntry callLogEntry) {
        reload();
    }

    @Override
    public void onRemove(CallLogEntry callLogEntry) {
        reload();
    }

    @Override
    public void onAdd(final CallLogEntry callLogEntry) {
        reload();
    }

    @Override
    public void onStoreRefreshed() {
        reload();
    }

    public void reload(){
        final List<GroupedCallLogEntry> groupedCallLogEntries = CallLogGroupingUtil.group(CallLogDataStore.getRecentCallLogEntries(context));
        this.post(() -> {
            adapter.clear();
            adapter.addAll(groupedCallLogEntries);
            adapter.notifyDataSetChanged();
        });
    }

    public void onDestroy(){
        CallLogDataStore.removeDataChangeListener(this);
    }
    public void setEditNumberBeforeCallHandler(EditNumberBeforeCallHandler editNumberBeforeCallHandler) {
        this.editNumberBeforeCallHandler = editNumberBeforeCallHandler;
    }

}
