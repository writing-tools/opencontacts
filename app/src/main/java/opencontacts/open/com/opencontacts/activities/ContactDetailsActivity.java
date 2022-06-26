package opencontacts.open.com.opencontacts.activities;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.addFavorite;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.isFavorite;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.removeFavorite;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.copyToClipboard;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getFormattedDate;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getIntentToAddFullDayEventOnCalendar;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getMenuItemClickHandlerFor;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.openMap;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.wrapInConfirmation;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.formatAddressToAMultiLineString;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getAddressTypeTranslatedText;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getMobileNumberTypeTranslatedText;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isSocialIntegrationEnabled;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getMobileNumber;

import android.content.Intent;
import android.os.Bundle;
import androidx.core.util.Pair;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
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
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.Note;
import ezvcard.property.Url;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.ExpandedList;
import opencontacts.open.com.opencontacts.components.TintedDrawablesStore;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.DomainUtils;
import opencontacts.open.com.opencontacts.utils.VCardUtils;


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
        ContactsDataStore.togglePrimaryNumber(getSelectedMobileNumber((View) v.getParent()), contactId);
        contact = ContactsDataStore.getContactWithId(contactId);
        fillPhoneNumbers();
    };

    private View.OnClickListener messageContact = v -> AndroidUtils.message(getSelectedMobileNumber((View) v.getParent()), ContactDetailsActivity.this);

    private View.OnClickListener openSocialApp = v -> AndroidUtils.openSocialApp(getSelectedMobileNumber((View) v.getParent()), ContactDetailsActivity.this);
    private View.OnLongClickListener socialLongPress = v -> AndroidUtils.onSocialLongPress(getSelectedMobileNumber((View) v.getParent()), ContactDetailsActivity.this);

    private View.OnLongClickListener copyPhoneNumberToClipboard = v -> {
        copyToClipboard(getSelectedMobileNumber(v), ContactDetailsActivity.this);
        Toast.makeText(ContactDetailsActivity.this, R.string.copied_phonenumber_to_clipboard, Toast.LENGTH_SHORT).show();
        return true;
    };
    private boolean shouldShowSocialAppIcon;

    private String getSelectedMobileNumber(View v) {
        return v.getTag().toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        contactId = intent.getLongExtra(MainActivity.INTENT_EXTRA_LONG_CONTACT_ID, -1);
        if (contactId == -1) showInvalidContactErrorAndExit();
        contact = ContactsDataStore.getContactWithId(contactId);
        shouldShowSocialAppIcon = isSocialIntegrationEnabled(this);
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_contact_details;
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        contact = ContactsDataStore.getContactWithId(contactId);
        VCardData vcardData = ContactsDataStore.getVCardData(contactId);
        if (contact == null) {
            showInvalidContactErrorAndExit();
            return;
        }
        if (vcardData != null) {
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
        if (vcard == null) return;
        fillPhoneNumbers();
        fillEmailAddress();
        fillAddress();
        fillNotes();
        fillDateOfBirth();
        fillWebsite();
        fillGroups();
    }

    private void fillGroups() {
        List<String> categories = VCardUtils.getCategories(vcard);
        View groupsCard = findViewById(R.id.groups_card);
        if (categories.isEmpty()) {
            groupsCard.setVisibility(GONE);
            return;
        }
        groupsCard.setVisibility(VISIBLE);
        AppCompatTextView groupsTextView = groupsCard.findViewById(R.id.text_view);
        groupsTextView.setText(Contact.getGroupsNamesCSVString(categories).replaceAll(",", ", "));
        groupsTextView.setOnClickListener(v -> startActivity(new Intent(this, GroupsActivity.class)));
    }

    private void fillWebsite() {
        Url url = U.firstOrNull(vcard.getUrls());
        View websiteCard = findViewById(R.id.website_card);
        if (url == null) {
            websiteCard.setVisibility(GONE);
            return;
        }
        websiteCard.setVisibility(VISIBLE);
        AppCompatTextView websiteTextView = websiteCard.findViewById(R.id.text_view);
        websiteTextView.setText(url.getValue());
        websiteTextView.setOnClickListener(v -> AndroidUtils.goToUrl(url.getValue(), this));
    }

    private void fillDateOfBirth() {
        Birthday birthday = vcard.getBirthday();
        View birthDayCard = findViewById(R.id.date_of_birth_card);
        if (birthday == null || birthday.getDate() == null) {
            birthDayCard.setVisibility(GONE);
            return;
        }
        birthDayCard.setVisibility(VISIBLE);
        AppCompatTextView birthDayTextView = birthDayCard.findViewById(R.id.text_view);
        birthDayTextView.setText(getFormattedDate(birthday.getDate()));
        birthDayTextView.setOnClickListener(v -> {
            Intent intent = getIntentToAddFullDayEventOnCalendar(birthday.getDate(), getString(R.string.calendar_event_title_birthday, contact.name));
            startActivity(intent);
        });
    }


    private void fillNotes() {
        Note note = U.firstOrNull(vcard.getNotes());
        View notesCard = findViewById(R.id.notes_card);
        if (note == null) {
            notesCard.setVisibility(GONE);
            return;
        }
        AppCompatTextView notesTextView = notesCard.findViewById(R.id.text_view);
        notesTextView.setText(note.getValue());
        notesTextView.setOnLongClickListener(v -> {
            copyToClipboard(notesTextView.getText().toString(), true, this);
            return true;
        });
        notesCard.setVisibility(VISIBLE);
    }

    private void fillAddress() {
        if (U.isEmpty(vcard.getAddresses())) {
            findViewById(R.id.address_card).setVisibility(GONE);
            return;
        }
        findViewById(R.id.address_card).setVisibility(VISIBLE);
        addressLinearLayout.removeAllViews();
        List<Address> addresses = vcard.getAddresses();
        ExpandedList addressesExpandedListView = new ExpandedList.Builder(this)
            .withOnItemClickListener(index -> openMap(addresses.get(index).getStreetAddress(), this))
            .withItems(U.map(addresses, address -> new Pair<>(formatAddressToAMultiLineString(address, this), getAddressTypeTranslatedText(address, this))))
            .withOnItemLongClickListener(index -> copyToClipboard(formatAddressToAMultiLineString(addresses.get(index), this), true, this))
            .build();
        addressLinearLayout.addView(addressesExpandedListView);
    }

    private void fillEmailAddress() {
        if (U.isEmpty(vcard.getEmails())) {
            findViewById(R.id.email_card).setVisibility(GONE);
            return;
        }
        findViewById(R.id.email_card).setVisibility(VISIBLE);
        emailAddressLinearLayout.removeAllViews();
        List<Email> emails = vcard.getEmails();
        ExpandedList emailsExpandedListView = new ExpandedList.Builder(this)
            .withOnItemClickListener(index -> AndroidUtils.email(emails.get(index).getValue(), this))
            .withItems(U.map(emails, email -> new Pair<>(email.getValue(), DomainUtils.getEmailTypeTranslatedText(email.getTypes(), this))))
            .withOnItemLongClickListener(index -> copyToClipboard(emails.get(index).getValue(), true, this))
            .build();
        emailAddressLinearLayout.addView(emailsExpandedListView);
    }

    private void fillPhoneNumbers() {
        if (U.isEmpty(vcard.getTelephoneNumbers())) {
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
            View socialAppIcon = inflatedView.findViewById(R.id.button_social);
            if (shouldShowSocialAppIcon) {
                socialAppIcon.setOnClickListener(openSocialApp);
                socialAppIcon.setOnLongClickListener(socialLongPress);
                socialAppIcon.setVisibility(VISIBLE);
            }
            ((AppCompatTextView) inflatedView.findViewById(R.id.phone_number_type)).setText(getMobileNumberTypeTranslatedText(telephone.getTypes(), this));
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
        menu.findItem(R.id.image_button_export_to_contacts_app).setOnMenuItemClickListener(getMenuItemClickHandlerFor(this::exportToContactsApp));
        menu.findItem(R.id.image_button_edit_contact).setOnMenuItemClickListener(item -> {
            Intent editContact = new Intent(ContactDetailsActivity.this, EditContactActivity.class);
            editContact.putExtra(EditContactActivity.INTENT_EXTRA_CONTACT_CONTACT_DETAILS, contact);
            ContactDetailsActivity.this.startActivity(editContact);
            return true;
        });
        menu.findItem(R.id.image_button_delete_contact).setOnMenuItemClickListener(getMenuItemClickHandlerFor(this::deleteContactAfterConfirmation));
        boolean isFavorite = isFavorite(contact);
        menu.add(isFavorite ? R.string.remove_favorite : R.string.add_to_favorites)
            .setIcon(TintedDrawablesStore.getTintedDrawable(isFavorite ? R.drawable.ic_favorite_solid_24dp : R.drawable.ic_favorite_hollow_black_24dp, this))
            .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
            .setOnMenuItemClickListener(item -> {
                if (isFavorite) removeFavorite(contact);
                else addFavorite(contact);
                invalidateOptionsMenu();
                return true;
            });
        return super.onCreateOptionsMenu(menu);
    }

    private void deleteContactAfterConfirmation() {
        wrapInConfirmation(() -> {
            ContactsDataStore.removeContact(contact);
            Toast.makeText(ContactDetailsActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
            finish();
        }, this);
    }
}
