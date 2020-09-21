package opencontacts.open.com.opencontacts.activities;

import android.os.Bundle;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;


public class ContactGroupEditActivity extends ContactChooserActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onContactSelect(Contact selectedContact) {

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
