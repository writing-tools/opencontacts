package opencontacts.open.com.opencontacts.activities;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.domain.ContactGroup;

import static android.text.TextUtils.isEmpty;
import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore.createNewGroup;
import static opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore.updateGroup;
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
                .setOnMenuItemClickListener(item -> {
                    save();
                    finish();
                    return true;
                });
        return super.onCreateOptionsMenu(menu);
    }

    private void save() {
        List<Contact> selectedContacts = getSelectedContacts();
        ContactGroup group = ContactGroupsDataStore.getGroup(groupNameFromPrevScreen);
        if(group == null) createNewGroup(selectedContacts, groupNameEditText.getText().toString());
        else updateGroup(selectedContacts, groupNameEditText.getText().toString(), group);
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
