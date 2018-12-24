package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


import com.github.underscore.U;

import java.io.IOException;
import java.util.List;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.InputFieldCollection;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

import static android.text.TextUtils.isEmpty;

public class EditContactActivity extends AppBaseActivity {
    Contact contact = null;
    public static final String INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT = "add_new_contact";
    public static final String INTENT_EXTRA_CONTACT_CONTACT_DETAILS = "contact_details";
    public static final String INTENT_EXTRA_STRING_PHONE_NUMBER = "phone_number";
    EditText editText_firstName;
    EditText editText_lastName;
    private boolean addingNewContact = false;
    private VCard vcardBeforeEdit;
    private InputFieldCollection phoneNumbersInputCollection;
    private InputFieldCollection emailsInputCollection;
    private InputFieldCollection addressesInputCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editText_firstName = findViewById(R.id.editFirstName);
        editText_lastName = findViewById(R.id.editLastName);
        phoneNumbersInputCollection = findViewById(R.id.phonenumbers);
        emailsInputCollection = findViewById(R.id.emails);
        addressesInputCollection = findViewById(R.id.addresses);

        Intent intent = getIntent();
        if(intent.getBooleanExtra(INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, false)) {
            addingNewContact = true;
            toolbar.setTitle(R.string.new_contact);
            vcardBeforeEdit = new VCard();
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
        }
        fillFieldsFromContactDetails();
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_edit_contact;
    }

    private void fillFieldsFromContactDetails() {
        fillTelephoneNumbers();
        fillEmails();
        fillAddress();

        if(addingNewContact) return;

        editText_firstName.setText(contact.firstName);
        editText_lastName.setText(contact.lastName);
    }

    private void fillAddress() {
        List<Address> addresses = vcardBeforeEdit.getAddresses();
        if(U.isEmpty(addresses)) {
            addressesInputCollection.addOneMoreView(null);
            return;
        }
        U.forEach(addresses, address -> addressesInputCollection.addOneMoreView(address.getStreetAddress(), DomainUtils.getAddressTypeTranslatedText(address.getTypes(), EditContactActivity.this)));
    }

    private void fillEmails() {
        List<Email> emails = vcardBeforeEdit.getEmails();
        if(U.isEmpty(emails)) {
            emailsInputCollection.addOneMoreView(null);
            return;
        }
        U.forEach(emails, email -> emailsInputCollection.addOneMoreView(email.getValue(), DomainUtils.getEmailTypeTranslatedText(email.getTypes(), EditContactActivity.this)));
    }

    private void fillTelephoneNumbers() {
        List<Telephone> telephoneNumbers = vcardBeforeEdit.getTelephoneNumbers();
        String newPhoneNumberToBeAdded = getIntent().getStringExtra(INTENT_EXTRA_STRING_PHONE_NUMBER);
        if(U.isEmpty(telephoneNumbers)) {
            phoneNumbersInputCollection.addOneMoreView(newPhoneNumberToBeAdded, "");
            return;
        }
        U.forEach(telephoneNumbers, telephoneNumber -> phoneNumbersInputCollection.addOneMoreView(telephoneNumber.getText(), DomainUtils.getMobileNumberTypeTranslatedText(telephoneNumber.getTypes(), EditContactActivity.this)));
        if(newPhoneNumberToBeAdded != null) phoneNumbersInputCollection.addOneMoreView(newPhoneNumberToBeAdded, "");
    }

    public void saveContact(View view) {
        String firstName = String.valueOf(editText_firstName.getText());
        String lastName = String.valueOf(editText_lastName.getText());
        if (warnIfNotFilledMandatoryFields(firstName, lastName)) return;

        VCard vcardAfterEdit = createVCardFromInputFields(firstName, lastName);
        if(addingNewContact) {
            ContactsDataStore.addContact(vcardAfterEdit, this);
        }
        else{
            ContactsDataStore.updateContact(contact.id, contact.primaryPhoneNumber, vcardAfterEdit, this);
        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean warnIfNotFilledMandatoryFields(String firstName, String lastName) {
        if(isEmpty(firstName) && isEmpty(lastName)){
            editText_firstName.setError(getString(R.string.required_firstname_or_lastname));
            return true;
        }
        if(phoneNumbersInputCollection.isEmpty()){
            phoneNumbersInputCollection.getFieldAt(0).editText.setError(getString(R.string.required));
            return true;
        }
        return false;
    }

    private VCard createVCardFromInputFields(String firstName, String lastName) {
        VCard newVCard = new VCard();

        newVCard.setStructuredName(getStructuredName(firstName, lastName));
        addTelephoneNumbersFromFieldsToNewVCard(newVCard);
        addEmailsFromFieldsToNewVCard(newVCard);
        addAddressFromFieldsToNewVCard(newVCard);

        return newVCard;
    }

    private void addAddressFromFieldsToNewVCard(VCard newVCard) {
        if(addressesInputCollection.isEmpty()) return;
        U.chain(addressesInputCollection.getValuesAndTypes())
                .map(this::createAddress)
                .forEach(newVCard::addAddress);
    }

    private void addEmailsFromFieldsToNewVCard(VCard newVCard) {
        if(emailsInputCollection.isEmpty()) return;
        U.chain(emailsInputCollection.getValuesAndTypes())
                .map(this::createEmail)
                .forEach(newVCard::addEmail);
    }

    private void addTelephoneNumbersFromFieldsToNewVCard(VCard newVCard) {
        if(phoneNumbersInputCollection.isEmpty()) return;
        U.chain(phoneNumbersInputCollection.getValuesAndTypes())
                .map(this::createTelephone)
                .forEach(newVCard::addTelephoneNumber);
    }

    @NonNull
    private StructuredName getStructuredName(String firstName, String lastName) {
        StructuredName structuredName = new StructuredName();
        structuredName.setGiven(firstName);
        structuredName.setFamily(lastName);
        return structuredName;
    }

    private Address createAddress(Pair<String, String> addressAndTypePair) {
        Address address = new Address();
        address.setStreetAddress(addressAndTypePair.first);
        address.getTypes().add(DomainUtils.getAddressType(addressAndTypePair.second, this));
        return address;
    }

    private Email createEmail(Pair<String, String> emailAndTypePair) {
        Email email = new Email(emailAndTypePair.first);
        email.getTypes().add(DomainUtils.getEmailType(emailAndTypePair.second, this));
        return email;
    }

    private Telephone createTelephone(Pair<String, String> phoneNumberAndTypePair) {
        Telephone telephone = new Telephone(phoneNumberAndTypePair.first);
        telephone.getTypes().add(DomainUtils.getMobileNumberType(phoneNumberAndTypePair.second, this));
        return telephone;
    }
}
