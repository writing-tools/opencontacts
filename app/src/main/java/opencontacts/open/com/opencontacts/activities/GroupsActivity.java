package opencontacts.open.com.opencontacts.activities;

import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.widget.ArrayAdapter;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactGroupsStore;
import opencontacts.open.com.opencontacts.domain.ContactGroup;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
        allGroups = ContactGroupsStore.getAllGroups();
        if(allGroups.isEmpty()) showAddGroupLayout();
        else setupAndShowGroups();
    }

    private void setupAndShowGroups() {
        if(spinnerAdapter == null)
            spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, allGroups.toArray());
        else{
            spinnerAdapter.clear();
            spinnerAdapter.addAll(allGroups);
        }
        groupNameSpinner.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();
        showGroups();
    }

    private void showAddGroupLayout(){
        findViewById(R.id.add_group_layout).setVisibility(VISIBLE);
        groupNameSpinner.setVisibility(GONE);
        setTitle(R.string.groups);
    }

    private void showGroups() {
        findViewById(R.id.add_group_layout).setVisibility(GONE);
        groupNameSpinner.setVisibility(VISIBLE);
        setTitle("");
    }
}
