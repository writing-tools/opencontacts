package opencontacts.open.com.opencontacts;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;

import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils;

import static android.view.View.*;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isT9SearchEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isWhatsappIntegrationEnabled;

public class ContactsListViewAdapter extends ArrayAdapter<Contact>{
    private ContactsListActionsListener contactsListActionsListener;
    private LayoutInflater layoutInflater;
    public ContactsListFilter contactsListFilter;
    private boolean whatsappIntegrationEnabled;
    //android has weakref to this listener and gets garbage collected hence we should have it here.
    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    ContactsListViewAdapter(@NonNull Context context, int resource, ContactsListFilter.AllContactsHolder allContactsHolder) {
        super(context, resource, new ArrayList<>(allContactsHolder.getContacts()));
        layoutInflater = LayoutInflater.from(context);
        createContactsListFilter(allContactsHolder);
        whatsappIntegrationEnabled = isWhatsappIntegrationEnabled(context);
        //android has weakref to this listener and gets garbage collected hence we should have it here.
        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (!WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY.equals(key)) return;
            whatsappIntegrationEnabled = isWhatsappIntegrationEnabled(context);
            notifyDataSetChanged();
        };
        SharedPreferencesUtils.setSharedPreferencesChangeListener(sharedPreferenceChangeListener, context);
    }

    private void createContactsListFilter(ContactsListFilter.AllContactsHolder allContactsHolder) {
        contactsListFilter = isT9SearchEnabled(getContext()) ? new ContactsListT9Filter(this, allContactsHolder)
                : new ContactsListTextFilter(this, allContactsHolder);
    }

    private final OnClickListener callContact = v -> {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) v.getTag();
            contactsListActionsListener.onCallClicked(contact);
    };
    private final OnClickListener messageContact = v -> {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onMessageClicked(contact);
    };
    private final OnClickListener showContactDetails = v -> {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onShowDetails(contact);
    };
    private final OnClickListener whatsappContact = v -> {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onWhatsappClicked(contact);
    };

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Contact contact = getItem(position);
        if(convertView == null)
            convertView = layoutInflater.inflate(R.layout.contact, parent, false);
        ((TextView) convertView.findViewById(R.id.textview_full_name)).setText(contact.name);
        ((TextView) convertView.findViewById(R.id.textview_phone_number)).setText(contact.primaryPhoneNumber.phoneNumber);
        convertView.findViewById(R.id.button_info).setOnClickListener(showContactDetails);
        convertView.findViewById(R.id.button_message).setOnClickListener(messageContact);
        View whatsappIcon = convertView.findViewById(R.id.button_whatsapp);
        if(whatsappIntegrationEnabled){
            whatsappIcon.setOnClickListener(whatsappContact);
            whatsappIcon.setVisibility(VISIBLE);
        }
        else whatsappIcon.setVisibility(GONE);
        convertView.setTag(contact);
        convertView.setOnClickListener(callContact);
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return contactsListFilter;
    }

    public void setContactsListActionsListener(ContactsListActionsListener contactsListActionsListener){
        this.contactsListActionsListener = contactsListActionsListener;
    }

    interface ContactsListActionsListener {
        void onCallClicked(Contact contact);
        void onMessageClicked(Contact contact);
        void onShowDetails(Contact contact);
        void onWhatsappClicked(Contact contact);
    }
}
