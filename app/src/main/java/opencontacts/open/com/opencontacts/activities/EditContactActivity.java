package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.github.underscore.U;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;

import static android.text.TextUtils.isEmpty;
import static android.view.ViewGroup.LayoutParams.*;

public class EditContactActivity extends AppBaseActivity {
    Contact contact = null;
    public static final String INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT = "add_new_contact";
    public static final String INTENT_EXTRA_CONTACT_CONTACT_DETAILS = "contact_details";
    public static final String INTENT_EXTRA_STRING_PHONE_NUMBER = "phone_number";
    EditText editText_firstName;
    EditText editText_lastName;
    EditText editText_mobileNumber;
    private boolean addingNewContact = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editText_firstName = findViewById(R.id.editFirstName);
        editText_lastName = findViewById(R.id.editLastName);
        editText_mobileNumber = findViewById(R.id.editPhoneNumber);

        Intent intent = getIntent();
        if(intent.getBooleanExtra(INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, false)) {
            addingNewContact = true;
            editText_mobileNumber.setText(intent.getStringExtra(INTENT_EXTRA_STRING_PHONE_NUMBER));
            toolbar.setTitle(R.string.new_contact);
        }
        else{
            contact = (Contact) intent.getSerializableExtra(INTENT_EXTRA_CONTACT_CONTACT_DETAILS);
            if(contact.id == -1){
                Toast.makeText(this, R.string.error_while_loading_contact, Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
            toolbar.setTitle(contact.firstName);
            fillFieldsFromContactDetails();
        }
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_edit_contact;
    }

    private void fillFieldsFromContactDetails() {
        editText_firstName.setText(contact.firstName);
        editText_lastName.setText(contact.lastName);
        editText_mobileNumber.setText(contact.phoneNumbers.get(0).phoneNumber);
        List<String> phoneNumbers = U.map(contact.phoneNumbers, arg -> arg.phoneNumber);
        if(phoneNumbers.size() > 1)
            for(int i = 1, totalNumbers = phoneNumbers.size(); i < totalNumbers; i++){
                addOneMorePhoneNumberView(null).setText(phoneNumbers.get(i));
            }
    }

    public void saveContact(View view) {
        String firstName = String.valueOf(editText_firstName.getText());
        String lastName = String.valueOf(editText_lastName.getText());
        if(isEmpty(firstName) && isEmpty(lastName)){
            editText_firstName.setError(getString(R.string.required_firstname_or_lastname));
            return;
        }
        if(phoneNumbersNotEntered()){
            editText_mobileNumber.setError(getString(R.string.required));
            return;
        }

        if(addingNewContact)
            ContactsDataStore.addContact(firstName, lastName, getPhoneNumbersFromView(), EditContactActivity.this);
        else{
            Contact updatedContact = new Contact(this.contact.id, firstName, lastName, getPhoneNumbersFromView());
            updatedContact.primaryPhoneNumber = contact.primaryPhoneNumber;
            ContactsDataStore.updateContact(updatedContact, this);
        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean phoneNumbersNotEntered() {
        return getPhoneNumbersFromView().isEmpty();
    }

    private List<PhoneNumber> getPhoneNumbersFromView() {
        LinearLayout phoneNumbersContainer = findViewById(R.id.phonenumbers);
        int numberOfPhoneNumbers = phoneNumbersContainer.getChildCount();
        String extraPhoneNumber;
        ArrayList<String> phoneNumbers = new ArrayList<>(numberOfPhoneNumbers);
        for(int i=0; i<numberOfPhoneNumbers; i++){
            extraPhoneNumber = String.valueOf(((EditText) phoneNumbersContainer.getChildAt(i)).getText()).trim();
            if(isEmpty(extraPhoneNumber))
                continue;
            phoneNumbers.add(extraPhoneNumber);
        }
        return U.map(phoneNumbers, PhoneNumber::new);
    }

    public EditText addOneMorePhoneNumberView(View view){
        LinearLayout phoneNumbers_linearLayout = findViewById(R.id.phonenumbers);
        EditText oneMorePhoneNumberField = new EditText(this);
        oneMorePhoneNumberField.setLayoutParams(new ActionBar.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        oneMorePhoneNumberField.setInputType(InputType.TYPE_CLASS_PHONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            oneMorePhoneNumberField.setBackground((findViewById(R.id.editPhoneNumber)).getBackground());
        }
        else
            oneMorePhoneNumberField.setBackgroundDrawable((findViewById(R.id.editPhoneNumber)).getBackground());
        oneMorePhoneNumberField.setHint(R.string.phone_number);
        phoneNumbers_linearLayout.addView(oneMorePhoneNumberField);
        return oneMorePhoneNumberField;
    }
}
