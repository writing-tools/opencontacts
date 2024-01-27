package opencontacts.open.com.opencontacts.activities;

import static opencontacts.open.com.opencontacts.activities.EditContactActivity.INTENT_EXTRA_STRING_PHONE_NUMBER;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;

public class AddToContactActivity extends ContactChooserActivityBase {

    private String phoneNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phoneNumber = getIntent().getStringExtra(INTENT_EXTRA_STRING_PHONE_NUMBER);
        if (phoneNumber == null)
            finish();
    }

    @Override
    int title() {
        return R.string.add_to_contact;
    }

    @Override
    public void onContactSelect(Contact selectedContact) {
        Intent editContact = new Intent(AddToContactActivity.this, EditContactActivity.class);
        editContact.putExtra(EditContactActivity.INTENT_EXTRA_CONTACT_CONTACT_DETAILS, selectedContact);
        editContact.putExtra(EditContactActivity.INTENT_EXTRA_STRING_PHONE_NUMBER, phoneNumber);
        AddToContactActivity.this.startActivity(editContact);
    }
}
