package opencontacts.open.com.opencontacts.activities;

import static android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM;
import static android.widget.RelativeLayout.ALIGN_PARENT_RIGHT;
import static opencontacts.open.com.opencontacts.components.TintedDrawablesStore.setDrawableForFAB;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.mergeContacts;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getMenuItemClickHandlerFor;
import static opencontacts.open.com.opencontacts.utils.ContactsAutoMergeUtils.autoMergeByName;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import android.view.Menu;
import android.widget.RelativeLayout;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

public class MergeContactsActivity extends ContactChooserActivityBase {
    @Override
    public void onContactSelect(Contact selectedContact) {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        moveContactsListViewTopToAccomodateFABButton();
        addFABMergeButton();
    }

    private void moveContactsListViewTopToAccomodateFABButton() {
        RelativeLayout.LayoutParams contactsListViewParams = (RelativeLayout.LayoutParams) contactsListView.getLayoutParams();
        contactsListViewParams.bottomMargin = (int) AndroidUtils.dpToPixels(50);
        contactsListView.setLayoutParams(contactsListViewParams);
    }

    private void addFABMergeButton() {
        FloatingActionButton fabMergeButton = new FloatingActionButton(this);
        setDrawableForFAB(R.drawable.ic_group_merge_contacts_24dp, fabMergeButton, this);
        int sizeOfFabInPixels = (int) AndroidUtils.dpToPixels(100);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(sizeOfFabInPixels, sizeOfFabInPixels);
        layoutParams.rightMargin = (int) AndroidUtils.dpToPixels(20);
        layoutParams.addRule(ALIGN_PARENT_RIGHT);
        layoutParams.addRule(ALIGN_PARENT_BOTTOM);
        ((RelativeLayout) findViewById(R.id.contacts_list)).addView(fabMergeButton, layoutParams);

        fabMergeButton.setOnClickListener(v -> mergeContacts(getSelectedContacts(), MergeContactsActivity.this));
    }

    @Override
    public int getTitleResource() {
        return R.string.merge;
    }

    @Override
    public boolean shouldEnableMultiSelect() {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(getString(R.string.Automerge))
            .setOnMenuItemClickListener(getMenuItemClickHandlerFor(this::launchAutoMergeOptions));
        return super.onCreateOptionsMenu(menu);
    }

    private void launchAutoMergeOptions() {
        AlertDialog pleaseWaitDialog = new AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage("Please wait...")
            .show();
        autoMergeByName(this);
        pleaseWaitDialog.dismiss();
    }

}
