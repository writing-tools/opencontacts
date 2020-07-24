package opencontacts.open.com.opencontacts.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.github.underscore.Consumer;
import com.github.underscore.U;

import java.util.List;

import opencontacts.open.com.opencontacts.ContactsListViewAdapter;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.actions.DefaultContactsListActions;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.SelectableTab;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.PhoneCallUtils;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore.getUnLabelledCallLogEntriesMatching;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.getContactsMatchingT9;
import static opencontacts.open.com.opencontacts.utils.PhoneCallUtils.hasMultipleSims;

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
        linkDialerButtonsToHandlers();
    }

    private void linkEditTextWithSearchList(View view) {
        setupSearchList(view);
        String unknownContactString = context.getString(R.string.unknown);
        dialPadEditText = view.findViewById(R.id.editText_dialpad_number);
        dialPadEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable searchText) {
                String t9Text = searchText.toString();
                if(TextUtils.isEmpty(t9Text)){
                    hideSearchList();
                    return;
                }
                List<Contact> unLabelledCallLogEntriesMatchingText = new U<>(getUnLabelledCallLogEntriesMatching(t9Text))
                        .map(callLogEntry -> new Contact(unknownContactString, "", callLogEntry.getPhoneNumber()));
                List<Contact> contactsMatchingT9 = getContactsMatchingT9(t9Text);
                if(contactsMatchingT9.isEmpty()) hideSearchList();
                else{
                    searchListAdapter.clear();
                    searchListAdapter.addAll(unLabelledCallLogEntriesMatchingText);
                    searchListAdapter.addAll(contactsMatchingT9);
                    searchListAdapter.notifyDataSetChanged();
                    searchList.setVisibility(VISIBLE);
                }
            }
        });
    }

    private void hideSearchList() {
        searchListAdapter.clear();
        searchListAdapter.notifyDataSetChanged();
        searchList.setVisibility(INVISIBLE);
    }

    private void setupSearchList(View view) {
        searchList = view.findViewById(R.id.search_list);
        searchListAdapter = new ContactsListViewAdapter(context);
        searchListAdapter.setContactsListActionsListener(new DefaultContactsListActions(context){
            @Override
            public void onLongClick(Contact contact) {
            }
        });
        searchList.setAdapter(searchListAdapter);
    }

    public void setNumber(String number) {
        dialPadEditText.setText(number);
    }

    private void linkDialerButtonsToHandlers() {

        view.findViewById(R.id.button_call).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.call(phoneNumber, context)));

        view.findViewById(R.id.button_whatsapp).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.whatsapp(phoneNumber, context)));

        view.findViewById(R.id.button_message).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.message(phoneNumber, context)));

        view.findViewById(R.id.button_add_contact).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.getAlertDialogToAddContact(phoneNumber, context).show()));

        view.findViewById(R.id.button_call_sim1).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> PhoneCallUtils.callUsingSim(phoneNumber, 0, context)));

        view.findViewById(R.id.button_call_sim2).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> PhoneCallUtils.callUsingSim(phoneNumber, 1, context)));

        if(hasMultipleSims(getContext())) showMultiSimDialingButtons();
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

    private void performActionIfPhoneNumberIsValidElseShowError(Consumer<String> action) {
        String phoneNumber = dialPadEditText.getText().toString();
        if(isInvalid(phoneNumber))
            dialPadEditText.setError(getString(R.string.invalid_number));
        else
            action.accept(phoneNumber);
    }

    private boolean isInvalid(String phoneNumber) {
        return TextUtils.isEmpty(phoneNumber) || TextUtils.getTrimmedLength(phoneNumber) == 0;
    }


    @Override
    public void onSelect() {
        if(this.view == null) return;
        EditText editText = dialPadEditText == null ? (EditText) view.findViewById(R.id.editText_dialpad_number) : dialPadEditText;
        AndroidUtils.showSoftKeyboard(editText, context);
    }

    @Override
    public void onUnSelect() {
        EditText editText = dialPadEditText == null ? (EditText) view.findViewById(R.id.editText_dialpad_number) : dialPadEditText;
        AndroidUtils.hideSoftKeyboard(editText, context);
    }
}
