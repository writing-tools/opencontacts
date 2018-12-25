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
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

import static android.view.View.*;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY;

public class ContactsListViewAdapter extends ArrayAdapter<Contact>{
    private ContactsListActionsListener contactsListActionsListener;
    private LayoutInflater layoutInflater;
    public ContactsListFilter contactsListFilter;
    private boolean isWhatsappIntegrationEnabled;
    //android has weakref to this listener and gets garbage collected hence we should have it here.
    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    ContactsListViewAdapter(@NonNull Context context, int resource, ContactsListFilter.AllContactsHolder allContactsHolder) {
        super(context, resource, new ArrayList<>(allContactsHolder.getContacts()));
        layoutInflater = LayoutInflater.from(context);
        contactsListFilter = new ContactsListFilter(this, allContactsHolder);
        isWhatsappIntegrationEnabled = AndroidUtils.isWhatsappIntegrationEnabled(context);
        //android has weakref to this listener and gets garbage collected hence we should have it here.
        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (!WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY.equals(key)) return;
            isWhatsappIntegrationEnabled = AndroidUtils.isWhatsappIntegrationEnabled(context);
            notifyDataSetChanged();
        };
        AndroidUtils.setSharedPreferencesChangeListener(sharedPreferenceChangeListener, context);
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
        if(isWhatsappIntegrationEnabled){
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
