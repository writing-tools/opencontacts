package opencontacts.open.com.opencontacts;

import static android.graphics.Color.TRANSPARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static opencontacts.open.com.opencontacts.activities.CallLogGroupDetailsActivity.getIntentToShowCallLogEntries;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.dpToPixels;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getASpaceOfHeight;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.handleLongClickWith;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getTimestampPattern;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.shareContact;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.shareContactAsText;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.SOCIAL_INTEGRATION_ENABLED_PREFERENCE_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.defaultSocialAppEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isSocialIntegrationEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.setSharedPreferencesChangeListener;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldToggleContactActions;
import static opencontacts.open.com.opencontacts.utils.ThemeUtils.getHighlightColor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.underscore.Consumer;
import com.github.underscore.U;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import opencontacts.open.com.opencontacts.components.ImageButtonWithTint;
import opencontacts.open.com.opencontacts.components.TintedDrawablesStore;
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

/**
 * Created by sultanm on 7/31/17.
 */

public class CallLogListView extends RelativeLayout implements DataStoreChangeListener<CallLogEntry> {
    private boolean inSelectionMode;
    private String UNKNOWN;
    Context context;
    private EditNumberBeforeCallHandler editNumberBeforeCallHandler;
    ArrayAdapter<GroupedCallLogEntry> adapter;
    private boolean isSocialAppIntegrationEnabled;
    //android has weakref to this listener and gets garbage collected hence we should have it here.
    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;
    private SimpleDateFormat timeStampFormat;
    private LinkedHashMap<String, Consumer<GroupedCallLogEntry>> longClickOptionsAndTheirActions;
    private ListView listView;
    private HashSet<GroupedCallLogEntry> selectedEntries;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Runnable onEnteringMultiSelectMode;
    private Runnable onExitingMultiSelectMode;


    public CallLogListView(final Context context, EditNumberBeforeCallHandler editNumberBeforeCallHandler) {
        super(context);
        this.context = context;
        this.UNKNOWN = context.getString(R.string.unknown);
        this.editNumberBeforeCallHandler = editNumberBeforeCallHandler;
        listView = new ListView(context);
        listView.setId(android.R.id.list);
        listView.setFastScrollEnabled(true);
        addView(getSwipeRefreshLayout(context));
        prepareLongClickActions();
        boolean shouldToggleContactActions = shouldToggleContactActions(context);
        isSocialAppIntegrationEnabled = isSocialIntegrationEnabled(context);
        timeStampFormat = getTimestampPattern(context);
        List<CallLogEntry> callLogEntries = new ArrayList<>();
        inSelectionMode = false;

        final OnClickListener callContact = v -> {
            if (inSelectionMode) return;
            CallLogEntry callLogEntry = getLatestCallLogEntry((View) v.getParent());
            AndroidUtils.call(callLogEntry.getPhoneNumber(), context);
        };

        final OnClickListener socialAppContact = v -> {
            if (inSelectionMode) return;
            CallLogEntry callLogEntry = getLatestCallLogEntry((View) v.getParent());
            AndroidUtils.openSocialApp(callLogEntry.getPhoneNumber(), context);
        };

        final OnLongClickListener socialAppLongClick = v -> {
            if (inSelectionMode) return false;
            CallLogEntry callLogEntry = getLatestCallLogEntry((View) v.getParent());
            AndroidUtils.onSocialLongPress(callLogEntry.getPhoneNumber(), context);
            return true;
        };

        final OnClickListener messageContact = v -> {
            if (inSelectionMode) return;
            CallLogEntry callLogEntry = getLatestCallLogEntry((View) v.getParent());
            AndroidUtils.message(callLogEntry.getPhoneNumber(), context);
        };

        final OnClickListener selectionModeTap = v -> {
            View parent = (View) v.getParent();
            GroupedCallLogEntry groupedCallLogEntry = ((GroupedCallLogEntry) parent.getTag());
            if (selectedEntries.contains(groupedCallLogEntry))
                selectedEntries.remove(groupedCallLogEntry);
            else selectedEntries.add(groupedCallLogEntry);
            if (selectedEntries.isEmpty()) exitSelectionMode();
            else adapter.notifyDataSetChanged();
        };

        final OnClickListener showContactDetails = v -> {
            CallLogEntry callLogEntry = getLatestCallLogEntry((View) v.getParent());
            long contactId = callLogEntry.getContactId();
            if (contactId == -1)
                return;
            Contact contact = ContactsDataStore.getContactWithId(contactId);
            if (contact == null)
                return;
            Intent showContactDetails1 = AndroidUtils.getIntentToShowContactDetails(contactId, CallLogListView.this.context);
            context.startActivity(showContactDetails1);
        };

        final OnLongClickListener callLogEntryLongClickListener = v -> {
            View parent = (View) v.getParent();
            GroupedCallLogEntry groupedCallLogEntry = ((GroupedCallLogEntry) parent.getTag());
            CallLogEntry callLogEntry = groupedCallLogEntry.latestCallLogEntry;
            List<String> longClickOptions = new ArrayList<>(Arrays.asList(longClickOptionsAndTheirActions.keySet().toArray(new String[0])));
            if (callLogEntry.contactId != -1) {
                longClickOptions.remove(context.getString(R.string.add_contact));
            }
            else {
                longClickOptions.remove(context.getString(R.string.share_menu_item));
                longClickOptions.remove(context.getString(R.string.share_as_text));
            }
            String[] dynamicListOfLongClickActions = longClickOptions.toArray(new String[0]);
            handleLongClickWith(longClickOptionsAndTheirActions, dynamicListOfLongClickActions, groupedCallLogEntry, context);
            return true;
        };

        adapter = new ArrayAdapter<GroupedCallLogEntry>(CallLogListView.this.context, R.layout.grouped_call_log_entry, CallLogGroupingUtil.group(callLogEntries)) {
            private LayoutInflater layoutInflater = LayoutInflater.from(CallLogListView.this.context);

            @NonNull
            @Override
            public View getView(int position, View reusableView, ViewGroup parent) {
                GroupedCallLogEntry groupedCallLogEntry = getItem(position);
                CallLogEntry callLogEntry = groupedCallLogEntry.latestCallLogEntry;
                if (reusableView == null)
                    reusableView = layoutInflater.inflate(R.layout.grouped_call_log_entry, parent, false);
                ((TextView) reusableView.findViewById(R.id.textview_full_name)).setText(callLogEntry.getContactId() == -1 ? UNKNOWN : callLogEntry.getName());
                ((TextView) reusableView.findViewById(R.id.textview_phone_number)).setText(callLogEntry.getPhoneNumber());
                setCallAndMessageActions(reusableView, callLogEntry);

                View socialAppIcon = reusableView.findViewById(R.id.button_social);
                if (isSocialAppIntegrationEnabled) {
                    socialAppIcon.setOnClickListener(socialAppContact);
                    socialAppIcon.setContentDescription(defaultSocialAppEnabled(context) + " " + callLogEntry.name);
                    socialAppIcon.setOnLongClickListener(socialAppLongClick);
                    socialAppIcon.setVisibility(VISIBLE);
                } else socialAppIcon.setVisibility(GONE);
                ImageView callType = reusableView.findViewById(R.id.image_view_call_type);
                if (callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.INCOMING_TYPE))){
                    callType.setImageResource(R.drawable.ic_call_received_black_24dp);
                    callType.setContentDescription(context.getString(R.string.incoming_call));
                }
                else if (callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.OUTGOING_TYPE))){
                    callType.setImageResource(R.drawable.ic_call_made_black_24dp);
                    callType.setContentDescription(context.getString(R.string.outgoing_call));
                }
                else if (callLogEntry.getCallType().equals(String.valueOf(CallLog.Calls.MISSED_TYPE))){
                    callType.setImageResource(R.drawable.ic_call_missed_outgoing_black_24dp);
                    callType.setContentDescription(context.getString(R.string.missed_call));
                }
                ((TextView) reusableView.findViewById(R.id.text_view_duration)).setText(Common.getDurationInMinsAndSecs(Integer.valueOf(callLogEntry.getDuration())));
                ((TextView) reusableView.findViewById(R.id.text_view_sim)).setText(String.valueOf(callLogEntry.getSimId()));
                String timeStampOfCall = timeStampFormat.format(new Date(Long.parseLong(callLogEntry.getDate())));
                ((TextView) reusableView.findViewById(R.id.text_view_timestamp)).setText(timeStampOfCall);

                List<CallLogEntry> callLogEntriesInGroup = groupedCallLogEntry.callLogEntries;
                AppCompatTextView callRepeatCount = reusableView.findViewById(R.id.call_repeat_count);
                int groupSize = callLogEntriesInGroup.size();
                if (groupSize == 1) callRepeatCount.setVisibility(GONE);
                else {
                    callRepeatCount.setText(context.getString(R.string.call_repeat_text, groupSize));
                    callRepeatCount.setContentDescription(context.getString(R.string.call_repeat_text_content_description, groupSize));
                    callRepeatCount.setVisibility(VISIBLE);
                }

                reusableView.setTag(groupedCallLogEntry);
                View callLogDetails = reusableView.findViewById(R.id.call_log_details);
                if (inSelectionMode) {
                    callLogDetails.setOnClickListener(selectionModeTap);
                    callLogDetails.setOnLongClickListener(null);
                    if (selectedEntries.contains(groupedCallLogEntry))
                        reusableView.setBackgroundColor(getHighlightColor(context));
                    else reusableView.setBackgroundColor(TRANSPARENT);
                } else {
                    callLogDetails.setOnClickListener(showContactDetails);
                    callLogDetails.setOnLongClickListener(callLogEntryLongClickListener);
                    reusableView.setBackgroundColor(TRANSPARENT);
                }
                return reusableView;
            }

            private void setCallAndMessageActions(View reusableView, CallLogEntry callLogEntry) {
                ImageButtonWithTint actionButton1 = reusableView.findViewById(R.id.button_action1);
                ImageButtonWithTint actionButton2 = reusableView.findViewById(R.id.button_action2);
                String callAction = context.getString(R.string.call) + callLogEntry.name;
                String messageAction = context.getString(R.string.message) + callLogEntry.name;
                if (shouldToggleContactActions) {
                    actionButton1.setContentDescription(messageAction);
                    actionButton1.setOnClickListener(messageContact);
                    actionButton1.setImageResource(R.drawable.ic_chat_black_24dp);
                    actionButton2.setOnClickListener(callContact);
                    actionButton2.setImageResource(R.drawable.ic_call_black_24dp);
                    actionButton2.setContentDescription(callAction);
                } else {
                    actionButton1.setContentDescription(callAction);
                    actionButton1.setOnClickListener(callContact);
                    actionButton1.setImageResource(R.drawable.ic_call_black_24dp);
                    actionButton2.setOnClickListener(messageContact);
                    actionButton2.setImageResource(R.drawable.ic_chat_black_24dp);
                    actionButton2.setContentDescription(messageAction);
                }
            }
        };
        listView.setAdapter(adapter);
        CallLogDataStore.addDataChangeListener(this);
        reload();
        //android has weakref to this listener and gets garbage collected hence we should have it here.
        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (!SOCIAL_INTEGRATION_ENABLED_PREFERENCE_KEY.equals(key)
                && !PREFTIMEFORMAT_12_HOURS_SHARED_PREF_KEY.equals(key)
            ) return;
            isSocialAppIntegrationEnabled = isSocialIntegrationEnabled(context);
            timeStampFormat = getTimestampPattern(context);
            adapter.notifyDataSetChanged();
        };
        setSharedPreferencesChangeListener(sharedPreferenceChangeListener, context);
        listView.addFooterView(getFooterView());
    }

    private View getFooterView() {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(getViewMoreButton());
        linearLayout.addView(getASpaceOfHeight(10, 56, context)); //56 is height of bottom menu, 10 is arbitrary
        return linearLayout;
    }

    @NonNull
    private SwipeRefreshLayout getSwipeRefreshLayout(Context context) {
        swipeRefreshLayout = new SwipeRefreshLayout(context);
        swipeRefreshLayout.addView(listView);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (listView.getCount() == 0)
                reload();
            else
                CallLogDataStore.loadRecentCallLogEntriesAsync(context);
            swipeRefreshLayout.setRefreshing(false);
        });
        return swipeRefreshLayout;
    }

    @NonNull
    private AppCompatButton getViewMoreButton() {
        AppCompatButton viewMoreButton = new AppCompatButton(getContext());
        viewMoreButton.setText(R.string.view_more);
        viewMoreButton.setOnClickListener(v -> CallLogDataStore.loadNextChunkOfCallLogEntries());
        return viewMoreButton;
    }

    private void prepareLongClickActions() {
        longClickOptionsAndTheirActions = new LinkedHashMap<>();
        longClickOptionsAndTheirActions.put(context.getString(R.string.delete_multiple), groupedCallLogEntry -> {
            enterSelectionMode();
            selectedEntries.add(groupedCallLogEntry);
            adapter.notifyDataSetChanged();
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.copy_to_clipboard), groupedCallLogEntry -> {
            AndroidUtils.copyToClipboard(groupedCallLogEntry.latestCallLogEntry.getPhoneNumber(), context);
            Toast.makeText(context, R.string.copied_phonenumber_to_clipboard, Toast.LENGTH_SHORT).show();
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.add_contact), groupedCallLogEntry -> {
            AndroidUtils.getAlertDialogToAddContact(groupedCallLogEntry.latestCallLogEntry.getPhoneNumber(), context).show();
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.edit_before_call), groupedCallLogEntry -> {
            this.editNumberBeforeCallHandler.setNumber(groupedCallLogEntry.latestCallLogEntry.getPhoneNumber());
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.delete), groupedCallLogEntry -> {
            CallLogDataStore.delete(groupedCallLogEntry.latestCallLogEntry.getId());
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.show_details), groupedCallLogEntry -> {
            context.startActivity(getIntentToShowCallLogEntries(groupedCallLogEntry.latestCallLogEntry.getPhoneNumber(), context));
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.share_menu_item), groupedCallLogEntry -> {
            shareContact(groupedCallLogEntry.latestCallLogEntry.contactId, context);
        });
        longClickOptionsAndTheirActions.put(context.getString(R.string.share_as_text), groupedCallLogEntry -> {
            shareContactAsText(groupedCallLogEntry.latestCallLogEntry.contactId, context);
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

    public void reload() {
        final List<GroupedCallLogEntry> groupedCallLogEntries = CallLogGroupingUtil.group(CallLogDataStore.getRecentCallLogEntries(context));
        this.post(() -> {
            adapter.clear();
            adapter.addAll(groupedCallLogEntries);
            adapter.notifyDataSetChanged();
        });
    }

    public void onDestroy() {
        CallLogDataStore.removeDataChangeListener(this);
    }

    public void setEditNumberBeforeCallHandler(EditNumberBeforeCallHandler editNumberBeforeCallHandler) {
        this.editNumberBeforeCallHandler = editNumberBeforeCallHandler;
    }

    public void enterSelectionMode() {
        inSelectionMode = true;
        if (selectedEntries == null) selectedEntries = new HashSet<>(0);
        else selectedEntries.clear();
        addDeleteFABButton();
        swipeRefreshLayout.setEnabled(false);
        if (onEnteringMultiSelectMode != null) onEnteringMultiSelectMode.run();
    }

    public void exitSelectionMode() {
        if (!inSelectionMode) return;
        inSelectionMode = false;
        removeView(findViewById(R.id.delete));
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setEnabled(true);
        if (onExitingMultiSelectMode != null) onExitingMultiSelectMode.run();
    }

    private void addDeleteFABButton() {
        FloatingActionButton deleteMultipleContacts = new FloatingActionButton(getContext());
        deleteMultipleContacts.setImageDrawable(TintedDrawablesStore.getTintedDrawable(R.drawable.delete, getContext()));
        deleteMultipleContacts.setId(R.id.delete);
        RelativeLayout.LayoutParams deleteFABLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        deleteFABLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        deleteFABLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        deleteFABLayoutParams.rightMargin = (int) dpToPixels(12);
        addView(deleteMultipleContacts, deleteFABLayoutParams);
        deleteMultipleContacts.setTranslationY(-dpToPixels(16)); //plain old relative layout align parent bottom and bottom margin
        deleteMultipleContacts.setOnClickListener(v -> {
            deleteSelection();
            exitSelectionMode();
        });
    }

    private void deleteSelection() {
        List<CallLogEntry> individualCallLogEntries = U.chain(selectedEntries)
            .map(groupedCallLogEntry -> groupedCallLogEntry.callLogEntries)
            .flatten()
            .value();
        CallLogDataStore.deleteCallLogEntries(individualCallLogEntries);
        selectedEntries.clear();
    }

    public void setOnEnteringMultiSelectMode(Runnable onEnteringMultiSelectMode) {
        this.onEnteringMultiSelectMode = onEnteringMultiSelectMode;
    }

    public void setOnExitingMultiSelectMode(Runnable onExitingMultiSelectMode) {
        this.onExitingMultiSelectMode = onExitingMultiSelectMode;
    }

    public boolean isInSelectionMode() {
        return inSelectionMode;
    }

}
