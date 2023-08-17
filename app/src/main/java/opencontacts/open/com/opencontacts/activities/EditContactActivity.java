package opencontacts.open.com.opencontacts.activities;

import static android.text.TextUtils.isEmpty;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.*;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.addTemporaryContact;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.wrapInConfirmation;
import static opencontacts.open.com.opencontacts.utils.Common.getCalendarInstanceAt;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.defaultPhoneNumberTypeTranslatedText;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getMobileNumber;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.util.Pair;

import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.github.underscore.U;
import com.thomashaertel.widget.MultiSpinner;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.property.Address;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.Note;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Url;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.fieldcollections.addressfieldcollection.AddressFieldCollection;
import opencontacts.open.com.opencontacts.components.fieldcollections.textinputspinnerfieldcollection.TextInputAndSpinnerFieldCollection;
import opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.domain.ContactGroup;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.DomainUtils;
import opencontacts.open.com.opencontacts.utils.SpinnerUtil;
import opencontacts.open.com.opencontacts.utils.VCardUtils;

public class EditContactActivity extends AppBaseActivity {
    Contact contact = null;
    boolean isTemporaryContactBefore = false;
    public static final String INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT = "add_new_contact";
    public static final String INTENT_EXTRA_CONTACT_CONTACT_DETAILS = "contact_details";
    public static final String INTENT_EXTRA_STRING_PHONE_NUMBER = ContactsContract.Intents.Insert.PHONE;
    EditText editText_firstName;
    EditText editText_lastName;
    private boolean addingNewContact = false;
    private VCard vcardBeforeEdit;
    private TextInputAndSpinnerFieldCollection phoneNumbersInputCollection;
    private TextInputAndSpinnerFieldCollection emailsInputCollection;
    private AddressFieldCollection addressesInputCollection;
    private TextInputEditText notesTextInputEditText;
    private TextInputEditText websiteTextInputEditText;
    private TextInputEditText dateOfBirthTextInputEditText;
    private AppCompatCheckBox temporaryContactCheckbox;
    private Date selectedBirthDay;
    private MultiSpinner groupsSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attachViewsToInstanceVariables();
        initializeVCard();
        fillFieldsFromVCard();
        addClickListenersToViews();
    }

    private void initializeVCard() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra(INTENT_EXTRA_BOOLEAN_ADD_NEW_CONTACT, false)) {
            addingNewContact = true;
            toolbar.setTitle(R.string.new_contact);
            vcardBeforeEdit = new VCard();
        } else {
            contact = (Contact) intent.getSerializableExtra(INTENT_EXTRA_CONTACT_CONTACT_DETAILS);
            if (contact.id == -1) {
                Toast.makeText(this, R.string.error_while_loading_contact, Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
            toolbar.setTitle(contact.firstName);
            isTemporaryContactBefore = ContactsDataStore.isTemporary(contact.id);
            try {
                vcardBeforeEdit = new VCardReader(getVCardData(contact.id).vcardDataAsString).readNext();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addClickListenersToViews() {
        View.OnClickListener onBirthDayClickListener = v -> {
            Birthday birthday = vcardBeforeEdit.getBirthday();
            Calendar dateOfBirthInstance = Calendar.getInstance();
            if (selectedBirthDay != null)
                dateOfBirthInstance = getCalendarInstanceAt(selectedBirthDay.getTime());
            else if (birthday != null)
                dateOfBirthInstance = getCalendarInstanceAt(birthday.getDate().getTime());
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar selectedCalendar = getCalendarInstanceAt(year, month, dayOfMonth);
                ((TextInputEditText) v).setText(AndroidUtils.getFormattedDate(selectedCalendar.getTime()));
                selectedBirthDay = selectedCalendar.getTime();
            }, dateOfBirthInstance.get(YEAR), dateOfBirthInstance.get(MONTH), dateOfBirthInstance.get(DAY_OF_MONTH)).show();
        };
        findViewById(R.id.date_of_birth).setOnClickListener(onBirthDayClickListener);
        temporaryContactCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) wrapInConfirmation(() -> {}, () -> buttonView.setChecked(false), R.string.mark_contact_as_temporary, this);
        });

    }

    private void attachViewsToInstanceVariables() {
        editText_firstName = findViewById(R.id.editFirstName);
        editText_lastName = findViewById(R.id.editLastName);
        phoneNumbersInputCollection = findViewById(R.id.phonenumbers);
        emailsInputCollection = findViewById(R.id.emails);
        addressesInputCollection = findViewById(R.id.addresses);
        notesTextInputEditText = findViewById(R.id.notes);
        websiteTextInputEditText = findViewById(R.id.website);
        dateOfBirthTextInputEditText = findViewById(R.id.date_of_birth);
        groupsSpinner = findViewById(R.id.groups);
        temporaryContactCheckbox = findViewById(R.id.temp_contact_checkbox);
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_edit_contact;
    }

    private void fillFieldsFromVCard() {
        fillTelephoneNumbers();
        fillEmails();
        fillAddress();
        fillNotes();
        fillWebsite();
        fillDateOfBirth();
        fillGroups();

        if (addingNewContact) return;

        editText_firstName.setText(contact.firstName);
        editText_lastName.setText(contact.lastName);
        temporaryContactCheckbox.setChecked(isTemporaryContactBefore);
    }

    private void fillGroups() {
        ArrayAdapter<String> groupsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        groupsSpinner.setAdapter(groupsAdapter, false, selected -> {
        });
        List<String> allGroups = getAllGroupNames();
        groupsAdapter.addAll(allGroups);
        if (contact == null) return;
        SpinnerUtil.setSelection(contact.getGroupNames(), allGroups, groupsSpinner);
    }

    private List<String> getAllGroupNames() {
        return U.chain(ContactGroupsDataStore.getAllGroups())
            .map(ContactGroup::getName)
            .value();
    }

    private void fillDateOfBirth() {
        Birthday birthday = vcardBeforeEdit.getBirthday();
        if (birthday == null) return;
        dateOfBirthTextInputEditText.setText(AndroidUtils.getFormattedDate(birthday.getDate()));
    }

    private void fillWebsite() {
        Url url = U.firstOrNull(vcardBeforeEdit.getUrls());
        if (url == null) return;
        websiteTextInputEditText.setText(url.getValue());
    }

    private void fillNotes() {
        Note note = U.firstOrNull(vcardBeforeEdit.getNotes());
        if (note == null) return;
        notesTextInputEditText.setText(note.getValue());
    }

    private void fillAddress() {
        addressesInputCollection.setAddresses(vcardBeforeEdit.getAddresses());
    }

    private void fillEmails() {
        List<Email> emails = vcardBeforeEdit.getEmails();
        if (U.isEmpty(emails)) {
            emailsInputCollection.addOneMoreView();
            return;
        }
        U.forEach(emails, email -> emailsInputCollection.addOneMoreView(email.getValue(), DomainUtils.getEmailTypeTranslatedText(email.getTypes(), EditContactActivity.this)));
    }

    private void fillTelephoneNumbers() {
        List<Telephone> telephoneNumbers = vcardBeforeEdit.getTelephoneNumbers();
        String newPhoneNumberToBeAdded = getIntent().getStringExtra(INTENT_EXTRA_STRING_PHONE_NUMBER);
        if (U.isEmpty(telephoneNumbers)) {
            phoneNumbersInputCollection.addOneMoreView(newPhoneNumberToBeAdded, defaultPhoneNumberTypeTranslatedText);
            return;
        }
        U.forEach(telephoneNumbers, telephoneNumber -> phoneNumbersInputCollection.addOneMoreView(getMobileNumber(telephoneNumber), DomainUtils.getMobileNumberTypeTranslatedText(telephoneNumber.getTypes(), EditContactActivity.this)));
        if (newPhoneNumberToBeAdded != null)
            phoneNumbersInputCollection.addOneMoreView(newPhoneNumberToBeAdded, defaultPhoneNumberTypeTranslatedText);
    }

    public void saveContact(View view) {
        String firstName = String.valueOf(editText_firstName.getText());
        String lastName = String.valueOf(editText_lastName.getText());
        if (warnIfMandatoryFieldsAreNotFilled(firstName, lastName)) return;

        VCard vcardAfterEdit = createVCardFromInputFields(firstName, lastName);
        boolean markAsTemporary = temporaryContactCheckbox.isChecked();
        if (addingNewContact) {
            if (markAsTemporary) addTemporaryContact(vcardAfterEdit, this);
            else addContact(vcardAfterEdit, this);
        } else {
            updateContact(contact.id, contact.primaryPhoneNumber.phoneNumber, vcardAfterEdit, this);
            updateTemporaryStatus(markAsTemporary, contact.id);
        }
        if (temporaryContactCheckbox.isChecked()) {

        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean warnIfMandatoryFieldsAreNotFilled(String firstName, String lastName) {
        if (isEmpty(firstName) && isEmpty(lastName)) {
            editText_firstName.setError(getString(R.string.required_firstname_or_lastname));
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
        addNotesFromFieldsToNewVCard(newVCard);
        addWebsiteFromFieldsToNewVCard(newVCard);
        addDateOfBirthFromFieldsToNewVCard(newVCard);
        addGroupsToNewVCard(newVCard);
        return newVCard;
    }

    private void addGroupsToNewVCard(VCard newVCard) {
        List<String> newGroupNames = SpinnerUtil.getSelectedItems(groupsSpinner, getAllGroupNames());
        if (newGroupNames.isEmpty()) return;
        VCardUtils.setCategories(newGroupNames, newVCard);
    }

    private void addDateOfBirthFromFieldsToNewVCard(VCard newVCard) {
        if (selectedBirthDay == null) {
            newVCard.setBirthday(vcardBeforeEdit == null ? null : vcardBeforeEdit.getBirthday());
            return;
        }
        newVCard.setBirthday(new Birthday(selectedBirthDay));
    }

    private void addWebsiteFromFieldsToNewVCard(VCard newVCard) {
        String website = websiteTextInputEditText.getText().toString();
        if (TextUtils.isEmpty(website)) return;
        newVCard.addUrl(new Url(website));
    }

    private void addNotesFromFieldsToNewVCard(VCard newVCard) {
        String notes = notesTextInputEditText.getText().toString();
        if (TextUtils.isEmpty(notes)) return;
        newVCard.addNote(notes);
    }

    private void addAddressFromFieldsToNewVCard(VCard newVCard) {
        if (addressesInputCollection.isEmpty()) return;
        U.chain(addressesInputCollection.getAllAddresses())
            .forEach(newVCard::addAddress);
    }

    private void addEmailsFromFieldsToNewVCard(VCard newVCard) {
        if (emailsInputCollection.isEmpty()) return;
        U.chain(emailsInputCollection.getValuesAndTypes())
            .map(this::createEmail)
            .forEach(newVCard::addEmail);
    }

    private void addTelephoneNumbersFromFieldsToNewVCard(VCard newVCard) {
        if (phoneNumbersInputCollection.isEmpty()) return;
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

    private Address createAddress(int index, Pair<String, String> addressAndTypePair) {
        Address address = U.elementAtOrElse(vcardBeforeEdit.getAddresses(), index, new Address());
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

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.add(R.string.save)
            .setIcon(R.drawable.ic_save_black_24dp)
            .setOnMenuItemClickListener(item -> {
                saveContact(null);
                return true;
            })
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

}
