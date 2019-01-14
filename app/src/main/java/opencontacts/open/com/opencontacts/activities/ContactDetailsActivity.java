package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.underscore.U;

import java.io.IOException;
import java.util.List;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Note;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.ExpandedList;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getMobileNumberTypeTranslatedText;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getMobileNumber;


public class ContactDetailsActivity extends AppBaseActivity {
    private long contactId;
    private Contact contact;
    private VCard vcard;
    private LayoutInflater layoutInflater;
    private LinearLayout phoneNumbersLinearLayout;
    private LinearLayout emailAddressLinearLayout;
    private LinearLayout addressLinearLayout;

    private View.OnClickListener callContact = v -> AndroidUtils.call(getSelectedMobileNumber(v), ContactDetailsActivity.this);

    private View.OnClickListener togglePrimaryNumber = v -> {
        ContactsDataStore.togglePrimaryNumber(getSelectedMobileNumber((View)v.getParent()), contactId);
        contact = ContactsDataStore.getContactWithId(contactId);
        fillPhoneNumbers();
    };

    private View.OnClickListener messageContact = v -> AndroidUtils.message(getSelectedMobileNumber((View)v.getParent()), ContactDetailsActivity.this);

    private View.OnClickListener whatsappContact = v -> AndroidUtils.whatsapp(getSelectedMobileNumber((View)v.getParent()), ContactDetailsActivity.this);

    private View.OnLongClickListener copyPhoneNumberToClipboard = v -> {
        AndroidUtils.copyToClipboard(getSelectedMobileNumber(v), ContactDetailsActivity.this);
        Toast.makeText(ContactDetailsActivity.this, R.string.copied_phonenumber_to_clipboard, Toast.LENGTH_SHORT).show();
        return true;
    };
    private boolean shouldShowWhatsappIcon;

    private String getSelectedMobileNumber(View v){
        return v.getTag().toString();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        contactId = intent.getLongExtra(MainActivity.INTENT_EXTRA_LONG_CONTACT_ID, -1);
        if(contactId == -1)
            showInvalidContactErrorAndExit();
        shouldShowWhatsappIcon = AndroidUtils.isWhatsappIntegrationEnabled(this);
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_contact_details;
    }

    @Override
    protected void onResume() {
        super.onResume();
        contact = ContactsDataStore.getContactWithId(contactId);
        VCardData vcardData = ContactsDataStore.getVCardData(contactId);
        if(contact == null)
            showInvalidContactErrorAndExit();
        if(vcardData != null){
            try {
                vcard = new VCardReader(vcardData.vcardDataAsString).readNext();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error_while_parsing_contact_details, Toast.LENGTH_SHORT).show();
            }
        }
        setUpUI();
    }

    private void showInvalidContactErrorAndExit() {
        Toast.makeText(this, R.string.error_while_loading_contact, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void setUpUI() {
        toolbar.setTitle(contact.firstName);
        toolbar.setSubtitle(contact.lastName);
        phoneNumbersLinearLayout = findViewById(R.id.phone_numbers_list);
        emailAddressLinearLayout = findViewById(R.id.email_address_list);
        addressLinearLayout = findViewById(R.id.address_list);
        layoutInflater = getLayoutInflater();
        if(vcard == null) return;
        fillPhoneNumbers();
        fillEmailAddress();
        fillAddress();
        fillNotes();
    }

    private void fillNotes() {
        Note note = U.firstOrNull(vcard.getNotes());
        View notesCard  = findViewById(R.id.notes_card);
        if(note == null) {
            notesCard.setVisibility(GONE);
            return;
        }
        AppCompatTextView notesTextView = notesCard.findViewById(R.id.text_view);
        notesTextView.setText(note.getValue());
        notesTextView.setOnLongClickListener(v -> {
            AndroidUtils.copyToClipboard(notesTextView.getText().toString(), true, this);
            return true;
        });
        notesCard.setVisibility(VISIBLE);
    }

    private void fillAddress() {
        if(U.isEmpty(vcard.getAddresses())) {
            findViewById(R.id.address_card).setVisibility(GONE);
            return;
        }
        findViewById(R.id.address_card).setVisibility(VISIBLE);
        addressLinearLayout.removeAllViews();
        List<Address> addresses = vcard.getAddresses();
        ExpandedList addressesExpandedListView = new ExpandedList.Builder(this)
                .withOnItemClickListener(index -> { })
                .withItems(U.map(addresses, address -> new Pair<>(address.getStreetAddress(), DomainUtils.getAddressTypeTranslatedText(address.getTypes(), this))))
                .withOnItemLongClickListener(index -> AndroidUtils.copyToClipboard(addresses.get(index).getStreetAddress(), true, this))
                .build();
        addressLinearLayout.addView(addressesExpandedListView);
    }

    private void fillEmailAddress() {
        if(U.isEmpty(vcard.getEmails())) {
            findViewById(R.id.email_card).setVisibility(GONE);
            return;
        }
        findViewById(R.id.email_card).setVisibility(VISIBLE);
        emailAddressLinearLayout.removeAllViews();
        List<Email> emails = vcard.getEmails();
        ExpandedList emailsExpandedListView = new ExpandedList.Builder(this)
                .withOnItemClickListener(index -> AndroidUtils.email(emails.get(index).getValue(), this))
                .withItems(U.map(emails, email -> new Pair<>(email.getValue(), DomainUtils.getEmailTypeTranslatedText(email.getTypes(), this))))
                .withOnItemLongClickListener(index -> AndroidUtils.copyToClipboard(emails.get(index).getValue(),  true, this))
                .build();
        emailAddressLinearLayout.addView(emailsExpandedListView);
    }

    private void fillPhoneNumbers() {
        if(U.isEmpty(vcard.getTelephoneNumbers())) {
            findViewById(R.id.phone_card).setVisibility(GONE);
            return;
        }
        findViewById(R.id.phone_card).setVisibility(VISIBLE);
        phoneNumbersLinearLayout.removeAllViews();
        U.forEach(vcard.getTelephoneNumbers(), telephone -> {
            View inflatedView = layoutInflater.inflate(R.layout.contact_details_row, phoneNumbersLinearLayout, false);
            String telephoneText = getMobileNumber(telephone);
            ((TextView) inflatedView.findViewById(R.id.textview_phone_number)).setText(telephoneText);
            AppCompatImageButton primaryNumberToggleButton = inflatedView.findViewById(R.id.button_primary_number);
            primaryNumberToggleButton.setImageResource(telephoneText.equals(contact.primaryPhoneNumber.phoneNumber) ? R.drawable.ic_star_filled_24dp : R.drawable.ic_star_empty_24dp);
            primaryNumberToggleButton.setOnClickListener(togglePrimaryNumber);
            inflatedView.findViewById(R.id.button_message).setOnClickListener(messageContact);
            View whatsappIcon = inflatedView.findViewById(R.id.button_whatsapp);
            if(shouldShowWhatsappIcon){
                whatsappIcon.setOnClickListener(whatsappContact);
                whatsappIcon.setVisibility(VISIBLE);
            }
            ((AppCompatTextView)inflatedView.findViewById(R.id.phone_number_type)).setText(getMobileNumberTypeTranslatedText(telephone.getTypes(),this));
            inflatedView.setOnClickListener(callContact);
            inflatedView.setOnLongClickListener(copyPhoneNumberToClipboard);
            inflatedView.setTag(telephoneText);
            phoneNumbersLinearLayout.addView(inflatedView);
        });
    }

    private void exportToContactsApp() {
        Intent exportToContactsAppIntent = AndroidUtils.getIntentToExportContactToNativeContactsApp(contact);
        startActivity(exportToContactsAppIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.contact_details_menu, menu);
        menu.findItem(R.id.image_button_export_to_contacts_app).setOnMenuItemClickListener(item -> {
            exportToContactsApp();
            return true;
        });
        menu.findItem(R.id.image_button_edit_contact).setOnMenuItemClickListener(item -> {
            Intent editContact = new Intent(ContactDetailsActivity.this, EditContactActivity.class);
            editContact.putExtra(EditContactActivity.INTENT_EXTRA_CONTACT_CONTACT_DETAILS, contact);
            ContactDetailsActivity.this.startActivity(editContact);
            return true;
        });
        menu.findItem(R.id.image_button_delete_contact).setOnMenuItemClickListener(item -> {
            new AlertDialog.Builder(ContactDetailsActivity.this)
                    .setMessage(R.string.do_you_want_to_delete)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        ContactsDataStore.removeContact(contact);
                        Toast.makeText(ContactDetailsActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(R.string.no, null).show();
            return true;
        });
        return super.onCreateOptionsMenu(menu);
    }
}