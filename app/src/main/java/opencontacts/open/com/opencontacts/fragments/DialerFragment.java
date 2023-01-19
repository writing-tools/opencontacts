package opencontacts.open.com.opencontacts.fragments;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static opencontacts.open.com.opencontacts.activities.CallLogGroupDetailsActivity.getIntentToShowCallLogEntries;
import static opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore.getUnLabelledCallLogEntriesMatching;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.getContactsMatchingT9;
import static opencontacts.open.com.opencontacts.domain.Contact.createDummyContact;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getASpaceOfHeight;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getIntentToAddContact;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getIntentToShowContactDetails;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.handleLongClickWith;
import static opencontacts.open.com.opencontacts.utils.PhoneCallUtils.hasMultipleSims;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.Nullable;

import com.github.underscore.Consumer;
import com.github.underscore.U;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import opencontacts.open.com.opencontacts.ContactsListViewAdapter;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.actions.DefaultContactsListActions;
import opencontacts.open.com.opencontacts.components.ImageButtonWithTint;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.SelectableTab;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.DomainUtils;
import opencontacts.open.com.opencontacts.utils.PhoneCallUtils;

public class DialerFragment extends AppBaseFragment implements SelectableTab {
    private Context context;
    private View view;
    private EditText dialPadEditText;
    private ListView searchList;
    private ContactsListViewAdapter searchListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        this.context = getContext();
        this.view = view;
        linkEditTextWithSearchList(view);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        linkButtons();
    }

    private void linkEditTextWithSearchList(View view) {
        setupSearchList(view);
        String unknownContactString = context.getString(R.string.unknown);
        dialPadEditText = view.findViewById(R.id.editText_dialpad_number);
        dialPadEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable searchText) {
                String t9Text = searchText.toString();
                if (TextUtils.isEmpty(t9Text)) {
                    hideSearchListAndUpdateUIForRest();
                    return;
                }
                List<Contact> unLabelledCallLogEntriesMatchingText = new U<>(getUnLabelledCallLogEntriesMatching(t9Text))
                    .map(callLogEntry -> createDummyContact(unknownContactString, "", callLogEntry.getPhoneNumber(), callLogEntry.getDate()));
                List<Contact> contactsMatchingT9 = getContactsMatchingT9(t9Text);
                if (contactsMatchingT9.isEmpty() && unLabelledCallLogEntriesMatchingText.isEmpty())
                    hideSearchListAndUpdateUIForRest();
                else {
                    List<Contact> finalListOfContacts = U.flatten(Arrays.asList(unLabelledCallLogEntriesMatchingText, contactsMatchingT9));
                    Collections.sort(finalListOfContacts, DomainUtils.getContactComparatorBasedOnLastAccessed());
                    searchListAdapter.clear();
                    searchListAdapter.addAll(finalListOfContacts);
                    searchListAdapter.notifyDataSetChanged();
                    searchList.setVisibility(VISIBLE);
                    hideMultiSimDialingButtons();
                }
            }
        });
    }

    private void hideSearchListAndUpdateUIForRest() {
        searchListAdapter.clear();
        searchListAdapter.notifyDataSetChanged();
        searchList.setVisibility(INVISIBLE);
        enableMultiSimDialingButtonsIfHavingMutipleSims();
    }

    private LinkedHashMap<String, Consumer<String>> longClickOptionsAndListeners() {
        LinkedHashMap<String, Consumer<String>> longClickListenersMap = new LinkedHashMap<>();
        longClickListenersMap.put(context.getString(R.string.add_contact), number -> startActivity(getIntentToAddContact(number, context)));
        longClickListenersMap.put(context.getString(R.string.copy_to_clipboard), number -> AndroidUtils.copyToClipboard(number, true, context));
        longClickListenersMap.put(context.getString(R.string.edit_before_call), number -> this.dialPadEditText.setText(number));
        return longClickListenersMap;
    }

    private void setupSearchList(View view) {
        searchList = view.findViewById(R.id.search_list);
        ImageButtonWithTint closeList = new ImageButtonWithTint(context);
        closeList.setImageResource(R.drawable.ic_arrow_down_24dp);
        closeList.setOnClickListener(v -> hideSearchListAndUpdateUIForRest());
        searchList.addHeaderView(closeList);
        searchList.addFooterView(getASpaceOfHeight(1, 56, context)); //56 here is height of bottom menu
        searchListAdapter = new ContactsListViewAdapter(context);
        LinkedHashMap<String, Consumer<String>> longClickOptionsAndListeners = longClickOptionsAndListeners();
        searchListAdapter.setContactsListActionsListener(new DefaultContactsListActions(context) {
            @Override
            public void onShowDetails(Contact contact) {
                if (contact.id == -1) {
                    startActivity(getIntentToShowCallLogEntries(contact.primaryPhoneNumber.phoneNumber, getContext()));
                } else startActivity(getIntentToShowContactDetails(contact.id, getContext()));
            }

            @Override
            public void onLongClick(Contact contact) {
                if (contact.id != -1) return;
                handleLongClickWith(longClickOptionsAndListeners, contact.primaryPhoneNumber.phoneNumber, context);
            }
        });
        searchList.setAdapter(searchListAdapter);
    }

    public void setNumber(String number) {
        dialPadEditText.setText(number);
    }

    private void linkButtons() {

        view.findViewById(R.id.button_call).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.call(phoneNumber, context)));

        view.findViewById(R.id.button_social).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.openSocialApp(phoneNumber, context)));

        view.findViewById(R.id.button_social).setOnLongClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.onSocialLongPress(phoneNumber, context)));

        view.findViewById(R.id.button_message).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.message(phoneNumber, context)));

        view.findViewById(R.id.button_add_contact).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.getAlertDialogToAddContact(phoneNumber, context).show()));

        view.findViewById(R.id.button_call_sim1).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> PhoneCallUtils.callUsingSim(phoneNumber, 0, context)));

        view.findViewById(R.id.button_call_sim2).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> PhoneCallUtils.callUsingSim(phoneNumber, 1, context)));

        view.findViewById(R.id.button_clear).setOnClickListener(v -> dialPadEditText.setText(""));

        if (searchListAdapter != null && !searchListAdapter.isEmpty()) hideMultiSimDialingButtons();
        else enableMultiSimDialingButtonsIfHavingMutipleSims();
    }

    private void enableMultiSimDialingButtonsIfHavingMutipleSims() {
        if (hasMultipleSims(getContext())) showMultiSimDialingButtons();
        else hideMultiSimDialingButtons();
    }

    private void hideMultiSimDialingButtons() {
        setVisibilityOfMultiSimButtons(INVISIBLE);
    }

    private void setVisibilityOfMultiSimButtons(int visibility) {
        view.findViewById(R.id.button_call_sim1).setVisibility(visibility);
        view.findViewById(R.id.button_call_sim2).setVisibility(visibility);
        view.findViewById(R.id.text_call_sim1).setVisibility(visibility);
        view.findViewById(R.id.text_call_sim2).setVisibility(visibility);
    }

    private void showMultiSimDialingButtons() {
        setVisibilityOfMultiSimButtons(VISIBLE);
    }

    private boolean performActionIfPhoneNumberIsValidElseShowError(Consumer<String> action) {
        String phoneNumber = dialPadEditText.getText().toString();
        if (isInvalid(phoneNumber))
            dialPadEditText.setError(getString(R.string.invalid_number));
        else
            action.accept(phoneNumber);
        return true;
    }

    private boolean isInvalid(String phoneNumber) {
        return TextUtils.isEmpty(phoneNumber) || TextUtils.getTrimmedLength(phoneNumber) == 0;
    }


    @Override
    public void onSelect() {
        if (this.view == null) return;
        EditText editText = dialPadEditText == null ? (EditText) view.findViewById(R.id.editText_dialpad_number) : dialPadEditText;
        AndroidUtils.showSoftKeyboard(editText, context);
    }

    @Override
    public void onUnSelect() {
        EditText editText = dialPadEditText == null ? (EditText) view.findViewById(R.id.editText_dialpad_number) : dialPadEditText;
        AndroidUtils.hideSoftKeyboard(editText, context);
    }
}
