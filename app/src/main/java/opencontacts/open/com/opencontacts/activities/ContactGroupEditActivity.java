package opencontacts.open.com.opencontacts.activities;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.domain.ContactGroup;

import static android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
import static android.text.TextUtils.isEmpty;
import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.widget.Toast.LENGTH_SHORT;
import static opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore.createNewGroup;
import static opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore.updateGroup;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getMenuItemClickHandlerFor;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.runOnMainDelayed;
import static opencontacts.open.com.opencontacts.utils.Common.getEmptyStringIfNull;


public class ContactGroupEditActivity extends ContactChooserActivityBase {

    public static final String GROUP_NAME_INTENT_EXTRA = "group_name";
    private TextInputEditText groupNameEditText;
    private String groupNameFromPrevScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupNameFromPrevScreen = getEmptyStringIfNull(getIntent().getStringExtra(GROUP_NAME_INTENT_EXTRA));
        LinearLayout aboveContactsListLinearLayout = findViewById(R.id.above_contacts_list);
        aboveContactsListLinearLayout.addView(getEditTextForGroupName());
        if(isEmpty(groupNameFromPrevScreen)) return;
        runOnMainDelayed(this::preselectContactsFromGroup, 300);
    }

    private void preselectContactsFromGroup() {
        ContactGroup group = ContactGroupsDataStore.getGroup(groupNameFromPrevScreen);
        if(group == null) return;
        setSelectedContacts(group.contacts);
    }

    private View getEditTextForGroupName() {
        groupNameEditText = new TextInputEditText(this);
        groupNameEditText.setHint(R.string.group_name);
        groupNameEditText.setText(groupNameFromPrevScreen);
        groupNameEditText.setInputType(TYPE_TEXT_FLAG_CAP_SENTENCES);
        TextInputLayout textInputLayout = new TextInputLayout(this);
        textInputLayout.addView(groupNameEditText);
        return textInputLayout;
    }

    @Override
    public void onContactSelect(Contact selectedContact) { }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(getString(R.string.save))
                .setIcon(R.drawable.ic_save_black_24dp)
                .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
                .setOnMenuItemClickListener(getMenuItemClickHandlerFor(this::saveAndGoback));
        return super.onCreateOptionsMenu(menu);
    }

    private void saveAndGoback() {
        String groupName = groupNameEditText.getText().toString();
        if(isEmpty(groupName)) {
            Toast.makeText(this, R.string.no_group_name_error, LENGTH_SHORT).show();
            return;
        }
        List<Contact> selectedContacts = getSelectedContacts();
        ContactGroup group = ContactGroupsDataStore.getGroup(groupNameFromPrevScreen);
        if(group == null) createNewGroup(selectedContacts, groupName);
        else updateGroup(selectedContacts, groupName, group);
        finish();
    }

    @Override
    public boolean shouldEnableMultiSelect() {
        return true;
    }

    @Override
    public int getTitleResource() {
        return R.string.edit_group;
    }

}
