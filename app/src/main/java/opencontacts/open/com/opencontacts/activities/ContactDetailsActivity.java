package opencontacts.open.com.opencontacts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;


public class ContactDetailsActivity extends AppBaseActivity {
    private long contactId;
    private Contact contact;
    private ArrayAdapter<String> phoneNumbersListArrayAdapter;

    private View.OnClickListener callContact = v -> AndroidUtils.call(getSelectedMobileNumber(v), ContactDetailsActivity.this);

    private View.OnClickListener togglePrimaryNumber = v -> {
        ContactsDataStore.togglePrimaryNumber(getSelectedMobileNumber((View)v.getParent()), contactId);
        contact = ContactsDataStore.getContactWithId(contactId);
        phoneNumbersListArrayAdapter.clear();
        phoneNumbersListArrayAdapter.addAll(contact.phoneNumbers);
        phoneNumbersListArrayAdapter.notifyDataSetChanged();
    };

    private View.OnClickListener messageContact = v -> AndroidUtils.message(getSelectedMobileNumber((View)v.getParent()), ContactDetailsActivity.this);

    private View.OnClickListener whatsappContact = v -> AndroidUtils.whatsapp(getSelectedMobileNumber((View)v.getParent()), ContactDetailsActivity.this);

    private View.OnLongClickListener copyPhoneNumberToClipboard = new View.OnLongClickListener(){
        @Override
        public boolean onLongClick(View v) {
            AndroidUtils.copyToClipboard(getSelectedMobileNumber(v), ContactDetailsActivity.this);
            Toast.makeText(ContactDetailsActivity.this, R.string.copied_phonenumber_to_clipboard, Toast.LENGTH_SHORT).show();
            return true;
        }
    };
    private String getSelectedMobileNumber(View v){
        return v.getTag().toString();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.about_star).setOnClickListener(v -> new AlertDialog.Builder(ContactDetailsActivity.this)
                .setTitle(R.string.filled_star)
                .setMessage(R.string.about_primary_number)
                .show());
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
        ListView phoneNumbersListView = findViewById(R.id.listview_phone_numbers);
        final List<String> mobileNumbers = contact.phoneNumbers;
        phoneNumbersListArrayAdapter = new ArrayAdapter<String>(this, R.layout.contact_details_row, mobileNumbers) {
            private LayoutInflater layoutInflater = LayoutInflater.from(getContext());

            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = layoutInflater.inflate(R.layout.contact_details_row, parent, false);
                String mobileNumber = mobileNumbers.get(position);
                ((TextView) convertView.findViewById(R.id.textview_phone_number)).setText(mobileNumber);
                AppCompatImageButton primaryNumberToggleButton = convertView.findViewById(R.id.button_primary_number);
                primaryNumberToggleButton.setImageResource(mobileNumber.equals(contact.primaryPhoneNumber) ? R.drawable.ic_star_filled_24dp : R.drawable.ic_star_empty_24dp);
                primaryNumberToggleButton.setOnClickListener(togglePrimaryNumber);
                convertView.findViewById(R.id.button_message).setOnClickListener(messageContact);
                convertView.findViewById(R.id.button_whatsapp).setOnClickListener(whatsappContact);
                convertView.setOnClickListener(callContact);
                convertView.setOnLongClickListener(copyPhoneNumberToClipboard);
                convertView.setTag(mobileNumber);
                return convertView;
            }
        };
        phoneNumbersListView.setAdapter(phoneNumbersListArrayAdapter);
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