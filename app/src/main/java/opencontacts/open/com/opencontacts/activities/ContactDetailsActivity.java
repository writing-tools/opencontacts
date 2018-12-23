package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.os.Bundle;
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

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

import static opencontacts.open.com.opencontacts.utils.DomainUtils.getMobileNumberTypeTranslatedText;


public class ContactDetailsActivity extends AppBaseActivity {
    private long contactId;
    private Contact contact;
    private VCardData vcardData;

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
    private LinearLayout phoneNumbersLinearLayout;
    private LayoutInflater layoutInflater;

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
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_contact_details;
    }

    @Override
    protected void onResume() {
        super.onResume();
        contact = ContactsDataStore.getContactWithId(contactId);
        vcardData = ContactsDataStore.getVCardData(contactId);
        if(contact == null)
            showInvalidContactErrorAndExit();
        setUpUI();
    }

    private void showInvalidContactErrorAndExit() {
        Toast.makeText(this, R.string.error_while_loading_contact, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void setUpUI() {
        toolbar.setTitle(contact.firstName);
        toolbar.setSubtitle(contact.name);
        phoneNumbersLinearLayout = findViewById(R.id.phone_numbers_list);
        layoutInflater = getLayoutInflater();
        fillPhoneNumbers();
    }

    private void fillPhoneNumbers() {
        phoneNumbersLinearLayout.removeAllViews();
        U.forEach(contact.phoneNumbers, phoneNumber -> {
            View inflatedView = layoutInflater.inflate(R.layout.contact_details_row, phoneNumbersLinearLayout, false);
            ((TextView) inflatedView.findViewById(R.id.textview_phone_number)).setText(phoneNumber.phoneNumber);
            AppCompatImageButton primaryNumberToggleButton = inflatedView.findViewById(R.id.button_primary_number);
            primaryNumberToggleButton.setImageResource(phoneNumber.equals(contact.primaryPhoneNumber) ? R.drawable.ic_star_filled_24dp : R.drawable.ic_star_empty_24dp);
            primaryNumberToggleButton.setOnClickListener(togglePrimaryNumber);
            inflatedView.findViewById(R.id.button_message).setOnClickListener(messageContact);
            inflatedView.findViewById(R.id.button_whatsapp).setOnClickListener(whatsappContact);
            ((AppCompatTextView)inflatedView.findViewById(R.id.phone_number_type)).setText(getMobileNumberTypeTranslatedText(phoneNumber.type,this));
            inflatedView.setOnClickListener(callContact);
            inflatedView.setOnLongClickListener(copyPhoneNumberToClipboard);
            inflatedView.setTag(phoneNumber.phoneNumber);
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