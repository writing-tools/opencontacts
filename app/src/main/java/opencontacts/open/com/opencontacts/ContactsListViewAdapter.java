package opencontacts.open.com.opencontacts;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;

import opencontacts.open.com.opencontacts.domain.Contact;

public class ContactsListViewAdapter extends ArrayAdapter<Contact>{
    private ContactsListActionsListener contactsListActionsListener;
    private LayoutInflater layoutInflater;
    public ContactsListFilter contactsListFilter;

    private final View.OnClickListener callContact = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) v.getTag();
            contactsListActionsListener.onCallClicked(contact);
        }
    };
    private final View.OnClickListener messageContact = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onMessageClicked(contact);
        }
    };
    private final View.OnClickListener showContactDetails = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onShowDetails(contact);
        }
    };
    private final View.OnClickListener whatsappContact = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onWhatsappClicked(contact);
        }
    };

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Contact contact = getItem(position);
        if(convertView == null)
            convertView = layoutInflater.inflate(R.layout.contact, parent, false);
        ((TextView) convertView.findViewById(R.id.textview_full_name)).setText(contact.name);
        ((TextView) convertView.findViewById(R.id.textview_phone_number)).setText(contact.primaryPhoneNumber);
        convertView.findViewById(R.id.button_info).setOnClickListener(showContactDetails);
        convertView.findViewById(R.id.button_message).setOnClickListener(messageContact);
        convertView.findViewById(R.id.button_whatsapp).setOnClickListener(whatsappContact);
        convertView.setTag(contact);
        convertView.setOnClickListener(callContact);
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return contactsListFilter;
    }

    ContactsListViewAdapter(@NonNull Context context, int resource, ContactsListFilter.AllContactsHolder allContactsHolder) {
        super(context, resource, new ArrayList<>(allContactsHolder.getContacts()));
        layoutInflater = LayoutInflater.from(context);
        contactsListFilter = new ContactsListFilter(this, allContactsHolder);
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
