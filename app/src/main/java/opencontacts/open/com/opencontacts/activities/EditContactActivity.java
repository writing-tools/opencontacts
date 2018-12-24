package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.github.underscore.U;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

import static android.text.TextUtils.isEmpty;
import static opencontacts.open.com.opencontacts.utils.Common.mapIndexes;

public class EditContactActivity extends AppBaseActivity {
    Contact contact = null;
    public static final String INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT = "add_new_contact";
    public static final String INTENT_EXTRA_CONTACT_CONTACT_DETAILS = "contact_details";
    public static final String INTENT_EXTRA_STRING_PHONE_NUMBER = "phone_number";
    EditText editText_firstName;
    EditText editText_lastName;
    EditText editText_mobileNumber;
    private boolean addingNewContact = false;
    private String[] phoneNumberTypesInSpinner;
    private VCard vcardBeforeEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editText_firstName = findViewById(R.id.editFirstName);
        editText_lastName = findViewById(R.id.editLastName);
        editText_mobileNumber = findViewById(R.id.editPhoneNumber);
        phoneNumberTypesInSpinner = getResources().getStringArray(R.array.phone_number_types);
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
            try {
                vcardBeforeEdit = new VCardReader(ContactsDataStore.getVCardData(contact.id).vcardDataAsString).readNext();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        if(vcardBeforeEdit == null)
            return;
        List<Telephone> telephoneNumbers = vcardBeforeEdit.getTelephoneNumbers();
        Telephone telephone = U.first(telephoneNumbers);
        editText_mobileNumber.setText(telephone.getText());
        String mobileNumberTypeTranslatedText = DomainUtils.getMobileNumberTypeTranslatedText(telephone.getTypes(), EditContactActivity.this);
        int phoneNumberTypeIndexInSpinner = U.findIndex(phoneNumberTypesInSpinner, arg -> arg.equals(mobileNumberTypeTranslatedText));
        ((AppCompatSpinner)findViewById(R.id.phone_number_type)).setSelection(phoneNumberTypeIndexInSpinner);

        U.forEach(U.rest(telephoneNumbers), this::addPhoneNumberViewFor);
    }

    private void addPhoneNumberViewFor(Telephone telephone) {
        LinearLayout linearLayout = addOneMorePhoneNumberView(null);
        ((AppCompatEditText)linearLayout.findViewById(R.id.editPhoneNumber)).setText(telephone.getText());
        String mobileNumberTypeTranslatedText = DomainUtils.getMobileNumberTypeTranslatedText(telephone.getTypes(), EditContactActivity.this);
        int phoneNumberTypeIndexInSpinner = U.findIndex(phoneNumberTypesInSpinner, arg -> arg.equals(mobileNumberTypeTranslatedText));
        ((AppCompatSpinner)linearLayout.findViewById(R.id.phone_number_type)).setSelection(phoneNumberTypeIndexInSpinner);
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
        List<Pair<PhoneNumber, TelephoneType>> phoneNumbersAndTypeList = getPhoneNumbersAndTelephoneTypesFromView();
        VCard newVCardData = createVCard(firstName, lastName, phoneNumbersAndTypeList);
        if(addingNewContact)
            ContactsDataStore.addContact(firstName, lastName, U.map(phoneNumbersAndTypeList, pair -> pair.first), newVCardData);
        else{
            Contact updatedContact = new Contact(this.contact.id, firstName, lastName, U.map(phoneNumbersAndTypeList, pair -> pair.first));
            updatedContact.primaryPhoneNumber = contact.primaryPhoneNumber;
            ContactsDataStore.updateContact(updatedContact, newVCardData);
        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private VCard createVCard(String firstName, String lastName, List<Pair<PhoneNumber, TelephoneType>> phoneNumbersAndTypeList) {
        VCard newVCard = new VCard();
        StructuredName structuredName = new StructuredName();
        structuredName.setGiven(firstName);
        structuredName.setFamily(lastName);
        newVCard.setStructuredName(structuredName);
        U.forEach(phoneNumbersAndTypeList, pair -> newVCard.addTelephoneNumber(pair.first.phoneNumber, pair.second));
        U.forEach(getEmailFromView(), newVCard::addEmail);
        U.forEach(getAddressFromView(), newVCard::addAddress);
        return newVCard;
    }

    private List<Address> getAddressFromView() {
        return new ArrayList<>();
    }

    private List<Email> getEmailFromView() {
        return new ArrayList<>();
    }

    private boolean phoneNumbersNotEntered() {
        return getPhoneNumbersAndTelephoneTypesFromView().isEmpty();
    }

    private List<Pair<PhoneNumber, TelephoneType>> getPhoneNumbersAndTelephoneTypesFromView() {
        LinearLayout phoneNumbersContainer = findViewById(R.id.phonenumbers);
        int numberOfPhoneNumbers = phoneNumbersContainer.getChildCount();
        return U.chain(mapIndexes(numberOfPhoneNumbers, phoneNumbersContainer::getChildAt))
                .map(this::createPhoneNumberAndTypePair)
                .value();
    }

    private Pair<PhoneNumber, TelephoneType> createPhoneNumberAndTypePair(View phoneNumberHolder) {
        String phoneNumber = ((AppCompatEditText) (phoneNumberHolder.findViewById(R.id.editPhoneNumber))).getText().toString();
        String selectedPhoneNumberTypeInSpinner = (String) ((AppCompatSpinner) phoneNumberHolder.findViewById(R.id.phone_number_type)).getSelectedItem();
        return new Pair<>(new PhoneNumber(phoneNumber), DomainUtils.getMobileNumberType(selectedPhoneNumberTypeInSpinner, this));
    }

    public LinearLayout addOneMorePhoneNumberView(View view){
        LinearLayout phoneNumbers_linearLayout = findViewById(R.id.phonenumbers);
        LinearLayout inflatedLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.layout_edit_phone_number_and_type, phoneNumbers_linearLayout, false);
        phoneNumbers_linearLayout.addView(inflatedLayout);
        return inflatedLayout;
    }
}
