package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import opencontacts.open.com.opencontacts.domain.Contact;

import static opencontacts.open.com.opencontacts.activities.EditContactActivity.INTENT_EXTRA_STRING_PHONE_NUMBER;

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
    public void onContactSelect(Contact selectedContact) {
        Intent editContact = new Intent(AddToContactActivity.this, EditContactActivity.class);
        editContact.putExtra(EditContactActivity.INTENT_EXTRA_CONTACT_CONTACT_DETAILS, selectedContact);
        editContact.putExtra(EditContactActivity.INTENT_EXTRA_STRING_PHONE_NUMBER, phoneNumber);
        AddToContactActivity.this.startActivity(editContact);
    }
}
