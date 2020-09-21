package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.view.Menu;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore;
import opencontacts.open.com.opencontacts.domain.ContactGroup;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static opencontacts.open.com.opencontacts.activities.ContactGroupEditActivity.GROUP_NAME_INTENT_EXTRA;

public class GroupsActivity extends AppBaseActivity {

    private AppCompatSpinner groupNameSpinner;
    private List<ContactGroup> allGroups;
    private ArrayAdapter<Object> spinnerAdapter;

    @Override
    int getLayoutResource() {
        return R.layout.activity_groups;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
        groupNameSpinner = findViewById(R.id.group_name);
    }

    @Override
    protected void onResume() {
        super.onResume();
        allGroups = ContactGroupsDataStore.getAllGroups();
        if(allGroups.isEmpty()) showEmptyGroupsMessage();
        else setupAndShowGroups();
    }

    private void setupAndShowGroups() {
        if(spinnerAdapter == null)
            spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(allGroups));
        else{
            spinnerAdapter.clear();
            spinnerAdapter.addAll(allGroups);
        }
        invalidateOptionsMenu(); //this will show the edit option
        groupNameSpinner.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();
        showGroups();
    }

    private void showEmptyGroupsMessage(){
        findViewById(R.id.empty_groups_textview).setVisibility(VISIBLE);
        groupNameSpinner.setVisibility(GONE);
        setTitle(R.string.groups);
    }

    private void showGroups() {
        findViewById(R.id.empty_groups_textview).setVisibility(GONE);
        groupNameSpinner.setVisibility(VISIBLE);
        setTitle("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(R.string.add_group)
                .setIcon(R.drawable.ic_add_24dp)
                .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
                .setOnMenuItemClickListener(item -> {
                    startActivity(new Intent(GroupsActivity.this, ContactGroupEditActivity.class));
                    return true;
                });
        if(allGroups == null || allGroups.isEmpty()) return super.onCreateOptionsMenu(menu);
        menu.add(R.string.edit_group)
                .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
                .setIcon(R.drawable.edit)
                .setOnMenuItemClickListener(item -> {
                    ContactGroup selectedGroup = (ContactGroup) groupNameSpinner.getSelectedItem();
                    startActivity(
                            new Intent(GroupsActivity.this, ContactGroupEditActivity.class)
                                    .putExtra(GROUP_NAME_INTENT_EXTRA, selectedGroup == null ? "" : selectedGroup.getName())
                    );
                    return true;
                });
        return super.onCreateOptionsMenu(menu);
    }

}
